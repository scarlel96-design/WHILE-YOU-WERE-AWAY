#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:?Pass a disposable support ZIP copy}"
[[ "$TARGET" == *.zip && -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
ROOT="$(cd -- "$HERE/../.." && pwd)"
ALLOWED="$(realpath -- "$ROOT/build/return-network-support-rollback")"
TARGET="$(realpath -- "$TARGET")"
[[ "$TARGET" == "$ALLOWED/"* ]] || exit 5
BASE="$HERE/baseline/main-source.zip"
EXPECTED="$(cat "$HERE/BUNDLE.sha256")"
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "$EXPECTED" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: disposable support ZIP restored to MAIN source bundle; production source, JAR and worlds untouched'
