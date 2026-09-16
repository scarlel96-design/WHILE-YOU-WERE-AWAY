#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
cp "$root/evidence/upload-policy/baseline-AGENTS.md" "$root/AGENTS.md"
cp "$root/evidence/upload-policy/baseline-README.md" "$root/README.md"
echo "ROLLBACK PASS: upload policy documents restored"
