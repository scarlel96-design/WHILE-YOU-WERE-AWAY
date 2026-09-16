#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "3d3b08beddd7e9fae87c41188c31251e941ee9da9b6697b50bcf96a2fd960766" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "a66cf05706329faab022af12d9ab016dc61b8e719724858f70c3449f5ae8ad03" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "3d3b08beddd7e9fae87c41188c31251e941ee9da9b6697b50bcf96a2fd960766" ]] || exit 5
echo 'ROLLBACK PASS: wayfarer-stability JAR restored; world files untouched'
