"""Package the art revision only after live muted client gates and copy rollback pass."""
from pathlib import Path
import difflib, hashlib, json, shutil, sys, zipfile
R=Path(__file__).resolve().parents[1]; E=R/'evidence/art'; D=R/'dist'
BASE=E/'baseline/previous.jar'; MOD=D/'whileaway-0.3.0-dev.1.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(BASE)=='70ea359aa99044c542379fbc95a1d204d884446e9891db281e9bfa22a881848e'
if '--prepare' in sys.argv:
    shutil.copyfile(R/'build/libs'/MOD.name,MOD)
    changes=[]
    files=[p for folder in ['src','tools','docs'] for p in (R/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
    files += [R/n for n in ['build.gradle','gradle.properties','README.md','AGENTS.md']]
    with zipfile.ZipFile(E/'baseline/previous-source.zip') as old:
        for p in sorted(files):
            try:after=p.read_text(encoding='utf-8')
            except UnicodeError:continue
            rel=p.relative_to(R).as_posix(); key='while-you-were-away/'+rel
            before=old.read(key).decode('utf-8') if key in old.namelist() else ''
            changes.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
    (E/'DIFF.patch').write_text(''.join(changes),encoding='utf-8')
    script='''#!/usr/bin/env bash
set -euo pipefail
# Restores an explicitly supplied JAR copy; never edits worlds or source files.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass an existing modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || { echo 'ERROR existing regular JAR required'; exit 2; }
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || { echo 'ERROR baseline changed'; exit 3; }
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || { echo 'ERROR target differs; preserving it'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 5
echo 'ROLLBACK PASS: 0.2 JAR hash restored; world files untouched'
'''.replace('BASE_HASH',sha(BASE)).replace('MOD_HASH',sha(MOD))
    (E/'ROLLBACK.sh').write_text(script,encoding='utf-8',newline='\n')
    print('PASS prepared modified JAR, source diff, hash-guarded rollback');sys.exit(0)

log=(E/'runtime.log').read_text(encoding='utf-8-sig')
assert 'All 17 required tests passed' in log and 'BUILD SUCCESSFUL' in log
assert 'WHILEAWAY_CITY_FAILED' not in log and log.count('VERIFIED MUTED before launch:')==2
for task in ['runCitySmoke','runCityReload']:
    assert ('PASS '+task) in (E/(task+'.txt')).read_text(encoding='utf-8')
assert 'PASS seven apparition render, descent phase and cleanup' in log
rows=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'));assert all(row['exit']==0 for row in rows)
assert sha(MOD)==sha(R/'build/libs'/MOD.name)
assert sha(R/'build/rollback-art/whileaway.jar')==sha(BASE)
report=[
    'WHILEAWAY 0.3.0-dev.1 / CITY ART AND SEVEN DEPARTURES / 2026-09-09',
    'Changed fields: mod_version 0.2.0-dev.1 -> 0.3.0-dev.1; new CityArtLayout.VERSION=2;',
    'NarrativeData.SCHEMA 2 -> 3; persistent cityShockTriggered; ScenePayload.kind=3.',
    'New districts use art revision 2. Existing revision-1 district cell order/cursor remain unchanged.',
    'MODIFIED_FILE='+str(MOD),'DIFF_FILE='+str(E/'DIFF.patch'),
    'VERIFICATION='+str(E/'VERIFICATION.txt'),'ROLLBACK='+str(E/'ROLLBACK.sh'),
    'BASELINE_SHA256='+sha(BASE),'MODIFIED_SHA256='+sha(MOD),'Working directory='+str(R),'',
]
for row in rows:
    report += [row['phase'],'COMMAND: '+row['command'],'INPUT: '+row['input'],
               'LITERAL OUTPUT: '+row['output'],'EXIT STATUS: '+str(row['exit']),'']
report += [
    'Restored copy: version=0.2.0-dev.1; art_revision=1; original hash equal.',
    'Retained modified release: version=0.3.0-dev.1; art_revision=2; modified hash unchanged.',
    'ROLLBACK.sh executed with Git Bash and test -x. JAR-copy restoration only; no world downgrade tested.',
    '',r'COMMAND: .\gradlew.bat build runGameTestServer runCitySmoke runCityReload --console=plain',
    'INPUT: Java 21 / Minecraft 1.21.1 / NeoForge 21.1.249 / GeckoLib 4.9.2; isolated seed 74192361 world.',
    'LITERAL OUTPUT: All 17 required tests passed :)',
    'LITERAL OUTPUT: PASS runCitySmoke','LITERAL OUTPUT: PASS runCityReload',
    next(line for line in log.splitlines() if line.startswith('BUILD SUCCESSFUL')),'EXIT STATUS: 0',
    'Full runtime log='+str(E/'runtime.log'),'',
    (E/'offline-tests.txt').read_text(encoding='utf-8-sig'),
    (E/'runCitySmoke.txt').read_text(encoding='utf-8'),
    (E/'runCityReload.txt').read_text(encoding='utf-8'),
    (E/'VISUAL_REVIEW.md').read_text(encoding='utf-8'),
    'ACTUAL CLIENT: district generation, transfer, books, return, reentry, one-shot scene, cleanup and next-process persistence.',
    'Scene actors reuse the existing folded Wayfarer geometry with an installed vanilla skeleton texture reference; not seven new monster species.',
    'NOT TESTED: two simultaneous clients, full natural survival quest, player death/respawn, listening quality, shader/modpack compatibility.',
    'PARTIAL: interrupted scene checkpoint/resume is not implemented; one-shot is committed on start. See docs/FUNCTION_FIRST.md F01.',
    'NEXT PRIORITY: function completion and recovery before final art. User requirements 1-32 and ordered stages 1-18 recorded in docs/FUNCTION_FIRST.md.',
    'Upper building floors are still shells. Full city campaign, additional monster species and multiple endings are not completed in this slice.',
]
(E/'VERIFICATION.txt').write_text('\n'.join(report)+'\n',encoding='utf-8')
files=[p for folder in ['src','tools','docs','gradle'] for p in (R/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
files += [R/n for n in ['README.md','AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','.gitignore']]
files += [E/n for n in ['DIFF.patch','ROLLBACK.sh','VERIFICATION.txt','transaction.json','offline-tests.txt','runtime.log','runCitySmoke.txt','runCityReload.txt','smoke-world.txt','VISUAL_REVIEW.md','baseline/previous.jar','baseline/previous-source.zip']]
files += list((E/'screenshots').glob('*.png'))
files += [E/'baseline/before-street.png',E/'baseline/before-overview.png',R/'evidence/audio-assets.json']
manifest=E/'SOURCE_MANIFEST.sha256'
manifest.write_text(''.join(f'{sha(p)}  {p.relative_to(R).as_posix()}\n' for p in sorted(files)),encoding='utf-8');files.append(manifest)
archive=D/'whileaway-0.3.0-dev.1-source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):
        info=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix(),(2026,9,9,0,0,0));info.create_system=3
        info.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16
        z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
with zipfile.ZipFile(MOD) as z:assert z.testzip() is None
(D/'SHA256SUMS-0.3.txt').write_text(f'{sha(MOD)}  {MOD.name}\n{sha(archive)}  {archive.name}\n',encoding='utf-8')
for p in [E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh',manifest,R/'README.md',R/'docs/ART_PASS.md']:assert p.read_text(encoding='utf-8').strip()
print(f'PASS art release package files={len(files)}; source ZIP byte equality; JAR reopened; rollback copy hash verified')
print('MODIFIED_SHA256='+sha(MOD));print('SOURCE_SHA256='+sha(archive))
