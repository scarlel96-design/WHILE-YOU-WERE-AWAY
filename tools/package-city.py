"""Prepare reversible JAR artifacts, then seal only after live city/reload gates pass."""
from pathlib import Path
import difflib,hashlib,json,shutil,sys,zipfile
R=Path(__file__).resolve().parents[1];E=R/'evidence/city';D=R/'dist'
BASE=E/'baseline/whileaway-0.1.0-dev.1.jar';MOD=D/'whileaway-0.2.0-dev.1.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(BASE)=='3a3d083731506450772d247f2a6df4bd0126db173c9b75f169b2a23e0902e49b'
if '--prepare' in sys.argv:
    D.mkdir(exist_ok=True);shutil.copyfile(R/'build/libs'/MOD.name,MOD)
    changes=[]
    with zipfile.ZipFile(E/'baseline/source.zip') as old:
        for folder in ['src','tools','docs']:
            for p in sorted((R/folder).rglob('*')):
                if not p.is_file() or '__pycache__' in p.parts:continue
                try:after=p.read_text(encoding='utf-8')
                except UnicodeError:continue
                rel=p.relative_to(R).as_posix();key='while-you-were-away/'+rel
                before=old.read(key).decode('utf-8') if key in old.namelist() else ''
                changes.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
        for rel in ['build.gradle','gradle.properties','README.md','AGENTS.md']:
            before=old.read('while-you-were-away/'+rel).decode('utf-8');after=(R/rel).read_text(encoding='utf-8')
            changes.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
    (E/'DIFF.patch').write_text(''.join(changes),encoding='utf-8')
    script='''#!/usr/bin/env bash
set -euo pipefail
# JAR-copy rollback only. World/schema migration is not undone.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/whileaway-0.1.0-dev.1.jar"
TARGET="${1:?Pass an existing modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || { echo 'ERROR existing regular JAR required'; exit 2; }
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || { echo 'ERROR baseline changed'; exit 3; }
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || { echo 'ERROR target differs; preserving it'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 5
echo 'ROLLBACK PASS: 0.1 JAR hash restored; world files untouched'
'''.replace('BASE_HASH',sha(BASE)).replace('MOD_HASH',sha(MOD))
    (E/'ROLLBACK.sh').write_text(script,encoding='utf-8',newline='\n')
    print('PASS prepared modified JAR, source diff, hash-guarded rollback')
    print('MODIFIED_SHA256='+sha(MOD));sys.exit(0)

log=(E/'runtime-final.log').read_text(encoding='utf-8-sig')
assert 'All 15 required tests passed' in log and 'BUILD SUCCESSFUL' in log
assert 'WHILEAWAY_CITY_FAILED' not in log
assert log.count('VERIFIED MUTED before launch:')==2
for task in ['runCitySmoke','runCityReload']:
    assert ('PASS '+task) in (E/(task+'.txt')).read_text(encoding='utf-8')
rows=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'));assert all(row['exit']==0 for row in rows)
assert sha(MOD)==sha(R/'build/libs'/MOD.name)
assert sha(R/'build/rollback-city/whileaway.jar')==sha(BASE)
report=[
    'WHILEAWAY 0.2.0-dev.1 / CITY SLICE / 2026-09-09',
    'Changed fields: mod_version 0.1.0-dev.1 -> 0.2.0-dev.1; NarrativeData.SCHEMA 1 -> 2;',
    'new quiet_city dimension; relay lit flag and conditional city entry; persistent city cursor/return position/records.',
    'MODIFIED_FILE='+str(MOD),'DIFF_FILE='+str(E/'DIFF.patch'),
    'VERIFICATION='+str(E/'VERIFICATION.txt'),'ROLLBACK='+str(E/'ROLLBACK.sh'),
    'BASELINE_SHA256='+sha(BASE),'MODIFIED_SHA256='+sha(MOD),
    'Working directory='+str(R),'',
]
for row in rows:
    report += [row['phase'],'COMMAND: '+row['command'],'INPUT: '+row['input'],
               'LITERAL OUTPUT: '+row['output'],'EXIT STATUS: '+str(row['exit']),'']
report += [
    'Restored copy: version=0.1.0-dev.1; city_dimension=false; baseline SHA256 equal.',
    'Retained modified release: version=0.2.0-dev.1; city_dimension=true; modified SHA256 unchanged.',
    'ROLLBACK.sh was executed with Git Bash and test -x. It restores a JAR COPY only, not sources or save data.',
    'Do not interpret this as a schema-2 world downgrade test. Existing worlds were not downgraded.',
    '',r'COMMAND: .\gradlew.bat build runGameTestServer runCitySmoke runCityReload --console=plain',
    'INPUT: Java 21 / Minecraft 1.21.1 / NeoForge 21.1.249 / GeckoLib 4.9.2; isolated seed 74192361 world.',
    'LITERAL OUTPUT: All 15 required tests passed :)',
    'LITERAL OUTPUT: PASS runCitySmoke','LITERAL OUTPUT: PASS runCityReload',
    next(line for line in log.splitlines() if line.startswith('BUILD SUCCESSFUL')),'EXIT STATUS: 0',
    'Full runtime log='+str(E/'runtime-final.log'),'',
    (E/'offline-tests.txt').read_text(encoding='utf-8-sig'),
    (E/'runCitySmoke.txt').read_text(encoding='utf-8'),
    (E/'runCityReload.txt').read_text(encoding='utf-8'),
    'VISUAL REVIEW: city-street.png, city-overview.png, city-reloaded.png inspected. Prototype block architecture, not final art.',
    'ACTUAL CLIENT: real integrated server transfer, three book-use calls, return, reentry and second-process disk reload.',
    'NOT TESTED: two simultaneous clients, death/respawn playthrough, full natural survival quest, listening quality, modpack/shader compatibility, crash-atomic persistence.',
    'Full city campaign / additional city monsters / multiple endings: NOT IMPLEMENTED in this slice.',
    'First server test mistakenly expected a custom level in vanilla GameTestServer; test responsibilities corrected after source inspection.',
    'First reload waited on experimental-world backup confirmation and timed out; harness now confirms backup only for its named isolated test save.',
]
(E/'VERIFICATION.txt').write_text('\n'.join(report)+'\n',encoding='utf-8')
files=[]
for folder in ['src','tools','docs','gradle']:
    files += [p for p in (R/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
files += [R/n for n in ['README.md','AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','.gitignore','VERIFICATION.txt','ROLLBACK.sh']]
files += [R/'evidence'/n for n in ['baseline/ThreatPolicy.java','DIFF.patch','transaction.json']]
files += [E/n for n in ['DIFF.patch','ROLLBACK.sh','VERIFICATION.txt','transaction.json','offline-tests.txt','runtime-final.log','runCitySmoke.txt','runCityReload.txt','smoke-world.txt','baseline/whileaway-0.1.0-dev.1.jar','baseline/source.zip']]
files += list((E/'screenshots').glob('*.png'))
files += [R/'evidence/audio-assets.json']
manifest=E/'SOURCE_MANIFEST.sha256'
manifest.write_text(''.join(f'{sha(p)}  {p.relative_to(R).as_posix()}\n' for p in sorted(files)),encoding='utf-8');files.append(manifest)
archive=D/'whileaway-0.2.0-dev.1-source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):
        info=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix(),(2026,9,9,0,0,0));info.create_system=3
        info.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16
        z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
with zipfile.ZipFile(MOD) as z:assert z.testzip() is None
(D/'SHA256SUMS-0.2.txt').write_text(f'{sha(MOD)}  {MOD.name}\n{sha(archive)}  {archive.name}\n',encoding='utf-8')
for p in [E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh',manifest,R/'README.md',R/'docs/CITY_SLICE.md']:assert p.read_text(encoding='utf-8').strip()
print(f'PASS city release package files={len(files)}; source ZIP byte equality; JAR reopened; rollback copy hash verified')
print('MODIFIED_SHA256='+sha(MOD));print('SOURCE_SHA256='+sha(archive))
