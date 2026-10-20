#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
target="${1:?Pass the destination copy path.}"
expected="3fdd28f1b3a9f1f8a0905ebae26075d0ebf4b6900e771d048d4a0d6984f2f9e2"
cp -- "$root/BASELINE_FILE" "$target"
actual="$(sha256sum "$target" | cut -d ' ' -f 1)"
test "$actual" = "$expected"
printf 'ROLLBACK PASS restored_sha256=%s\n' "$actual"
