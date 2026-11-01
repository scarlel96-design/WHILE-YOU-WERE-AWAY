#!/usr/bin/env bash
set -euo pipefail
if [ "$#" -ne 1 ]; then
  echo 'Usage: ROLLBACK.sh EXPLICIT_PACKAGE_COPY.jar' >&2
  exit 2
fi
here="$(cd -- "$(dirname -- "$0")" && pwd)"
expected=8cd59926e20cfa32ffe4861546fe36d7285ac8306b8b9a5ff3161127132cffd7
printf '%s  %s\n' "$expected" "$here/BASELINE.jar" | sha256sum --check --status
cp -- "$here/BASELINE.jar" "$1"
actual="$(sha256sum -- "$1" | cut -d ' ' -f 1)"
test "$actual" = "$expected"
printf 'ROLLBACK_PACKAGE_RESTORED sha256=%s\n' "$actual"
