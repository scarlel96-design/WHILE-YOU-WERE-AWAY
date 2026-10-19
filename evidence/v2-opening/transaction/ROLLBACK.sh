#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo 'usage: ROLLBACK.sh TARGET_COPY' >&2
  exit 2
fi

here="$(cd -- "$(dirname -- "$0")" && pwd)"
cp -- "$here/BASELINE_FILE" "$1"
sha256sum -- "$1"
