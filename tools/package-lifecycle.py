from pathlib import Path
import hashlib,shutil,difflib,zipfile,sys,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/lifecycle';D=R/'dist/lifecycle-work'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
BASE=E/'baseline/previous.jar';MOD=D/'whileaway-0.3.1-lifecycle-work.jar'
assert sha(BASE)=='9062d03914ff6dc8211e77342d785734c6263bd3af5bfb2fa5ce344a50c580d6'
assert sha(E/'baseline/previous-source.zip')=='9f0f464e680ce2cfdde8751777411c1bc264645ed6f70e6d195b063b7ae4e427'
assert sha(R/'dist/stability-work/whileaway-0.3.1-stability-work.jar')==sha(BASE)
assert sha(R/'dist/stability-work/whileaway-0.3.1-stability-work-source.zip')==sha(E/'baseline/previous-source.zip')
source=[p for folder in ['src','tools','docs','gradle'] for p in (R/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
source += [R/n for n in ['README.md','AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','.gitignore']]
if '--prepare' in sys.argv:
    D.mkdir(exist_ok=True);shutil.copyfile(R/'build/libs/whileaway-0.3.1-dev.1.jar',MOD)
    changes=[]
    with zipfile.ZipFile(E/'baseline/previous-source.zip') as old:
        for p in sorted(source):
            try:after=p.read_text(encoding='utf-8')
            except UnicodeError:continue
            rel=p.relative_to(R).as_posix();key='while-you-were-away/'+rel
            before=old.read(key).decode('utf-8').replace('\r\n','\n') if key in old.namelist() else ''
            changes.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
    (E/'DIFF.patch').write_text(''.join(changes),encoding='utf-8')
    script='''#!/usr/bin/env bash
set -euo pipefail
# Restores only a caller-supplied JAR copy. No world file operations.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass the modified JAR copy to restore}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || { echo 'ERROR preserving unexpected target'; exit 4; }
cp -- "$BASE" "$TARGET"
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 5
echo 'ROLLBACK PASS: stability-work JAR restored; world files untouched'
'''.replace('BASE_HASH',sha(BASE)).replace('MOD_HASH',sha(MOD))
    (E/'ROLLBACK.sh').write_text(script,encoding='utf-8',newline='\n')
    with zipfile.ZipFile(BASE) as old,zipfile.ZipFile(MOD) as new:
        names=[n for n in old.namelist() if n.startswith(('assets/','data/')) and not n.endswith('/')]
        assert set(names)=={n for n in new.namelist() if n.startswith(('assets/','data/')) and not n.endswith('/')}
        assert all(old.read(n)==new.read(n) for n in names)
        classes=list((R/'build/classes/java/main').rglob('*.class'))
        assert all(new.read(p.relative_to(R/'build/classes/java/main').as_posix())==p.read_bytes() for p in classes)
        assert new.testzip() is None
    (E/'ARCHIVE_PROOF.txt').write_text(f'PASS archive reopens; compiled classes match={len(classes)}; unchanged asset/data entries={len(names)}\nBASE_SHA256={sha(BASE)}\nMOD_SHA256={sha(MOD)}\n',encoding='utf-8')
    print((E/'ARCHIVE_PROOF.txt').read_text(encoding='utf-8'));sys.exit(0)
# Seal only after independently verified commands, runtime snapshots and rollback proof exist.
assert 'All 48 required tests passed' in (E/'modified-tests.log').read_text(encoding='utf-8')
assert 'PASS actor boundary snapshots=10; final client verifications=2' in (E/'FIXTURE_PROOF.txt').read_text(encoding='utf-8')
assert json.loads((E/'transaction.json').read_text(encoding='utf-8'))['restored_hash']==sha(BASE)
assert sha(MOD)!=sha(BASE)
for line in (E/'RUNTIME_SOURCE.sha256').read_text(encoding='utf-8').splitlines():
    digest,name=line.split('  ',1);assert sha(R/name)==digest,'runtime source drift: '+name
files=source+[BASE,E/'baseline/previous-source.zip',MOD,R/'evidence/reuse/baseline/legacy-v2.dat',R/'evidence/reuse/baseline/legacy-v3.dat']+[p for p in E.rglob('*') if p.is_file() and p.suffix not in ('.jar','.zip')]
files=list(dict.fromkeys(files));manifest=E/'SOURCE_MANIFEST.sha256'
files=[p for p in files if p!=manifest]
manifest.write_text(''.join(sha(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in sorted(files)),encoding='utf-8')
files.append(manifest)
archive=D/'whileaway-0.3.1-lifecycle-work-source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):
        info=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix(),(2026,9,10,0,0,0));info.create_system=3
        info.external_attr=(0o100755 if p.name in ('ROLLBACK.sh','gradlew') else 0o100644)<<16
        z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
for p in [MOD,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh']:assert p.read_bytes()
(D/'SHA256SUMS.txt').write_text(sha(MOD)+'  '+MOD.name+'\n'+sha(archive)+'  '+archive.name+'\n',encoding='utf-8')
print(f'PASS sealed working archives; source files={len(files)}; version remains 0.3.1-dev.1; overall=PARTIAL')
