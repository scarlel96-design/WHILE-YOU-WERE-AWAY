#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:?Pass a disposable package JAR copy}"
test -f "$TARGET"
test "$(realpath -- "$TARGET")" != "$(realpath -- "$HERE/BASELINE.jar")"
EXPECTED=8cd59926e20cfa32ffe4861546fe36d7285ac8306b8b9a5ff3161127132cffd7
test "$(sha256sum "$HERE/BASELINE.jar" | cut -d' ' -f1)" = "$EXPECTED"
cp -- "$HERE/BASELINE.jar" "$TARGET"
test "$(sha256sum "$TARGET" | cut -d' ' -f1)" = "$EXPECTED"
printf 'ROLLBACK_PACKAGE_RESTORED sha256=%s\n' "$EXPECTED"
