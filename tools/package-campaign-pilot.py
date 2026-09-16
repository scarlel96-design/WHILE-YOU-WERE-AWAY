"""Package a PARTIAL development slice; never republish the adopted lifecycle baseline."""
from pathlib import Path
import difflib
import hashlib
import shutil
import sys
import zipfile

R = Path(__file__).resolve().parents[1]
E = R/'evidence/campaign'
D = R/'dist/campaign-pilot'
BASE = E/'baseline/previous.jar'
MOD = D/'whileaway-0.3.1-campaign-pilot.jar'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
baseline_hash = '07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255'
source_hash = 'cc1a1899f1b16c317b8cf4022b406fd4c4e20db51f8fafa662d39647bd3597a0'
assert sha(BASE) == baseline_hash
assert sha(E/'baseline/previous-source.zip') == source_hash
assert sha(R/'dist/lifecycle-work/whileaway-0.3.1-lifecycle-work.jar') == baseline_hash
assert sha(R/'dist/lifecycle-work/whileaway-0.3.1-lifecycle-work-source.zip') == source_hash
source = [p for folder in ('src','tools','docs','gradle') for p in (R/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
source += [R/n for n in ('README.md','AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','.gitignore')]
if '--prepare' in sys.argv:
    D.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(R/'build/libs/whileaway-0.3.1-dev.1.jar',MOD)
    differences = []
    with zipfile.ZipFile(E/'baseline/previous-source.zip') as old:
        for p in sorted(source):
            try: after = p.read_text(encoding='utf-8')
            except UnicodeError: continue
            name = p.relative_to(R).as_posix()
            key = 'while-you-were-away/'+name
            before = old.read(key).decode('utf-8').replace('\r\n','\n') if key in old.namelist() else ''
            differences.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+name,tofile='b/'+name))
        for name in ('ActorGameTests.java','StoryGameTests.java'):
            key = 'while-you-were-away/src/main/java/io/github/whileaway/'+name
            assert old.read(key) == (R/'src/main/java/io/github/whileaway'/name).read_bytes()
    (E/'DIFF.patch').write_text(''.join(differences),encoding='utf-8')
    rollback = '''#!/usr/bin/env bash
set -euo pipefail
# Restore only a caller-supplied JAR copy. No world/save operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 5
echo 'ROLLBACK PASS: lifecycle-work JAR restored; world files untouched'
'''.replace('BASE_HASH',baseline_hash).replace('MOD_HASH',sha(MOD))
    (E/'ROLLBACK.sh').write_text(rollback,encoding='utf-8',newline='\n')
    with zipfile.ZipFile(BASE) as old, zipfile.ZipFile(MOD) as new:
        assets = [n for n in old.namelist() if n.startswith(('assets/','data/')) and not n.endswith('/')]
        assert set(assets) == {n for n in new.namelist() if n.startswith(('assets/','data/')) and not n.endswith('/')}
        assert all(old.read(n) == new.read(n) for n in assets)
        classes = list((R/'build/classes/java/main').rglob('*.class'))
        assert all(new.read(p.relative_to(R/'build/classes/java/main').as_posix()) == p.read_bytes() for p in classes)
        assert new.testzip() is None
    text = f'PASS archive reopened; runtime classes match={len(classes)}; unchanged asset/data entries={len(assets)}; baseline GameTest sources unchanged=2\nBASE_SHA256={sha(BASE)}\nMOD_SHA256={sha(MOD)}\n'
    (E/'ARCHIVE_PROOF.txt').write_text(text,encoding='utf-8')
    print(text)
else:
    for arm in ('A','B'):
        log = (R/f'evidence/caveman-pilot/{arm}/gradle.log').read_text(encoding='utf-8-sig')
        assert 'All 48 required tests passed' in log and 'BUILD SUCCESSFUL' in log
    assert (E/'VERIFICATION.txt').is_file()
    assert 'ROLLBACK PASS' in (E/'rollback.log').read_text(encoding='utf-8-sig')
    for line in (E/'RUNTIME_SOURCE.sha256').read_text().splitlines():
        digest,name=line.split('  ',1)
        assert sha(R/name)==digest, name
    files = source+[BASE,E/'baseline/previous-source.zip',MOD]
    files += [p for folder in ('campaign','caveman-pilot') for p in (R/'evidence'/folder).rglob('*') if p.is_file() and p.suffix not in ('.jar','.zip')]
    manifest = E/'SOURCE_MANIFEST.sha256'
    files = sorted(set(p for p in files if p != manifest))
    manifest.write_text(''.join(sha(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in files),encoding='utf-8')
    files.append(manifest)
    archive = D/'whileaway-0.3.1-campaign-pilot-source.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for p in files:
            info=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix(),(2026,9,10,0,0,0))
            info.create_system=3
            info.external_attr=(0o100755 if p.name in ('ROLLBACK.sh','gradlew') else 0o100644)<<16
            z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for p in files: assert z.read('while-you-were-away/'+p.relative_to(R).as_posix()) == p.read_bytes()
    for p in (MOD,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh'): assert p.read_bytes()
    (D/'SHA256SUMS.txt').write_text(sha(MOD)+'  '+MOD.name+'\n'+sha(archive)+'  '+archive.name+'\n',encoding='utf-8')
    print(f'PASS development archive reopened and verified; files={len(files)}; version=0.3.1-dev.1; overall=PARTIAL')
