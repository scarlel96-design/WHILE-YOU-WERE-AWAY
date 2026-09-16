#!/usr/bin/env bash
set -euo pipefail
# JAR-copy rollback only. World/schema migration is not undone.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/whileaway-0.1.0-dev.1.jar"
TARGET="${1:?Pass an existing modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || { echo 'ERROR existing regular JAR required'; exit 2; }
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "3a3d083731506450772d247f2a6df4bd0126db173c9b75f169b2a23e0902e49b" ]] || { echo 'ERROR baseline changed'; exit 3; }
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "70ea359aa99044c542379fbc95a1d204d884446e9891db281e9bfa22a881848e" ]] || { echo 'ERROR target differs; preserving it'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "3a3d083731506450772d247f2a6df4bd0126db173c9b75f169b2a23e0902e49b" ]] || exit 5
echo 'ROLLBACK PASS: 0.1 JAR hash restored; world files untouched'
