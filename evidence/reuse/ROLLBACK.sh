#!/usr/bin/env bash
set -euo pipefail
# Restores only a caller-supplied JAR copy. No world file operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass the modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "90085cc811858d41865c5a9973acdef8e8cc69ac560a5c80120ded378da59da1" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "9062d03914ff6dc8211e77342d785734c6263bd3af5bfb2fa5ce344a50c580d6" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "90085cc811858d41865c5a9973acdef8e8cc69ac560a5c80120ded378da59da1" ]] || exit 5
echo 'ROLLBACK PASS: adopted 0.3.1 JAR restored; world files untouched'
