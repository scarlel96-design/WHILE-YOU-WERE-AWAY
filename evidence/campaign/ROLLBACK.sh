#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255" ]] || exit 5
echo 'ROLLBACK PASS: lifecycle-work JAR restored; world files untouched'
