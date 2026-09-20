#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a disposable modified JAR copy}"
[[ "$TARGET" == *.jar && -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "bf8e234052c8a33ea49cb02c3cabcd941515e29fc126280fdcfa62d4ba0db9c0" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: return-network-contract JAR restored; world files untouched; save downgrade not performed'
