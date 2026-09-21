"""Preserve A-D preflight only. This package is NOT a sealed signal integration."""
from pathlib import Path
import difflib,hashlib,json,shutil,subprocess,sys,zipfile
R=Path(__file__).resolve().parents[1];E=R/'evidence/return-network-closure';D=R/'dist/return-network-closure-preflight'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
jar=D/'whileaway-0.3.1-return-network-closure-preflight.jar';source=D/'whileaway-0.3.1-return-network-closure-preflight-source.zip'
base=E/'baseline/main.jar';target=R/'build/rollback-return-network-closure/test.jar'
assert h(base)=='c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162'
assert h(E/'baseline/main-source.zip')=='c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda'
assert h(E/'baseline/support-source.zip')=='24b676bef766abddf7a9186284749ddf7613f739fc809be0a80bc59122335318'
def probe(label,p):
 cmd=[sys.executable,'tools/probe-art.py',str(p),'0.3.1-dev.1','2'];v=subprocess.run(cmd,cwd=R,text=True,capture_output=True)
 row=dict(label=label,command=subprocess.list2cmdline(cmd),input=str(p),output=v.stdout.strip(),stderr=v.stderr.strip(),exit=v.returncode,sha256=h(p))
 with (E/'artifact-commands.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(row)+'\n')
 assert v.returncode==0,row
 print(json.dumps(row))
if sys.argv[1]=='prepare':
 g=json.loads((E/'GATES.json').read_text());assert g['gameTests']==102 and g['finalSourceClientPasses']==5
 D.mkdir(exist_ok=True);assert not jar.exists();shutil.copyfile(R/'build/libs/whileaway-0.3.1-dev.1.jar',jar)
 with zipfile.ZipFile(base) as old,zipfile.ZipFile(jar) as new:
  changed=[n for n in old.namelist() if n.endswith('.class') and old.read(n)!=new.read(n)]
  assert changed==['io/github/whileaway/client/ReturnNetworkSmoke.class'],changed
  assets=[n for n in old.namelist() if n.startswith(('assets/','data/')) and not n.endswith('/')]
  assert all(old.read(n)==new.read(n) for n in assets)
  proof=dict(changedExistingClasses=changed,unchangedExistingClasses=sum(n.endswith('.class') for n in old.namelist())-len(changed),unchangedAssets=len(assets),productionSemantics='UNCHANGED',jar=h(jar))
  (E/'archive-proof.json').write_text(json.dumps(proof,indent=2))
 diff=subprocess.run(['git','-c','core.longpaths=true','diff','--binary','7aa36c50','--','src'],cwd=R,capture_output=True,check=True).stdout
 for name in ['ReturnNetworkClosureProbe.java','ReturnNetworkEnvironmentProbe.java']:
  p=R/'src/main/java/io/github/whileaway/client'/name
  diff+=''.join(difflib.unified_diff([],p.read_text().splitlines(True),fromfile='/dev/null',tofile='b/'+p.relative_to(R).as_posix())).encode()
 (E/'DIFF.patch').write_bytes(diff)
 rb='''#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd -- "$HERE/../.." && pwd)"
BASE="$HERE/baseline/main.jar"
TARGET="$(realpath -- "${1:?Pass disposable JAR copy in build/rollback-return-network-closure}")"
ALLOWED="$(realpath -- "$ROOT/build/rollback-return-network-closure")"
[[ "$TARGET" == "$ALLOWED/"*.jar && -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: previous verified MAIN JAR restored; world files untouched; no save downgrade'
'''.replace('BASE_HASH',h(base)).replace('MOD_HASH',h(jar))
 (E/'ROLLBACK.sh').write_text(rb,encoding='utf-8',newline='\n');target.parent.mkdir(exist_ok=True);shutil.copyfile(jar,target)
 probe('BASELINE',base);probe('MODIFIED',jar)
elif sys.argv[1]=='finalize':
 assert h(target)==h(base) and h(jar)!=h(base)
 probe('ROLLBACK_RESTORED',target)
 assert (E/'VERIFICATION.txt').stat().st_size>2000
 files=[]
 for root in ['src','gradle','docs','tools']:
  files += [p for p in (R/root).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix!='.pyc']
 files += [R/n for n in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','README.md','AGENTS.md','.gitignore'] if (R/n).exists()]
 files += [p for p in E.rglob('*') if p.is_file() and p.name not in ['main-source.zip','support-source.zip','SOURCE_MANIFEST.sha256','ARTIFACTS.json','UPLOAD.json']]
 files += [jar]
 manifest=E/'SOURCE_MANIFEST.sha256';manifest.write_text(''.join(h(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in sorted(set(files))),encoding='utf-8');files.append(manifest)
 assert not source.exists()
 with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(set(files)):
   i=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix());i.create_system=3;i.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16;z.writestr(i,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None
  for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
 outputs=[jar,source,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh']
 records=[dict(path=str(p),sha256=h(p),bytes=len(p.read_bytes())) for p in outputs]
 (E/'ARTIFACTS.json').write_text(json.dumps(records,indent=2));print(json.dumps(records,indent=2));print('PASS all artifacts reopened; ZIP CRC/source bytes exact; rollback restored on copy; modified JAR retained')
else:raise ValueError('prepare/finalize')
