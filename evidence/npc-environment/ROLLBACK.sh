#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "9096e4dc9276c8d1eae7aaf2446950e4f76edc5f0f58ee906d7ae70d7268530f" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "1fd5ef7e63960ac9e79d7ef7d11226f639457e4569e661809465b8aa6654e61b" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: npc-world JAR restored; world files untouched'
