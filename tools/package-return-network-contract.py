"""Prepare a reversible candidate, then seal only observed evidence. Never touches saves."""
from pathlib import Path
import difflib,hashlib,json,shutil,subprocess,sys,zipfile
R=Path(__file__).resolve().parents[1];E=R/'evidence/return-network-contract';D=R/'dist/return-network-contract';D.mkdir(exist_ok=True)
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
BASE='db567920ac9240a375a68170868cc6410dcde0a397d541a805e345e3ae88501a'
SOURCE='5b5460ee0eab81fffe8cadbe20b3a446b2af91bad385abfd32f52aca47d695c1'
b=E/'baseline/previous.jar';s=E/'baseline/previous-source.zip'
m=D/'whileaway-0.3.1-return-network-contract.jar';a=D/'whileaway-0.3.1-return-network-contract-source.zip';target=R/'build/rollback-return-network-contract/test.jar'
assert h(b)==BASE and h(s)==SOURCE
assert h(R/'dist/npc-path/whileaway-0.3.1-npc-path.jar')==BASE
assert h(R/'dist/npc-path/whileaway-0.3.1-npc-path-source.zip')==SOURCE
java=r'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe'
def probe(label,path):
    cmd=[java,'-cp','build/npc-world-archive-probe','PresenceArchiveProbe',str(path),'present']
    p=subprocess.run(cmd,cwd=R,capture_output=True,text=True)
    row=dict(label=label,command=cmd,input=str(path),exit=p.returncode,output=p.stdout+p.stderr,sha256=h(path))
    with (E/'package-commands.jsonl').open('a') as f:f.write(json.dumps(row)+'\n')
    print(json.dumps(row),flush=True);assert p.returncode==0
def source_files():
    files=[p for d in ['src','tools','docs','gradle'] for p in (R/d).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
    return files+[R/n for n in ['AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','README.md','.gitignore']]
if len(sys.argv)==1 or sys.argv[1]=='prepare':
    assert 'All 63 required tests passed' in (E/'baseline-tests.log').read_text(encoding='utf-8-sig')
    for name in ['tests.log']:
        t=(E/name).read_text(encoding='utf-8-sig');assert 'All 70 required tests passed' in t and 'PASS core assertions=176857' in t
    shutil.copyfile(R/'build/libs/whileaway-0.3.1-dev.1.jar',m)
    diff=[]
    with zipfile.ZipFile(s) as z:
        for p in sorted(source_files()):
            key='while-you-were-away/'+p.relative_to(R).as_posix()
            try:before=z.read(key).decode('utf-8').replace('\r\n','\n') if key in z.namelist() else '';after=p.read_text(encoding='utf-8')
            except UnicodeError:continue
            diff+=difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+p.relative_to(R).as_posix(),tofile='b/'+p.relative_to(R).as_posix())
    (E/'DIFF.patch').write_text(''.join(diff),encoding='utf-8')
    with zipfile.ZipFile(b) as old,zipfile.ZipFile(m) as new:
        assert new.testzip() is None
        changed=[n for n in old.namelist() if n.endswith('.class') and old.read(n)!=new.read(n)]
        assert not changed,changed # Only new, unregistered contract classes; existing runtime remains identical.
        art=[n for n in old.namelist() if n.startswith(('assets/','data/'))]
        assert all(old.read(n)==new.read(n) for n in art)
        (E/'archive-delta.json').write_text(json.dumps(dict(changedExistingClasses=changed,newClasses=[n for n in new.namelist() if n.endswith('.class') and n not in old.namelist()],unchangedArtEntries=len(art)),indent=2))
    rb='''#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: npc-path JAR restored; world files untouched'
'''.replace('BASE_HASH',BASE).replace('MOD_HASH',h(m))
    (E/'ROLLBACK.sh').write_text(rb,newline='\n');target.parent.mkdir(exist_ok=True);shutil.copyfile(m,target)
    probe('BASELINE',b);probe('MODIFIED',m)
    print('PREPARED: run ROLLBACK.sh on build/rollback-return-network-contract/test.jar, then finalize with VERIFICATION.txt present')
elif sys.argv[1]=='finalize':
    assert target.read_bytes()==b.read_bytes() and m.read_bytes()!=b.read_bytes()
    assert (E/'VERIFICATION.txt').stat().st_size>2000
    probe('RESTORED',target)
    files=sorted(set(source_files()+[p for p in E.rglob('*') if p.is_file()]+[m]))
    manifest=E/'SOURCE_MANIFEST.sha256';files=[p for p in files if p!=manifest]
    manifest.write_text(''.join(h(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in files));files.append(manifest)
    with zipfile.ZipFile(a,'w',zipfile.ZIP_DEFLATED) as z:
        for p in files:
            i=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix());i.create_system=3
            i.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16
            z.writestr(i,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
    with zipfile.ZipFile(a) as z:
        assert z.testzip() is None
        for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
        assert (z.getinfo('while-you-were-away/evidence/return-network-contract/ROLLBACK.sh').external_attr>>16)&0o111
    for p in [m,a,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh']:
        print(str(p),'SHA256='+h(p),'bytes='+str(len(p.read_bytes())))
    print('PASS all claimed artifacts reopened; source manifest/CRC verified; rollback copy restored; modified candidate retained; overall PARTIAL')
else:raise ValueError('expected prepare or finalize')
