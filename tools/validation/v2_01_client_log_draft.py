"""Prepare an untested client evidence draft from one explicitly selected log."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import tempfile

from v2_01_client_evidence import HUMAN_FIELDS, SCENARIOS


MARKERS = {
    "render_thread": r"\[Render thread/",
    "sound_engine_started": r"Sound engine started",
    "world_preparation": r"Preparing start region for dimension",
    "player_join": r"joined the game",
    "normal_stop": r"\[minecraft/Minecraft\]: Stopping!",
    "gradle_success": r"BUILD SUCCESSFUL",
    "error_or_kernel_failure": r"\[[^\]]*/ERROR\]|failed to write kernel assets|Traceback \(most recent call last\)",
}


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def prepare(log: Path, output: Path, profile="", build_sha=None, jar: Path | None = None):
    """Publish a new directory. Never infer acceptance or overwrite an old session."""
    log, output = log.resolve(), output.resolve()
    if output.exists():
        raise ValueError("OUTPUT_ALREADY_EXISTS")
    if bool(build_sha) != bool(jar):
        raise ValueError("TARGET_REQUIRES_BUILD_AND_JAR")
    if build_sha is not None and not re.fullmatch(r"[0-9a-fA-F]{40}", build_sha):
        raise ValueError("INVALID_TARGET_BUILD_SHA")
    raw = log.read_bytes()
    text = raw.decode("utf-8-sig", errors="replace")
    lines = text.splitlines()
    target = None
    if jar is not None:
        jar_bytes = jar.read_bytes()
        target = {"build_sha": build_sha.lower(), "jar_sha256": digest(jar_bytes),
                  "jar_size": len(jar_bytes), "jar_filename": jar.name,
                  "binding": "USER_DECLARED_TARGET_NOT_EXECUTION_PROOF"}
    markers = {}
    for name, pattern in MARKERS.items():
        matches = [i for i, line in enumerate(lines, 1) if re.search(pattern, line)]
        markers[name] = {"count": len(matches), "first_line_numbers": matches[:12]}
    review = {"schema": 1, "source_filename": log.name, "source_sha256": digest(raw),
              "source_size": len(raw), "line_count": len(lines), "markers": markers,
              "source_kind": "UNCLASSIFIED_LOG", "inferred_acceptance": False,
              "target_build": target,
              "claim_limit": "Log markers do not prove client scenarios, human ratings, or the executed build."}
    rows = [{"scenario": name, "result": "NOT_TESTED", "build_sha": None, "jar_sha256": None,
             "isolated_profile": profile, "world_identity": "", "start_condition": "",
             "observed_behavior": "",
             "expected_behavior": f"See docs/V2_01_CLIENT_VALIDATION_PROTOCOL.md: {name}",
             "evidence_reference": [], "tester": "", "tester_kind": "NOT_RUN", "notes": "",
             **dict.fromkeys(HUMAN_FIELDS, "NOT_TESTED")} for name in SCENARIOS]
    session = {"schema": 1, "evidence_mode": "REAL_CLIENT", "log_review": "log-review.json",
               "candidate_log": "references/source.log", "target_build": target, "scenarios": rows}
    output.parent.mkdir(parents=True, exist_ok=True)
    stage = Path(tempfile.mkdtemp(prefix=".v2-client-draft-", dir=output.parent))
    try:
        (stage / "references").mkdir()
        (stage / "references/source.log").write_bytes(raw)
        for name, content in (("log-review.json", review), ("client-session.json", session)):
            with (stage / name).open("w", encoding="utf-8", newline="\n") as stream:
                json.dump(content, stream, indent=2, ensure_ascii=False)
                stream.write("\n")
                stream.flush()
                os.fsync(stream.fileno())
        if digest((stage / "references/source.log").read_bytes()) != review["source_sha256"]:
            raise ValueError("LOG_COPY_HASH_MISMATCH")
        # Check again before publication. os.rename also rejects occupied directories.
        if output.exists():
            raise ValueError("OUTPUT_ALREADY_EXISTS")
        os.rename(stage, output)
    finally:
        if stage.exists():
            shutil.rmtree(stage)
    return review


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    parser.add_argument("output_directory", type=Path)
    parser.add_argument("--profile", default="")
    parser.add_argument("--target-build-sha")
    parser.add_argument("--target-jar", type=Path)
    args = parser.parse_args(argv)
    try:
        prepare(args.log, args.output_directory, args.profile, args.target_build_sha, args.target_jar)
        print("DRAFT_CREATED scenarios=15_NOT_TESTED REAL_CLIENT=NOT_TESTED")
        return 0
    except (OSError, ValueError) as error:
        print(f"DRAFT_ERROR: {error}")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
