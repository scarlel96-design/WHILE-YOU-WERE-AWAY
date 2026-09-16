#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "1fd5ef7e63960ac9e79d7ef7d11226f639457e4569e661809465b8aa6654e61b" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "db567920ac9240a375a68170868cc6410dcde0a397d541a805e345e3ae88501a" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: npc-environment JAR restored; world files untouched'
