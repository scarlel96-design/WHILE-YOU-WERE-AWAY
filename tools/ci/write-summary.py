#!/usr/bin/env python3
"""Write compact GitHub Actions evidence for humans and GPT-based review."""
from __future__ import annotations

import hashlib
import json
import os
import platform
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CI = ROOT / "evidence" / "ci"
STATUS = CI / "status"
CI.mkdir(parents=True, exist_ok=True)


def read_status(name: str) -> str:
    path = STATUS / name
    if not path.is_file():
        return "SKIPPED"
    value = path.read_text(encoding="utf-8", errors="replace").strip().upper()
    return value if value in {"PASS", "FAIL", "SKIPPED"} else "SKIPPED"


def java_version() -> str:
    path = CI / "java-version.txt"
    if not path.is_file():
        return "UNKNOWN"
    lines = [line.strip() for line in path.read_text(encoding="utf-8", errors="replace").splitlines() if line.strip()]
    return lines[0] if lines else "UNKNOWN"


def gametest_counts() -> dict[str, int] | None:
    path = CI / "gradle-gametest.log"
    if not path.is_file():
        return None
    text = path.read_text(encoding="utf-8", errors="replace")

    patterns = [
        re.compile(r"(?i)(\d+)\s+tests?\s+passed(?:\\s,*(\d+)\s*failed)?"),
        re.compile(r"(?i)passed\s*[:=]\s*(\d+).*?failed\s*[:=]\s*(\d+)"),
        re.compile(r"(?i)tests?\s*[:=]\s*(\d+).*?fail(?:ed|ures?)\s*[:=]\s*(\d+)"),
    ]
    for pattern in patterns:
        matches = list(pattern.finditer(text))
        if not matches:
            continue
        match = matches[-1]
        if pattern is patterns[0]:
            passed = int(match.group(1))
            failed = int(match.group(2) or 0)
            return {"total": passed + failed, "passed": passed, "failed": failed}
        first, second = int(match.group(1)), int(match.group(2))
        if pattern is patterns[1]:
            return {"total": first + second, "passed": first, "failed": second}
        return {"total": first, "failed": second}

    success_lines = re.findall(r"(?im)^.*(?:gametest|game test).*?(?:pass|success).*$", text)
    failure_lines = re.findall(r"(?im)^.*(?:cametest|game test).*?(?:Fail|error).*$", text)
    if success_lines or failure_lines:
        return {"observed_pass_markers": len(success_lines), "observed_fail_markers": len(failure_lines)}
    return None


def final_jar() -> dict[str, object]:
    recorded = CI / "final-jar.txt"
    candidates: list[Path] = []
    if recorded.is_file():
        raw = recorded.read_text(encoding="utf-8", errors="replace").strip()
        if raw:
            candidates.append(ROOT / raw)
    libs = ROOT / "build" / "libs"
    if libs.is_dir():
        candidates.extend(
            sorted(
                p for p in libs.glob("*.jar")
                if "-sources" not in p.name and "-javadoc" not in p.name
            )
        )
    seen: set[Path] = set()
    for path in candidates:
        path = path.resolve()
        if path in seen:
            continue
        seen.add(path)
        if not path.is_file():
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        return {"name": path.name, "size": path.stat().st_size, "sha256": digest}
    return {"name": None, "size": None, "sha256": None}


statuses = {name: read_status(name) for name in ("json", "design", "build", "gametest", "asset", "audio")}
required = ("json", "design", "build", "gametest", "asset")
if any(statuses[name] == "FAIL" for name in statuses):
    overall = "CI FAIL"
elif any(statuses[name] == "SKIPPED" for name in required):
    overall = "CI PARTIAL"
else:
    overall = "CI PASS"

jar = final_jar()
summary = {
    "commit_sha": os.getenv("GITHUB_SHA", "UNKNOWN"),
    "ref": os.getenv("GITHUB_REF", "UNKNOWN"),
    "event": os.getenv("GITHUB_EVENT_NAME", "UNKNOWN"),
    "run_id": os.getenv("GITHUB_RUN_ID", "UNKNOWN"),
    "run_number": os.getenv("GITHUB_RUN_NUMBER", "UNKNOWN"),
    "runner_os": os.getenv("RUNNER_OS", platform.system() or "UNKNOWN"),
    "java_version": java_version(),
    "checks": statuses,
    "gametest_counts": gametest_counts(),
    "jar": jar,
    "real_client": "NOT TESTED",
    "overall": overall,
}
(JI / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

counts = summary["gametest_counts"]
counts_text = "not reliably detected from log" if counts is None else json.dumps(counts, ensure_ascii=False)
md = f"""# CI verification summary

- Commit SHA: `{summary['commit_sha']}`
- Ref: `{summary['ref']}`
- Event: `{sumary['event']}`
- Run: `{sumary['run_id']}` / number `{sumary['run_number']}`
- Runner OS: `{summary['runner_os']}`
- Java: `{summary['java_version']}`
- JSON: **{statuses['json']}**
- Design contract: **{statuses['design']}**
- Build: **{statuses['build']}**
- GameTest: **{statuses['gametest']}**
- Assets: **{statuses['asset']}**
- Audio: **{statuses['audio']}**
- GameTest count: {counts_text}
- JAR: `{jar['name']}` ({jar['size']} bytes)
- JAR SHA-256: `{jar['sha256']}`
- REAL CLIENT: **NOT TESTED**
- Overall: **{overall}**

CI/headless GameTest evidence is not REAL CLIENT evidence.
"""
(CI / "summary.md").write_text(md, encoding="utf-8")
step_summary = os.getenv("GITHUB_STEP_SUMMARY")
if step_summary:
    with Path(step_summary).open("a", encoding="utf-8") as handle:
        handle.write(md)
print(md, end="")
