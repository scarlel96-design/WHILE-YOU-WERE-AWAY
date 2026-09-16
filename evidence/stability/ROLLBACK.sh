#!/usr/bin/env bash
set -euo pipefail
# JAR copy only. Never downgrade world saves.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass an existing modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "86b70224347ee8b527e0bd21041dba6009f263caa45d6395aaff3a3380283e49" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "90085cc811858d41865c5a9973acdef8e8cc69ac560a5c80120ded378da59da1" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "86b70224347ee8b527e0bd21041dba6009f263caa45d6395aaff3a3380283e49" ]] || exit 5
echo 'ROLLBACK PASS: 0.3 JAR hash restored; world files untouched'
