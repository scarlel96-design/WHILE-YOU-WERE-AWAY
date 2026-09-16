#!/usr/bin/env python3
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

def status(name: str) -> str:
    p = STATUS / name
    if not p.is_file():
        return "SKIPPED"
    v = p.read_text(encoding="utf-8", errors="replace").strip().upper()
    return v if v in {"PASS", "FAIL", "SKIPPED"} else "SKIPPED"

def java_version() -> str:
    p = CI / "java-version.txt"
    if not p.is_file():
        return "UNKNOWN"
    lines = [x.strip() for x in p.read_text(encoding="utf-8", errors="replace").splitlines() if x.strip()]
    return lines[0] if lines else "UNKNOWN"

def gametest_counts():
    p = CI / "gradle-gametest.log"
    if not p.is_file():
        return None
    text = p.read_text(encoding="utf-8", errors="replace")
    patterns = [
        re.compile(r"(?i)(\d+)\s+tests?\s+passed(?:,\s*(\d+)\s+failed)?"),
        re.compile(r"(?i)passed\s*[:=]\s*(\d+).*?failed\s*[:=]\s*(\d+)"),
        re.compile(r"(?i)tests?\s*[:=]\s*(\d+).*?fail(?:ed|ures?)\s*[:=]\s*(\d+)"),
    ]
    for i, pattern in enumerate(patterns):
        matches = list(pattern.finditer(text))
        if not matches:
            continue
        m = matches[-1]
        a, b = int(m.group(1)), int(m.group(2) or 0)
        if i < 2:
            return {"total": a + b, "passed": a, "failed": b}
        return {"total": a, "failed": b}
    return None

def jar_info():
    candidates = []
    p = CI / "final-jar.txt"
    if p.is_file():
        raw = p.read_text(encoding="utf-8", errors="replace").strip()
        if raw:
            candidates.append(ROOT / raw)
    libs = ROOT / "build" / "libs"
    if libs.is_dir():
        candidates.extend(sorted(x for x in libs.glob("*.jar") if "-sources" not in x.name and "-javadoc" not in x.name))
    for p in candidates:
        if p.is_file():
            return {"name": p.name, "size": p.stat().st_size, "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
    return {"name": None, "size": None, "sha256": None}

checks = {k: status(k) for k in ("json", "design", "build", "gametest", "asset", "audio")}
required = ("json", "design", "build", "gametest", "asset")
if any(v == "FAIL" for v in checks.values()):
    overall = "CI FAIL"
elif any(checks[k] == "SKIPPED" for k in required):
    overall = "CI PARTIAL"
else:
    overall = "CI PASS"

jar = jar_info()
data = {
    "commit_sha": os.getenv("GITHUB_SHA", "UNKNOWN"),
    "ref": os.getenv("GITHUB_REF", "UNKNOWN"),
    "event": os.getenv("GITHUB_EVENT_NAME", "UNKNOWN"),
    "run_id": os.getenv("GITHUB_RUN_ID", "UNKNOWN"),
    "run_number": os.getenv("GITHUB_RUN_NUMBER", "UNKNOWN"),
    "runner_os": os.getenv("RUNNER_OS", platform.system() or "UNKNOWN"),
    "java_version": java_version(),
    "checks": checks,
    "gametest_counts": gametest_counts(),
    "jar": jar,
    "real_client": "NOT TESTED",
    "overall": overall,
}
(CI / "summary.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

counts = "not reliably detected from log" if data["gametest_counts"] is None else json.dumps(data["gametest_counts"], ensure_ascii=False)
md = f"""# CI verification summary

- Commit SHA: `{data['commit_sha']}`
- Ref: `{data['ref']}`
- Event: `{data['event']}`
- Run: `{data['run_id']}` / number `{data['run_number']}`
- Runner OS: `{data['runner_os']}`
- Java: `{data['java_version']}`
- JSON: **{checks['json']}**
- Design: **{checks['design']}**
- Build: **{checks['build']}**
- GameTest: **{checks['gametest']}**
- Asset: **{checks['asset']}**
- Audio: **{checks['audio']}**
- GameTest count: {counts}
- JAR: `{jar['name']}` ({jar['size']} bytes)
- JAR SHA-256: `{jar['sha256']}`
- REAL CLIENT: **NOT TESTED**
- Overall: **{overall}**

CI/headless GameTest evidence is not REAL CLIENT evidence.
"""
(CI / "summary.md").write_text(md, encoding="utf-8")
step_summary = os.getenv("GITHUB_STEP_SUMMARY")
if step_summary:
    with Path(step_summary).open("a", encoding="utf-8") as f:
        f.write(md)
print(md, end="")
