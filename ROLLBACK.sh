#!/usr/bin/env bash
# Restore only the discovery-gate policy, not the whole mod or any game save.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$ROOT/evidence/baseline/ThreatPolicy.java"
TARGET="${1:-$ROOT/src/main/java/io/github/whileaway/core/ThreatPolicy.java}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || { echo 'ERROR target must be an existing regular copy'; exit 2; }
BASE_HASH="8b125547cfb45abb58a8849eeb8749582cdfcd0239f1c689c2705575505506c0"
MOD_HASH="1663ed61e6280e99f5ea0a1ac19abf9c02a5502aa9eb9cdbc30dcf775cc2b7b5"
actual="$(sha256sum "$BASE" | cut -d' ' -f1)"
[[ "$actual" == "$BASE_HASH" ]] || { echo 'ERROR baseline hash mismatch'; exit 3; }
actual="$(sha256sum "$TARGET" | cut -d' ' -f1)"
[[ "$actual" == "$MOD_HASH" ]] || { echo 'ERROR target changed; preserving it'; exit 4; }
cp -- "$BASE" "$TARGET"
actual="$(sha256sum "$TARGET" | cut -d' ' -f1)"
[[ "$actual" == "$BASE_HASH" ]] || { echo 'ERROR restored hash mismatch'; exit 5; }
echo 'ROLLBACK PASS: original SHA256 restored'
