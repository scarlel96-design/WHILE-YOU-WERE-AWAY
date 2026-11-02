#!/usr/bin/env bash
set -euo pipefail
here="$(cd -- "$(dirname -- "$0")" && pwd)"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$here/ROLLBACK.ps1")" -Target "${1:?Pass a source ZIP copy path}"
