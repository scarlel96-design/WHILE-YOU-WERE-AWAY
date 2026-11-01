"""Synthetic log ingestion tests. These tests do not execute Minecraft."""
import contextlib
import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest

import v2_01_client_evidence as validator
import v2_01_client_log_draft as draft


class LogDraftTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.log = self.root / "client.log"
        self.output = self.root / "draft"
        self.raw = (b"[12:00:00] [Render thread/INFO] [minecraft/SoundEngine]: Sound engine started\r\n"
                    b"BUILD SUCCESSFUL\r\nAll scenarios PASS; human audio PASS\r\n")
        self.log.write_bytes(self.raw)

    def test_success_markers_and_claim_text_never_grant_acceptance(self):
        review = draft.prepare(self.log, self.output)
        self.assertFalse(review["inferred_acceptance"])
        self.assertEqual(review["markers"]["gradle_success"]["count"], 1)
        session = json.loads((self.output / "client-session.json").read_text())
        summary = validator.summarize(session, self.output / "client-session.json", self.output / "summary.json")
        self.assertEqual(summary["counts"], {"NOT_TESTED": 15})
        self.assertEqual(summary["REAL_CLIENT"], "NOT_TESTED")
        self.assertEqual(summary["HUMAN_EXPERIENCE"], "NOT_TESTED")

    def test_source_copy_is_byte_exact_and_source_is_unchanged(self):
        review = draft.prepare(self.log, self.output)
        self.assertEqual((self.output / "references/source.log").read_bytes(), self.raw)
        self.assertEqual(self.log.read_bytes(), self.raw)
        self.assertEqual(review["source_sha256"], hashlib.sha256(self.raw).hexdigest())

    def test_target_jar_is_measured_but_never_attached_to_unexecuted_rows(self):
        jar = self.root / "candidate.jar"
        jar.write_bytes(b"SYNTHETIC JAR FIXTURE")
        review = draft.prepare(self.log, self.output, "planned-profile", "a" * 40, jar)
        self.assertEqual(review["target_build"]["jar_sha256"], hashlib.sha256(jar.read_bytes()).hexdigest())
        session = json.loads((self.output / "client-session.json").read_text())
        self.assertTrue(all(row["build_sha"] is None and row["jar_sha256"] is None for row in session["scenarios"]))

    def test_existing_output_is_preserved(self):
        self.output.mkdir()
        sentinel = self.output / "keep.txt"
        sentinel.write_text("KEEP")
        with self.assertRaisesRegex(ValueError, "OUTPUT_ALREADY_EXISTS"):
            draft.prepare(self.log, self.output)
        self.assertEqual(sentinel.read_text(), "KEEP")
        self.assertEqual(len(list(self.output.iterdir())), 1)

    def test_missing_log_does_not_publish(self):
        with self.assertRaises(OSError):
            draft.prepare(self.root / "missing.log", self.output)
        self.assertFalse(self.output.exists())

    def test_target_pair_and_hash_are_validated_before_publication(self):
        for build, jar in (("a" * 40, None), (None, self.log), ("bad", self.log)):
            with self.subTest(build=build, jar=jar), self.assertRaises(ValueError):
                draft.prepare(self.log, self.output, build_sha=build, jar=jar)
            self.assertFalse(self.output.exists())

    def test_server_log_does_not_prove_client_execution(self):
        self.log.write_text("Preparing start region for dimension minecraft:overworld\nPlayer joined the game\n")
        review = draft.prepare(self.log, self.output)
        self.assertEqual(review["source_kind"], "UNCLASSIFIED_LOG")
        self.assertEqual(review["markers"]["player_join"]["count"], 1)
        self.assertFalse(review["inferred_acceptance"])

    def test_kernel_failure_is_only_a_log_marker(self):
        self.log.write_text("failed to write kernel assets: os error 3\n")
        review = draft.prepare(self.log, self.output)
        self.assertEqual(review["markers"]["error_or_kernel_failure"]["count"], 1)
        session = json.loads((self.output / "client-session.json").read_text())
        self.assertTrue(all(row["result"] == "NOT_TESTED" for row in session["scenarios"]))

    def test_non_utf8_log_keeps_original_hash(self):
        self.log.write_bytes(b"\xff\xfe\x00\r\n")
        review = draft.prepare(self.log, self.output)
        self.assertEqual(review["source_sha256"], hashlib.sha256(self.log.read_bytes()).hexdigest())

    def test_cli_reports_draft_success_and_repeat_failure(self):
        with contextlib.redirect_stdout(io.StringIO()) as output:
            first = draft.main([str(self.log), str(self.output)])
            second = draft.main([str(self.log), str(self.output)])
        self.assertEqual((first, second), (0, 2))
        self.assertIn("REAL_CLIENT=NOT_TESTED", output.getvalue())
        self.assertIn("OUTPUT_ALREADY_EXISTS", output.getvalue())


if __name__ == "__main__":
    unittest.main()
