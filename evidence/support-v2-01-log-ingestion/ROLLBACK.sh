#!/usr/bin/env bash
set -euo pipefail
if [ "$#" -ne 1 ]; then
  echo "Usage: ROLLBACK.sh evidence/support-v2-01-log-ingestion/rollback-copy/tool.py" >&2
  exit 2
fi
root="$(cd -- "$(dirname -- "$0")" && pwd)"
python - "$root" "$1" <<'PY'
import hashlib
from pathlib import Path
import sys
root=Path(sys.argv[1]).resolve()
target=Path(sys.argv[2]).resolve()
allowed=root/'rollback-copy'
if not target.is_relative_to(allowed) or target.suffix != '.py':
    raise SystemExit('ROLLBACK_TARGET_REJECTED')
raw=(root/'BASELINE_FILE').read_bytes()
hash_value=hashlib.sha256(raw).hexdigest()
if hash_value != 'f54de65aa1aa62265ae960f4dcba3c71fcd00ec9aca0c19a6d23b98a3d94d636':
    raise SystemExit('BASELINE_HASH_MISMATCH')
target.parent.mkdir(parents=True,exist_ok=True)
target.write_bytes(raw)
if hashlib.sha256(target.read_bytes()).hexdigest() != hash_value:
    raise SystemExit('ROLLBACK_HASH_MISMATCH')
print('ROLLBACK_TOOL_RESTORED sha256='+hash_value)
PY
