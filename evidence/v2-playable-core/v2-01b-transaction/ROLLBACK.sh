#!/usr/bin/env bash
set -euo pipefail
base_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
expected=b73f009f52564c7cb495603f51fe549716642a1d4e8a903b36b84933d7f194b4
target="${1:?Pass the destination package JAR path. World files are not changed.}"
test -f "$target"
test "$(realpath -- "$target")" != "$(realpath -- "$base_dir/BASELINE.jar")"
actual="$(sha256sum -- "$base_dir/BASELINE.jar" | cut -d ' ' -f 1)"
test "$actual" = "$expected"
cp -- "$base_dir/BASELINE.jar" "$target"
actual="$(sha256sum -- "$target" | cut -d ' ' -f 1)"
test "$actual" = "$expected"
printf 'ROLLBACK_PACKAGE_RESTORED sha256=%s\n' "$actual"
