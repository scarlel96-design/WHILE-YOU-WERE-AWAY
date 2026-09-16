#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "80a80cf1dd741744c4b6c7c2bed21264437d651c5421d9198f13b40e3ca79e51" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "19636c1b8d8c8b9faf7693f5c05e69b199bb896c4f131c7f72c34331ca099c19" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "80a80cf1dd741744c4b6c7c2bed21264437d651c5421d9198f13b40e3ca79e51" ]] || exit 5
echo 'ROLLBACK PASS: npc-lifecycle JAR restored; world files untouched'
