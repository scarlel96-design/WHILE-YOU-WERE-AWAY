#!/usr/bin/env bash
set -euo pipefail
HERE=$(cd -- "$(dirname -- "$0")" && pwd)
if [ "$#" -ne 1 ]; then printf '%s
' 'Usage: ROLLBACK.sh EXISTING_JAR_COPY'; exit 64; fi
exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$HERE/ROLLBACK.ps1" -Target "$1"
