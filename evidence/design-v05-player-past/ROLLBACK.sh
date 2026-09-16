#!/usr/bin/env bash
set -euo pipefail
target="${1:-docs/DESIGN_V05.md}"
base="$(cd "$(dirname "$0")/../.." && pwd)/evidence/design-v05-player-past/baseline-DESIGN_V05.md"
cp -f "$base" "$target"
echo "ROLLBACK PASS: DESIGN_V05 restored from baseline; code/JAR/world files untouched"
