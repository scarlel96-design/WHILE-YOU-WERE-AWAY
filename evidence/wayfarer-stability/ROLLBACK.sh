#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "3d3b08beddd7e9fae87c41188c31251e941ee9da9b6697b50bcf96a2fd960766" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8" ]] || exit 5
echo 'ROLLBACK PASS: campaign-pilot JAR restored; world files untouched'
