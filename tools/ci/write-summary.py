#!/usr/bin/env python3

from __future__ import annotations

import hashlib
import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / "evidence" / "ci"
EVIDENCE.mkdir(parents=True, exist_ok=True)

GRADLE_LOG = EVIDENCE / "gradle-gametest.log"
SUMMARY_JSON = EVIDENCE / "summary.json"
SUMMARY_MD = EVIDENCE / "summary.md"


def env(name: str, default: str = "unknown") -> str:
    value = os.environ.get(name)
    if value is None or not value.strip():
        return default
    return value.strip()


def normalize_outcome(value: str) -> str:
    mapping = {
        "success": "PASS",
        "failure": "FAIL",
        "cancelled": "CANCELLED",
        "skipped": "SKIPPED",
        "unknown": "UNKNOWN",
    }
    return mapping.get(value.lower(), value.upper())


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def find_jar() -> Path | None:
    lib = ROOT / "build" / "libs"

    if not lib.exists():
        return None

    jars = [
        p
        for p in lib.glob("*.jar")
        if not p.name.endswith("-sources.jar")
        and not p.name.endswith("-javadoc.jar")
    ]

    if not jars:
        return None

    # deterministic selection
    jars.sort(key=lambda p: p.name)
    return jars[0]


def detect_gametest_count() -> int | None:
    if not GRADLE_LOG.exists():
        return None

    text = GRADLE_LOG.read_text(
        encoding="utf-8",
        errors="replace",
    )

    patterns = [
        r"All\s+(\d+)\s+required tests passed",
        r"All\s+(\d+)\s+tests passed",
        r"GameTests?.*?(\d+).*?passed",
    ]

    for pattern in patterns:
        matches = re.findall(
            pattern,
            text,
            flags=re.IGNORECASE | re.DOTALL,
        )

        if matches:
            try:
                return int(matches[-1])
            except ValueError:
                pass

    return None


def overall_status(results: dict[str, str]) -> str:
    values = set(results.values())

    if "FAIL" in values:
        return "CI FAIL"

    if "CANCELLED" in values:
        return "CI CANCELLED"

    required = (
        "build",
        "gametest",
        "design",
        "assets",
    )

    if all(results.get(name) == "PASS" for name in required):
        return "CI PASS"

    return "CI PARTIAL"


jar = find_jar()

results = {
    "json": normalize_outcome(env("CI_JSON_OUTCOME")),
    "design": normalize_outcome(env("CI_DESIGN_OUTCOME")),
    "build": normalize_outcome(env("CI_BUILD_OUTCOME")),
    "gametest": normalize_outcome(env("CI_GAMETEST_OUTCOME")),
    "assets": normalize_outcome(env("CI_ASSET_OUTCOME")),
    "audio": normalize_outcome(env("CI_AUDIO_OUTCOME")),
}

jar_info = None

if jar is not None:
    jar_info = {
        "name": jar.name,
        "path": str(jar.relative_to(ROOT)).replace("\\", "/"),
        "size_bytes": jar.stat().st_size,
        "sha256": sha256(jar),
    }

summary = {
    "schema": 1,
    "project": "WHILE-YOU-WERE-AWAY",
    "generated_utc": datetime.now(timezone.utc).isoformat(),
    "commit": env("GITHUB_SHA"),
    "ref": env("GITHUB_REF"),
    "event": env("GITHUB_EVENT_NAME"),
    "run_id": env("GITHUB_RUN_ID"),
    "run_number": env("GITHUB_RUN_NUMBER"),
    "runner_os": env("RUNNER_OS"),
    "java": env("CI_JAVA_VERSION", "21"),
    "results": results,
    "gametest_count": detect_gametest_count(),
    "jar": jar_info,

    # 절대 CI가 실제 클라이언트 시험을 했다고 주장하지 않는다.
    "real_client": "NOT TESTED",

    "overall": overall_status(results),
}

SUMMARY_JSON.write_text(
    json.dumps(
        summary,
        ensure_ascii=False,
        indent=2,
    )
    + "\n",
    encoding="utf-8",
)

lines = [
    "# WHILE YOU WERE AWAY — CI Summary",
    "",
    f"- Overall: **{summary['overall']}**",
    f"- Commit: `{summary['commit']}`",
    f"- Runner: `{summary['runner_os']}`",
    f"- Java: `{summary['java']}`",
    "",
    "## Validation",
    "",
    "| Gate | Result |",
    "|---|---|",
]

for name, value in results.items():
    lines.append(f"| {name} | {value} |")

lines.extend(
    [
        "",
        f"- GameTest count: `{summary['gametest_count']}`",
        f"- Real Minecraft client: **{summary['real_client']}**",
    ]
)

if jar_info is not None:
    lines.extend(
        [
            "",
            "## Artifact",
            "",
            f"- JAR: `{jar_info['name']}`",
            f"- Size: `{jar_info['size_bytes']}` bytes",
            f"- SHA-256: `{jar_info['sha256']}`",
        ]
    )
else:
    lines.extend(
        [
            "",
            "## Artifact",
            "",
            "- JAR: NOT AVAILABLE",
        ]
    )

SUMMARY_MD.write_text(
    "\n".join(lines) + "\n",
    encoding="utf-8",
)

print(json.dumps(summary, ensure_ascii=False, indent=2))
