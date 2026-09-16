#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "82b0201c0067bc50fe931e0daf24b54c2bef31cd07e2cc63845ae4677beca542" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "80a80cf1dd741744c4b6c7c2bed21264437d651c5421d9198f13b40e3ca79e51" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "82b0201c0067bc50fe931e0daf24b54c2bef31cd07e2cc63845ae4677beca542" ]] || exit 5
echo 'ROLLBACK PASS: canonical-recovery JAR restored; world files untouched'
