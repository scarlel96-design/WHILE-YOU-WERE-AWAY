#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
baseline="$script_dir/../../evidence/return-network-signal/baseline/main.jar"
expected="9e90a695dfa14ab193849335cb28dffa0d27cb8d212cc87387e260b0f8cb637e"
target="${1:?usage: ROLLBACK.sh TARGET_JAR}"

test -f "$baseline"
test -f "$target"
test "$baseline" != "$target"
cp -- "$baseline" "$target"
actual="$(sha256sum "$target" | cut -d ' ' -f 1)"
test "$actual" = "$expected"
printf 'ROLLBACK_OK SHA256=%s\n' "$actual"
