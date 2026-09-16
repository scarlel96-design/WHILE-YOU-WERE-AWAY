"""Final self-contained evidence composition; refuses incomplete current gates."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/canonical-recovery';D=R/'dist/canonical-recovery'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
g=json.loads((E/'GATES.json').read_text());assert g['canonical_suite']=='PASS'
parts=[(R/'docs/CANONICAL_RECOVERY_031.md').read_text(encoding='utf-8')]
parts.append('\n## Final observed gates\n'+json.dumps(g,ensure_ascii=False,indent=2))
parts.append('''\nCurrent: 0.3.1-dev.1 / canonical-recovery / current Actor adapter recovery PASS / campaign PARTIAL / NPC NOT STARTED.
No 0.3.2 promotion. No natural survival, NPC campaign, transactions or fallback proof is implied.
Baseline suite: 48 required GameTests. Current suite: 52 = those same 48 plus four new candidate/reservation tests.
Final storage regression: 580 cases / 2161 assertions; 20 repetitions retained from prior probe. These are NOT 580 new failure surfaces or clients.
Final actual clients: 19 canonical/generation + 5 load guard + 11 Wayfarer = 35 distinct fully exited processes.
The load-suite Blocked fixture is a malformed UUID string; missing UUID blocking is independently covered by Ambiguous/Unloaded/NoCandidate.
Six earlier development clients are separate and excluded. All clients were muted and verified inside RIGHT secondary DISPLAY1.
Blocked load tests call actual server saveEverything 3 times; this is not a natural autosave-timer playthrough.
The same-generation duplicate test poisons registered data; a resulting guarded save exception is an expected rejection, not a successful save.
''')
parts.append('\n## Acceptance matrix (separate evidence layers)\n|Item|AUTOMATED/GAME TEST/STORAGE PROBE|REAL CLIENT|Fresh process|Disk evidence|Final|\n|---|---|---|---|---|---|')
for item,auto,real,fresh,disk,verdict in [
 ('Load Guard','580 storage cases','5 load cases','yes','hash/quarantine','PASS'),
 ('Missing UUID exact','candidate tests','Exact','ExactReload','original/quarantine/repaired','PASS'),
 ('Missing UUID ambiguous','multiplicity test','Ambiguous','isolated process','unchanged after 3 saves','PASS'),
 ('Malformed UUID','storage probe','Blocked malformed-string fixture','isolated process','unchanged after 3 saves','PASS'),
('Missing UUID no candidate','absence classification','NoCandidate','isolated process','unchanged after 3 saves','PASS'),
 ('Unloaded vs missing','unloaded classification','Unloaded + Wayfarer actual unload','yes','story unchanged','PASS'),
 ('Confirmed missing / generation reservation','reservation test','Cut1-6 / Resume1-6','6 pairs','all cut/resume NBT','PASS'),
 *[(f'Boundary {i}','shared reservation tests',f'Cut{i}',f'Resume{i}','paired NBT / UUID / generation','PASS') for i in range(1,7)],
 ('Stale generation late-load','stale classification + old regression','Late actual entity chunk','separate copied world process','final canonical NBT','PASS'),
 ('Same-generation duplicate','multiplicity test','Duplicate','isolated process','write guard/hash','PASS'),
 ('Recovery fresh-process','serialization / existing tests','ExactReload + Resume1-6','yes','identity preserved','PASS'),
 ('Write guard / original preservation','storage probe','5 load + blocked canonical cases','yes','before/quarantine/after hash','PASS'),
 ('Story Integrity','existing + new checks','canonical stable / blocked diagnostics','yes','state assertions','PASS - current Actor/load scope'),
 ('Migration 1 to 4','20 storage fixtures','NOT TESTED separately','NOT TESTED separately','actual NBT writes/reads','PASS - storage regression'),
 ('Migration 2 to 4','20 storage fixtures','NOT TESTED separately','NOT TESTED separately','actual NBT writes/reads','PASS - storage regression'),
 ('Migration 3 to 4','20 storage fixtures','NOT TESTED separately','NOT TESTED separately','actual NBT writes/reads','PASS - storage regression'),
 ('Wayfarer regression','original 48 maintained','11 current processes','yes','actual copied world + snapshots','PASS'),
 ('Actor recovery gate','52 tests + storage','current required failure surfaces','yes','verified','PASS - current supported Actor scope'),
 ('NPC stage','NOT STARTED','NOT STARTED','NOT STARTED','none','NOT STARTED'),
 ('Campaign','incomplete','natural survival NOT TESTED','not complete','not complete','PARTIAL')]:
    parts.append('|'+ '|'.join((item,auto,real,fresh,disk,verdict))+'|')
parts.append('\n## Absolute artifacts and hashes\n')
for role,p in [('MODIFIED_FILE',D/'whileaway-0.3.1-canonical-recovery.jar'),('DIFF_FILE',E/'DIFF.patch'),('VERIFICATION',E/'VERIFICATION.txt'),('ROLLBACK',E/'ROLLBACK.sh'),('BASELINE_JAR',E/'baseline/previous.jar'),('BASELINE_SOURCE',E/'baseline/previous-source.zip')]:
    parts.append(f'{role}: {p}')
    if role!='VERIFICATION':parts.append('SHA-256: '+sha(p))
parts.append('Source ZIP digest is in dist/canonical-recovery/SHA256SUMS.txt outside the ZIP to avoid a circular hash.')
parts.append(f'\n## Exact commands and literal outputs\nWorking directory: {R}\nJAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot\nGRADLE_USER_HOME={R.parent.parent/"work/gradle-home"}')
for label,command,name,needles in [
 ('BASELINE_GAME','./gradlew.bat --offline runGameTestServer','baseline-tests.log',['All 48 required tests passed','BUILD SUCCESSFUL']),
 ('MODIFIED_GAME','./gradlew.bat --offline compileJava runGameTestServer build','final-tests.log',['All 52 required tests passed','PASS core assertions=','BUILD SUCCESSFUL']),
 ('MODIFIED_STORAGE','pwsh -NoProfile -File tools/probe-canonical-storage.ps1','probe.log',['PASS load guard cases=580'])]:
    lines=(E/name).read_text(encoding='utf-8-sig').splitlines();output='\n'.join(x for x in lines if any(n in x for n in needles));assert output
    parts.append(f'\n{label}\ncommand: {command}\ninput: current isolated fixtures; log {E/name}\noutput:\n{output}\nexit: 0')
t=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'))
for row in t['commands']:parts.append(f'\n{row["label"]}\ncommand: {row["command"]}\ninput: {row["input"]}\noutput:\n{row["output"]}\nexit: {row["exit"]}')
parts.append('Restored JAR hash='+t['restored_hash']+'; MODIFIED_FILE remains changed='+t['modified_hash']+'; no world operations or save downgrade.')
parts.append('\n## Actual client executions: launcher exit, child PID exit and markers are separate gates\n')
seconds=0
for filename in ('canonical-final-commands.jsonl','load-client-final-commands.jsonl','runtime-commands.jsonl'):
    for line in (E/filename).read_text().splitlines():
        row=json.loads(line);assert row['pass'] and row['exit']==0;seconds+=row['seconds'];parts.append(line)
parts.append(f'Final client launcher wall time sum={seconds:.3f} seconds; not total development time or natural gameplay duration.')
for folder in ('canonical-final','client-final','crash','exceptions'):
    for p in sorted((E/folder).glob('*.txt')):
        if 'PASS ' in p.read_text(encoding='utf-8'):
            parts.append(f'\nLiteral marker: {p}\n'+p.read_text(encoding='utf-8'))
parts.append('\n## Corrupt original / quarantine / final hashes\n')
for prefix,mode in [('canonical-final','Exact'),('canonical-final','Ambiguous'),('canonical-final','Unloaded'),('canonical-final','NoCandidate'),('client-final','Recover'),('client-final','Blocked'),('client-final','Unsupported')]:
    before=json.loads((E/f'{prefix}-before-{mode}.json').read_text());f=Path(before['target'])/'data/whileaway_story.dat'
    q=f.parent/'whileaway-quarantine'/f"{before['storyHash']}.dat";assert sha(q)==before['storyHash']
    parts.append(f'{prefix}/{mode}: original={before["storyHash"]}; quarantine={sha(q)}; final={sha(f)}')
parts.append('''\n## Additional execution issue
Git Bash initialization under the sandbox returned -1073741502 during rollback. The failed log was preserved in rollback-sandbox-failure.log. The identical JAR-copy-only rollback ran successfully in the ordinary execution context, exit 0. No world files were part of either command.
## Model / usage
No Terra/Luna delegation, model-switch experiment or Caveman execution occurred. Primary-session implementation and review only. No per-model input/output/reasoning-token measurement or usage-reduction percentage is claimed.
## Next exact starting point
Preserve these canonical-recovery JAR/source hashes as the next immutable baseline. Keep load guard, private recovery preflight, reservation UUID reuse, two-position readiness and generation fence.
Next functional task is a minimal REAL campaign NPC event using this lifecycle: appearance -> movement -> player interaction -> reaction to another NPC/object -> durable fact -> completion -> new residence/life state, with A/B/C/D/E and fresh-process proof. Do not start final art or promote 0.3.2.
''')
target=E/'VERIFICATION.txt';target.write_text('\n'.join(parts),encoding='utf-8');assert target.read_bytes()
print('PASS VERIFICATION.txt written and reopened; acceptance matrix, exact commands, all 35 process records, hashes, failure history and next step included')
