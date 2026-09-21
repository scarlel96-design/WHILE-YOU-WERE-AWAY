#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd -- "$HERE/../.." && pwd)"
BASE="$HERE/baseline/main.jar"
TARGET="$(realpath -- "${1:?Pass disposable JAR copy in build/rollback-return-network-closure}")"
ALLOWED="$(realpath -- "$ROOT/build/rollback-return-network-closure")"
[[ "$TARGET" == "$ALLOWED/"*.jar && -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "9e90a695dfa14ab193849335cb28dffa0d27cb8d212cc87387e260b0f8cb637e" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: previous verified MAIN JAR restored; world files untouched; no save downgrade'
