"""Reversible 0.3.1 archive and bounded recovery evidence, not full-campaign certification."""
from pathlib import Path
import difflib,hashlib,json,shutil,sys,zipfile
R=Path(__file__).resolve().parents[1];E=R/'evidence/stability';D=R/'dist'
BASE=E/'baseline/previous.jar';MOD=D/'whileaway-0.3.1-dev.1.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(BASE)=='86b70224347ee8b527e0bd21041dba6009f263caa45d6395aaff3a3380283e49'
source=[p for folder in ['src','tools','docs','gradle'] for p in (R/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
source += [R/n for n in ['README.md','AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','.gitignore']]
if '--prepare' in sys.argv:
    shutil.copyfile(R/'build/libs'/MOD.name,MOD)
    changes=[]
    with zipfile.ZipFile(E/'baseline/previous-source.zip') as old:
        for p in sorted(source):
            try:after=p.read_text(encoding='utf-8')
            except UnicodeError:continue
            rel=p.relative_to(R).as_posix();key='while-you-were-away/'+rel
            before=old.read(key).decode('utf-8') if key in old.namelist() else ''
            changes.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
    (E/'DIFF.patch').write_text(''.join(changes),encoding='utf-8')
    script='''#!/usr/bin/env bash
set -euo pipefail
# JAR copy only. Never downgrade world saves.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass an existing modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 5
echo 'ROLLBACK PASS: 0.3 JAR hash restored; world files untouched'
'''.replace('BASE_HASH',sha(BASE)).replace('MOD_HASH',sha(MOD))
    (E/'ROLLBACK.sh').write_text(script,encoding='utf-8',newline='\n')
    print('PASS prepared 0.3.1 JAR, diff and hash-guarded rollback');sys.exit(0)

log=(E/'regression-final.log').read_text(encoding='utf-8-sig')
assert 'All 25 required tests passed' in log and 'BUILD SUCCESSFUL' in log
assert 'WHILEAWAY_RECOVERY_FAILED' not in log
assert sha(MOD)==sha(R/'build/libs'/MOD.name)
assert sha(R/'build/rollback-stability/whileaway.jar')==sha(BASE)
with zipfile.ZipFile(BASE) as a,zipfile.ZipFile(MOD) as b:
    assets={n for n in a.namelist() if n.startswith(('assets/','data/'))}
    assert assets=={n for n in b.namelist() if n.startswith(('assets/','data/'))}
    for name in assets:assert a.read(name)==b.read(name),name
    assert b.testzip() is None
tasks=['runRecoveryCut1','runRecoveryCopy1','runRecoveryResume1','runRecoveryCut2','runRecoveryCopy2','runRecoveryResume2','runRecoveryLegacy']
for task in tasks:assert ('PASS '+task) in (E/(task+'.txt')).read_text(encoding='utf-8')
for task in ['runCitySmoke','runCityReload']:assert ('PASS '+task) in (E/'regression'/(task+'.txt')).read_text(encoding='utf-8')
for n in [1,2]:
    run=(E/f'runRecoveryResume{n}.txt').read_text(encoding='utf-8')
    for label in ['death_respawn cycle=1','death_respawn cycle=2','chunk_unload_reload cycle=1','chunk_unload_reload cycle=2','nether_return cycle=1','end_return cycle=2']:
        assert 'PASS interruption='+label in run
    proof=(E/f'copy-isolation-{n}.txt').read_text(encoding='utf-8-sig')
    assert ('PASS original story bytes unchanged' in proof if n==1 else 'PASS independent original/copy cityClues: 1 / 7' in proof)
rows=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'));assert all(r['exit']==0 for r in rows)
report=['WHILEAWAY 0.3.1-dev.1 / CHECKPOINT RECOVERY SUBSTAGE / WHOLE STABILITY PHASE NOT SEALED',
    'Changed fields: mod_version 0.3.0-dev.1 -> 0.3.1-dev.1; NarrativeData.SCHEMA 3 -> 4 (reads 1/2/3);',
    'SceneRecord checkpoint/state/instanceId/shown/interruption reason; ScenePayload lease/checkpoint; SceneAck;',
    'cityShockTriggered now committed at COMPLETED, not start. Legacy true is preserved as completed.',
    'Wayfarer offline/death/dimension active-time suspension and noncanonical duplicate cleanup.',
    'MODIFIED_FILE='+str(MOD),'DIFF_FILE='+str(E/'DIFF.patch'),'VERIFICATION='+str(E/'VERIFICATION.txt'),'ROLLBACK='+str(E/'ROLLBACK.sh'),
    'BASELINE_SHA256='+sha(BASE),'MODIFIED_SHA256='+sha(MOD),'Working directory='+str(R),'']
for row in rows:
    report += [row['phase'],'COMMAND: '+row['command'],'INPUT: '+row['input'],'LITERAL OUTPUT: '+row['output'],'EXIT STATUS: '+str(row['exit']),'']
report += ['Restored copy: version=0.3.0-dev.1; checkpoint_journal=false; original hash restored.',
    'Retained modified release: version=0.3.1-dev.1; checkpoint_journal=true; modified hash unchanged.',
    'Executable rollback was run against another JAR copy only. World schema downgrade NOT TESTED.',
    f'PASS unchanged art/data resource bytes: {len(assets)} archive entries.',
    (E/'runtime-commands.txt').read_text(encoding='utf-8'),
    (E/'offline-tests.txt').read_text(encoding='utf-8-sig'),
    (E/'GATES.md').read_text(encoding='utf-8'),
    (E/'FIXTURE_PROOF.txt').read_text(encoding='utf-8'),
    'FAILURE HISTORY: first Cut1 produced expected non-graceful native process exit -1073740791 after fault marker; initial Gradle wrapper marked it failed.',
    'Cut2 first attempt caught a harness race: renderer checkpoint arrived before queued server acknowledgement. Test now waits for server commit, then halts. The failed run is retained as cut2-first-attempt.log.',
    'Visual checkpoint screenshots inspected; no new visual assets. Actual audio remained muted. Listening quality NOT TESTED.',
]
for task in tasks:report += ['',(E/(task+'.txt')).read_text(encoding='utf-8')]
(E/'VERIFICATION.txt').write_text('\n'.join(report)+'\n',encoding='utf-8')
files=source+[p for p in E.rglob('*') if p.is_file() and p.name!='SOURCE_MANIFEST.sha256']
manifest=E/'SOURCE_MANIFEST.sha256';manifest.write_text(''.join(f'{sha(p)}  {p.relative_to(R).as_posix()}\n' for p in sorted(files)),encoding='utf-8');files.append(manifest)
archive=D/'whileaway-0.3.1-dev.1-source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):
        info=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix(),(2026,9,9,0,0,0));info.create_system=3
        info.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16
        z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
for p in [E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh',E/'GATES.md',manifest]:assert p.read_text(encoding='utf-8').strip()
(D/'SHA256SUMS-0.3.1.txt').write_text(f'{sha(MOD)}  {MOD.name}\n{sha(archive)}  {archive.name}\n',encoding='utf-8')
print(f'PASS stability package files={len(files)}; ZIP bytes verified; rollback copy restored; art resources unchanged')
print('MODIFIED_SHA256='+sha(MOD));print('SOURCE_SHA256='+sha(archive))
