#!/usr/bin/env bash
set -euo pipefail
# Restores an explicitly supplied JAR copy; never edits worlds or source files.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass an existing modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || { echo 'ERROR existing regular JAR required'; exit 2; }
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "70ea359aa99044c542379fbc95a1d204d884446e9891db281e9bfa22a881848e" ]] || { echo 'ERROR baseline changed'; exit 3; }
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "86b70224347ee8b527e0bd21041dba6009f263caa45d6395aaff3a3380283e49" ]] || { echo 'ERROR target differs; preserving it'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "70ea359aa99044c542379fbc95a1d204d884446e9891db281e9bfa22a881848e" ]] || exit 5
echo 'ROLLBACK PASS: 0.2 JAR hash restored; world files untouched'
