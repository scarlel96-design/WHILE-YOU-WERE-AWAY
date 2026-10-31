"""Validate reported client evidence. Never infer gameplay from automated tests."""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re
import tempfile

SCENARIOS = (
    "new_world_boot", "opening_transition", "yeoul_first_contact",
    "return_network_normal_path", "returner_unobserved_completion",
    "returner_positive_observation", "observation_interrupted", "occluded_returner",
    "warning_ignored_crisis", "recovery", "station_revisit_observed",
    "station_revisit_unobserved", "yeoul_post_event_dialogue", "yeoul_daily_route",
    "survival_continuity",
)
MUST = frozenset(SCENARIOS) - {
    "returner_unobserved_completion", "occluded_returner", "yeoul_daily_route",
}
RESULTS = {"PASS", "FAIL", "BLOCKED", "NOT_TESTED"}
HUMAN_FIELDS = ("human_readability", "fear_pacing", "audio_readability")
TEXT_FIELDS = ("start_condition", "observed_behavior", "expected_behavior", "tester", "notes")


class InvalidEvidence(ValueError):
    pass


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise InvalidEvidence(f"DUPLICATE_JSON_KEY: {key}")
        result[key] = value
    return result


def _hash(value, length, label, required):
    if not required and value in (None, ""):
        return
    if not isinstance(value, str) or not re.fullmatch(rf"[0-9a-fA-F]{{{length}}}", value):
        raise InvalidEvidence(f"INVALID_HASH: {label}")


def summarize(document, source: Path, destination: Path):
    if not isinstance(document, dict) or type(document.get("schema")) is not int or document["schema"] != 1:
        raise InvalidEvidence("UNSUPPORTED_EVIDENCE_SCHEMA")
    mode = document.get("evidence_mode")
    if mode not in ("REAL_CLIENT", "SYNTHETIC"):
        raise InvalidEvidence("INVALID_EVIDENCE_MODE")
    rows = document.get("scenarios")
    if not isinstance(rows, list):
        raise InvalidEvidence("SCENARIOS_MUST_BE_LIST")
    source, destination = source.resolve(), destination.resolve()
    if source == destination:
        raise InvalidEvidence("OUTPUT_IS_INPUT")
    root = source.parent
    found, references = {}, set()
    for row in rows:
        if not isinstance(row, dict):
            raise InvalidEvidence("SCENARIO_MUST_BE_OBJECT")
        name, result = row.get("scenario"), row.get("result")
        if not isinstance(name, str) or name not in SCENARIOS:
            raise InvalidEvidence(f"UNKNOWN_SCENARIO: {name}")
        if name in found:
            raise InvalidEvidence(f"DUPLICATE_SCENARIO: {name}")
        if not isinstance(result, str) or result not in RESULTS:
            raise InvalidEvidence(f"INVALID_RESULT: {name}")
        required = result != "NOT_TESTED"
        for field in TEXT_FIELDS:
            value = row.get(field)
            if not isinstance(value, str) or (required and field != "notes" and not value.strip()):
                raise InvalidEvidence(f"MISSING_METADATA: {name}.{field}")
        identity = row.get("world_identity") or row.get("isolated_profile")
        if required and (not isinstance(identity, str) or not identity.strip()):
            raise InvalidEvidence(f"MISSING_WORLD_IDENTITY: {name}")
        _hash(row.get("build_sha"), 40, name + ".build_sha", required)
        _hash(row.get("jar_sha256"), 64, name + ".jar_sha256", required)
        kind = row.get("tester_kind")
        if kind not in ("HUMAN", "AUTOMATED", "NOT_RUN") or (required and kind == "NOT_RUN"):
            raise InvalidEvidence(f"INVALID_TESTER_KIND: {name}")
        human = {}
        for field in HUMAN_FIELDS:
            value = row.get(field, "NOT_TESTED")
            if value not in ("PASS", "FAIL", "NOT_TESTED"):
                raise InvalidEvidence(f"INVALID_HUMAN_RESULT: {name}.{field}")
            if value != "NOT_TESTED" and (kind != "HUMAN" or mode != "REAL_CLIENT" or result == "NOT_TESTED"):
                raise InvalidEvidence(f"HUMAN_RESULT_WITHOUT_HUMAN_RUN: {name}.{field}")
            human[field] = value
        refs = row.get("evidence_reference")
        if not isinstance(refs, list) or (result == "PASS" and not refs):
            raise InvalidEvidence(f"MISSING_EVIDENCE: {name}")
        verified = []
        for ref in refs:
            if not isinstance(ref, str) or not ref.strip():
                raise InvalidEvidence(f"INVALID_REFERENCE: {name}")
            path = (root / ref).resolve()
            if not path.is_relative_to(root) or path in (source, destination) or not path.is_file():
                raise InvalidEvidence(f"INVALID_REFERENCE_TARGET: {name}")
            references.add(path)
            verified.append({"path": path.relative_to(root).as_posix(), "size": path.stat().st_size,
                             "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
        found[name] = dict(row, supplied=True, **human, verified_references=verified)
    normalized = [found.get(name, {"scenario": name, "result": "NOT_TESTED", "supplied": False,
                                 **dict.fromkeys(HUMAN_FIELDS, "NOT_TESTED")}) for name in SCENARIOS]
    required_rows = [row for row in normalized if row["scenario"] in MUST]
    for field in ("build_sha", "jar_sha256"):
        builds = {row[field].lower() for row in found.values() if row["result"] != "NOT_TESTED"}
        if len(builds) > 1:
            raise InvalidEvidence(f"MIXED_BUILD_EVIDENCE: {field}")
    gate = "FAIL" if any(row["result"] == "FAIL" for row in required_rows) else (
        "PASS" if all(row["result"] == "PASS" for row in required_rows) else "PARTIAL")
    human = {}
    for field in HUMAN_FIELDS:
        values = [row[field] for row in required_rows]
        human[field] = "FAIL" if "FAIL" in values else ("PASS" if all(v == "PASS" for v in values) else "NOT_TESTED")
    return {
        "schema": 1, "evidence_mode": mode, "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
        "reported_evidence_gate": gate,
        "V2_01_CLIENT_GATE": gate if mode == "REAL_CLIENT" else "PARTIAL",
        "REAL_CLIENT": gate if mode == "REAL_CLIENT" and found else "NOT_TESTED",
        "HUMAN_EXPERIENCE": human["human_readability"], "human": human,
        "counts": dict(Counter(row["result"] for row in normalized)),
        "missing_scenarios": [name for name in SCENARIOS if name not in found],
        "scenarios": normalized,
        "claim_limit": "Checks reported evidence structure and file hashes. Does not independently prove gameplay.",
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args(argv)
    try:
        raw = args.input.read_bytes()
        document = json.loads(raw.decode("utf-8-sig"), object_pairs_hook=unique_object)
        summary = summarize(document, args.input, args.output)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        fd, temporary = tempfile.mkstemp(prefix=".client-summary-", dir=args.output.parent)
        try:
            with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
                json.dump(summary, stream, indent=2, ensure_ascii=False)
                stream.write("\n")
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, args.output)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
        print(f"V2_01_CLIENT_GATE={summary['V2_01_CLIENT_GATE']} HUMAN_EXPERIENCE={summary['HUMAN_EXPERIENCE']}")
        return 0 if summary["V2_01_CLIENT_GATE"] == "PASS" else 1
    except (OSError, ValueError, TypeError) as error:
        print(f"INVALID_EVIDENCE: {error}")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
