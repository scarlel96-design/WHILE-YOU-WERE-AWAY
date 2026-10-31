"""Synthetic validator tests. These results are not real-client evidence."""
import contextlib
import copy
import io
import json
from pathlib import Path
import tempfile
import unittest

import v2_01_client_evidence as validator


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "session.json"
        self.output = self.root / "summary.json"
        (self.root / "run.log").write_text("SYNTHETIC SELF-TEST ONLY", encoding="utf-8")
        self.document = {"schema": 1, "evidence_mode": "SYNTHETIC", "scenarios": [self.row(n) for n in validator.SCENARIOS]}
        self.save()

    def row(self, name):
        return {"scenario": name, "result": "PASS", "build_sha": "a" * 40, "jar_sha256": "b" * 64,
                "isolated_profile": "synthetic-profile", "start_condition": "synthetic start",
                "observed_behavior": "synthetic observation", "expected_behavior": "synthetic expectation",
                "evidence_reference": ["run.log"], "tester": "validator self-test", "tester_kind": "AUTOMATED", "notes": ""}

    def save(self):
        self.source.write_text(json.dumps(self.document), encoding="utf-8")

    def summary(self):
        self.save()
        return validator.summarize(self.document, self.source, self.output)

    def reject(self, fragment):
        with self.assertRaisesRegex(validator.InvalidEvidence, fragment):
            self.summary()

    def test_missing_scenarios_are_not_tested(self):
        self.document["scenarios"] = []
        result = self.summary()
        self.assertEqual(result["counts"], {"NOT_TESTED": 15})
        self.assertEqual(result["V2_01_CLIENT_GATE"], "PARTIAL")
        self.assertEqual(result["REAL_CLIENT"], "NOT_TESTED")

    def test_synthetic_pass_never_becomes_client_pass(self):
        result = self.summary()
        self.assertEqual(result["reported_evidence_gate"], "PASS")
        self.assertEqual(result["V2_01_CLIENT_GATE"], "PARTIAL")
        self.assertEqual(result["HUMAN_EXPERIENCE"], "NOT_TESTED")

    def test_every_must_failure_blocks_gate(self):
        for name in validator.MUST:
            doc = copy.deepcopy(self.document)
            next(row for row in doc["scenarios"] if row["scenario"] == name)["result"] = "FAIL"
            result = validator.summarize(doc, self.source, self.output)
            self.assertEqual(result["reported_evidence_gate"], "FAIL", name)

    def test_blocked_and_not_tested_must_remain_partial(self):
        for status in ("BLOCKED", "NOT_TESTED"):
            self.document["scenarios"][0]["result"] = status
            self.assertEqual(self.summary()["reported_evidence_gate"], "PARTIAL")

    def test_optional_failure_does_not_override_required_gate(self):
        next(r for r in self.document["scenarios"] if r["scenario"] == "yeoul_daily_route")["result"] = "FAIL"
        self.assertEqual(self.summary()["reported_evidence_gate"], "PASS")

    def test_duplicate_and_unknown_scenarios_rejected(self):
        self.document["scenarios"].append(copy.deepcopy(self.document["scenarios"][0]))
        self.reject("DUPLICATE_SCENARIO")
        self.document["scenarios"][-1]["scenario"] = "imagined_success"
        self.reject("UNKNOWN_SCENARIO")

    def test_invalid_result_rejected(self):
        self.document["scenarios"][0]["result"] = "probably PASS"
        self.reject("INVALID_RESULT")

    def test_metadata_required_for_executed_scenario(self):
        for field in (*validator.TEXT_FIELDS, "build_sha", "jar_sha256", "tester_kind", "isolated_profile"):
            with self.subTest(field=field):
                row = self.document["scenarios"][0]
                old = row.pop(field)
                self.reject("MISSING|INVALID")
                row[field] = old

    def test_mixed_build_or_jar_rejected(self):
        for field, length in (("build_sha", 40), ("jar_sha256", 64)):
            old = self.document["scenarios"][0][field]
            self.document["scenarios"][0][field] = "c" * length
            self.reject("MIXED_BUILD_EVIDENCE")
            self.document["scenarios"][0][field] = old

    def test_automatic_human_result_rejected(self):
        self.document["evidence_mode"] = "REAL_CLIENT"
        for field in validator.HUMAN_FIELDS:
            self.document["scenarios"][0][field] = "PASS"
            self.reject("HUMAN_RESULT_WITHOUT_HUMAN_RUN")
            del self.document["scenarios"][0][field]

    def test_synthetic_human_claim_rejected(self):
        self.document["scenarios"][0].update(tester_kind="HUMAN", human_readability="PASS")
        self.reject("HUMAN_RESULT_WITHOUT_HUMAN_RUN")

    def test_human_rollup_requires_every_must(self):
        self.document["evidence_mode"] = "REAL_CLIENT"
        for row in self.document["scenarios"]:
            row.update(tester_kind="HUMAN", human_readability="PASS")
        self.assertEqual(self.summary()["HUMAN_EXPERIENCE"], "PASS")
        self.document["scenarios"][0]["human_readability"] = "NOT_TESTED"
        self.assertEqual(self.summary()["HUMAN_EXPERIENCE"], "NOT_TESTED")
        self.document["scenarios"][0]["human_readability"] = "FAIL"
        self.assertEqual(self.summary()["HUMAN_EXPERIENCE"], "FAIL")

    def test_reference_existence_and_hash(self):
        result = self.summary()
        ref = result["scenarios"][0]["verified_references"][0]
        self.assertEqual(ref["size"], len("SYNTHETIC SELF-TEST ONLY"))
        self.assertEqual(len(ref["sha256"]), 64)
        self.document["scenarios"][0]["evidence_reference"] = ["missing.log"]
        self.reject("INVALID_REFERENCE_TARGET")

    def test_reference_escape_and_input_output_rejected(self):
        for reference in (str(self.source), str(self.output), "../outside.log"):
            self.document["scenarios"][0]["evidence_reference"] = [reference]
            self.reject("INVALID_REFERENCE_TARGET")

    def test_pass_requires_evidence(self):
        self.document["scenarios"][0]["evidence_reference"] = []
        self.reject("MISSING_EVIDENCE")

    def test_duplicate_json_key_rejected(self):
        self.source.write_text('{"schema":1,"schema":1}', encoding="utf-8")
        with contextlib.redirect_stdout(io.StringIO()) as output:
            code = validator.main([str(self.source), str(self.output)])
        self.assertEqual(code, 2)
        self.assertIn("DUPLICATE_JSON_KEY", output.getvalue())

    def test_invalid_input_preserves_existing_output(self):
        self.output.write_text("KEEP", encoding="utf-8")
        self.document["schema"] = 99;self.save()
        with contextlib.redirect_stdout(io.StringIO()):
            code = validator.main([str(self.source), str(self.output)])
        self.assertEqual(code, 2)
        self.assertEqual(self.output.read_text(), "KEEP")

    def test_cli_partial_exit_and_input_preservation(self):
        self.document["scenarios"] = [];self.save();before=self.source.read_bytes()
        with contextlib.redirect_stdout(io.StringIO()):
            code = validator.main([str(self.source), str(self.output)])
        self.assertEqual(code, 1)
        self.assertEqual(self.source.read_bytes(), before)
        self.assertEqual(json.loads(self.output.read_text())["counts"]["NOT_TESTED"], 15)

    def test_cli_same_file_rejected(self):
        before = self.source.read_bytes()
        with contextlib.redirect_stdout(io.StringIO()):
            code = validator.main([str(self.source), str(self.source)])
        self.assertEqual(code, 2)
        self.assertEqual(self.source.read_bytes(), before)


if __name__ == "__main__":
    unittest.main()
