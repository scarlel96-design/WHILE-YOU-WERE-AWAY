#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "db567920ac9240a375a68170868cc6410dcde0a397d541a805e345e3ae88501a" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "bf8e234052c8a33ea49cb02c3cabcd941515e29fc126280fdcfa62d4ba0db9c0" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: npc-path JAR restored; world files untouched'
