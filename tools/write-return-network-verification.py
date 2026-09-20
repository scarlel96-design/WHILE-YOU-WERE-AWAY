"""Assemble observed evidence, not a replacement for client assertions."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parents[1]; E=R/'evidence/return-network-integration'
test=(E/'tests-4.log').read_text(encoding='utf-8-sig')
assert 'All 95 required tests passed' in test and 'PASS core assertions=176857' in test and 'BUILD SUCCESSFUL' in test
clients=json.loads((E/'CLIENT_VERIFICATION.json').read_text(encoding='utf-8'))
commands=[json.loads(s) for s in (E/'artifact-commands.jsonl').read_text(encoding='utf-8').splitlines()]
rows=[json.loads(s) for s in (E/'client-final-commands.jsonl').read_text(encoding='utf-8-sig').splitlines()]
text=(R/'docs/RETURN_NETWORK_INTEGRATION_031.md').read_text(encoding='utf-8')
text+='\n\n## Verified execution evidence\n'
text+='AUTOMATED: gradlew.bat --offline runGameTestServer build --rerun-tasks\n'
text+='Latest tests-4.log: All 95 required tests passed; PASS core assertions=176857; BUILD SUCCESSFUL; exit=0.\n'
text+='Baseline source test: 86 run, 3 failed (stale schema expectations), Gradle exit=1, server exit=3. Prior artifact probe is a separate gate.\n'
for r in rows:
 text+=json.dumps(r,ensure_ascii=False)+'\n'
text+='\nIndependent disk/client verification:\n'+json.dumps(clients,ensure_ascii=False,indent=2)+'\n'
text+='\nBASELINE / MODIFIED / ROLLBACK:\n'
for r in commands:text+=json.dumps(r,ensure_ascii=False)+'\n'
rb=E/'rollback-execution.json'
if rb.exists():text+=rb.read_text(encoding='utf-8-sig')+'\n'
text+='\nArtifact paths (reopen/hash verification in ARTIFACTS.json):\n'
for s in ['dist/return-network-integration/whileaway-0.3.1-return-network-integration.jar','dist/return-network-integration/whileaway-0.3.1-return-network-integration-source.zip','evidence/return-network-integration/DIFF.patch','evidence/return-network-integration/VERIFICATION.txt','evidence/return-network-integration/ROLLBACK.sh']:text+=str(R/s)+'\n'
text+='\nStorage: schema5, actorSchema optional1, ReturnNetwork schema1, art2. Reads legacy1/2/3/4 without inventing compound progress. Save downgrade is not supported; rollback replaces a disposable JAR copy only.\n'
text+='Model: current primary agent only; no Terra/Luna delegation, no Caveman, no measured token-cost comparison.\n'
text+='Status: compound event PARTIAL; campaign PARTIAL. No Item/Reward/Fallback implementation or 0.3.2 promotion.\n'
text+='Next: independently loaded NPC/equipment chunk dependencies, additional approach/path/lighting obstruction cases, prior NPC risk-based client regression and scene readability before sealing.\n'
text+='GitHub: upload branch work/return-network-integration; exact verified commit/remote state recorded separately in UPLOAD_RECEIPT.md after push.\n'
(E/'VERIFICATION.txt').write_text(text,encoding='utf-8')
print('VERIFICATION written; source evidence and live-client evidence remain separate')
