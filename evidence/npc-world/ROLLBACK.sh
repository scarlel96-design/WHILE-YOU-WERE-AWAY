#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "19636c1b8d8c8b9faf7693f5c05e69b199bb896c4f131c7f72c34331ca099c19" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "9096e4dc9276c8d1eae7aaf2446950e4f76edc5f0f58ee906d7ae70d7268530f" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "19636c1b8d8c8b9faf7693f5c05e69b199bb896c4f131c7f72c34331ca099c19" ]] || exit 5
echo 'ROLLBACK PASS: npc-exceptions JAR restored; world files untouched'
