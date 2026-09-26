"""Package the current Return Network source and verified JAR without touching baselines."""

from __future__ import annotations

import hashlib
import shutil
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "dist" / "return-network-signal"
JAR_INPUT = ROOT / "build" / "libs" / "whileaway-0.3.1-dev.1.jar"
JAR_OUTPUT = OUT / "whileaway-0.3.1-return-network-signal.jar"
SOURCE_OUTPUT = OUT / "whileaway-0.3.1-return-network-signal-source.zip"
ROOT_FILES = (
    ".gitattributes", ".gitignore", "AGENTS.md", "README.md", "build.gradle",
    "gradle.properties", "gradlew", "gradlew.bat", "settings.gradle",
)
SOURCE_DIRS = ("src", "docs", "tools", "gradle", ".github")
EXCLUDED_PARTS = {"__pycache__", ".gradle", "build", "dist", "evidence", "logs", ".git"}


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def main() -> None:
    if not JAR_INPUT.is_file():
        raise SystemExit(f"missing verified build: {JAR_INPUT}")
    OUT.mkdir(parents=True, exist_ok=True)
    shutil.copy2(JAR_INPUT, JAR_OUTPUT)
    files = [ROOT / name for name in ROOT_FILES if (ROOT / name).is_file()]
    for directory in SOURCE_DIRS:
        files.extend(
            path for path in (ROOT / directory).rglob("*")
            if path.is_file()
            and not any(part in EXCLUDED_PARTS for part in path.relative_to(ROOT).parts)
            and path.suffix != ".pyc"
        )
    with zipfile.ZipFile(SOURCE_OUTPUT, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in sorted(files, key=lambda p: p.relative_to(ROOT).as_posix()):
            archive.write(path, path.relative_to(ROOT).as_posix())
    for path in (JAR_OUTPUT, SOURCE_OUTPUT):
        print(f"PACKAGED {path} size={path.stat().st_size} sha256={digest(path)}")
    with zipfile.ZipFile(SOURCE_OUTPUT) as archive:
        assert archive.testzip() is None
        print(f"SOURCE_ZIP_ENTRIES={len(archive.namelist())} TESTZIP=PASS")


if __name__ == "__main__":
    main()
