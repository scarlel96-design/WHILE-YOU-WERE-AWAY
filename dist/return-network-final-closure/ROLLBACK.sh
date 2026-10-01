#!/usr/bin/env bash
set -euo pipefail

# Removes only the added, sound-on manual-playtest init script.
target="${1:?pass a copy of return-network-human.init.gradle}"
expected="67d3b99d3517b31a4189f5635a4baa49809ec7a85f019718e47eaf869467cc91"
if [[ "$(basename "$target")" != "return-network-human.init.gradle" ]]; then
  echo "ROLLBACK_BLOCKED unexpected file"
  exit 2
fi
actual="$(sha256sum "$target" | cut -d' ' -f1)"
if [[ "$actual" != "$expected" ]]; then
  echo "ROLLBACK_BLOCKED hash mismatch"
  exit 3
fi
rm -- "$target"
echo "ROLLBACK_PASS added init script removed; baseline=ABSENT"
