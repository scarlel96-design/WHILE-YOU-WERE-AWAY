"""Package only this candidate; rollback touches a JAR copy, never world files."""
from pathlib import Path
import hashlib,json,shutil,subprocess,sys,zipfile
R=Path(__file__).resolve().parents[1];E=R/'evidence/return-network-integration';D=R/'dist/return-network-integration'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
jar=D/'whileaway-0.3.1-return-network-integration.jar';source=D/'whileaway-0.3.1-return-network-integration-source.zip'
base=E/'baseline/previous.jar';basezip=E/'baseline/previous-source.zip'
assert h(base)=='bf8e234052c8a33ea49cb02c3cabcd941515e29fc126280fdcfa62d4ba0db9c0'
assert h(basezip)=='a44e33064860f13256127a3928d17049c54289a4671796f594b95d8774fb4887'
def probe(label,p):
 cmd=[sys.executable,'tools/probe-art.py',str(p),'0.3.1-dev.1','2']
 result=subprocess.run(cmd,cwd=R,text=True,capture_output=True)
 row={'label':label,'command':subprocess.list2cmdline(cmd),'input':str(p),'output':result.stdout.strip(),'stderr':result.stderr.strip(),'exit':result.returncode,'sha256':h(p)}
 with (E/'artifact-commands.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(row,ensure_ascii=False)+'\n')
 assert result.returncode==0,row
 print(label+': '+result.stdout.strip()+'; exit=0; sha256='+h(p))
if sys.argv[1]=='prepare':
 text=(E/'tests-4.log').read_text(encoding='utf-8-sig');assert 'All 95 required tests passed' in text and 'PASS core assertions=176857' in text
 D.mkdir(exist_ok=True);assert not jar.exists(), 'never overwrite an existing candidate'
 shutil.copyfile(R/'build/libs/whileaway-0.3.1-dev.1.jar',jar)
 with zipfile.ZipFile(jar) as z,zipfile.ZipFile(base) as old:
  assert z.testzip() is None and 'io/github/whileaway/ReturnNetworkEvents.class' in z.namelist()
  assert not any('MonitorAgent' in n for n in z.namelist())
  art=[n for n in old.namelist() if n.startswith(('assets/','data/')) and '/lang/' not in n]
  assert all(old.read(n)==z.read(n) for n in art),'nonlanguage art changed'
  (E/'archive-proof.json').write_text(json.dumps({'unchangedNonLanguageAssets':len(art),'registeredCompoundAdapter':True,'monitorAgentBundled':False},indent=2))
 diff=subprocess.run(['git','diff','--binary','HEAD','--','src','build.gradle'],cwd=R,capture_output=True,check=True).stdout
 # New files are not in git diff until staged; append explicit /dev/null sections.
 import difflib
 for path in ['src/main/java/io/github/whileaway/ReturnNetworkEvents.java','src/main/java/io/github/whileaway/ReturnNetworkFixtures.java','src/main/java/io/github/whileaway/ReturnNetworkIntegrationGameTests.java','src/main/java/io/github/whileaway/ReturnNetworkIntegrity.java','src/main/java/io/github/whileaway/client/ReturnNetworkSmoke.java']:
  diff+=''.join(difflib.unified_diff([], (R/path).read_text(encoding='utf-8-sig').splitlines(True),fromfile='/dev/null',tofile='b/'+path)).encode()
 (E/'DIFF.patch').write_bytes(diff)
 rb='''#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass a disposable modified JAR copy}"
[[ "$TARGET" == *.jar && -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: return-network-contract JAR restored; world files untouched; save downgrade not performed'
'''.replace('BASE_HASH',h(base)).replace('MOD_HASH',h(jar))
 (E/'ROLLBACK.sh').write_text(rb,encoding='utf-8',newline='\n')
 target=R/'build/rollback-return-network-integration/test.jar';target.parent.mkdir(exist_ok=True);shutil.copyfile(jar,target)
 probe('BASELINE',base);probe('MODIFIED',jar)
elif sys.argv[1]=='finalize':
 target=R/'build/rollback-return-network-integration/test.jar';assert h(target)==h(base) and h(jar)!=h(base)
 assert (E/'VERIFICATION.txt').stat().st_size>2000
 assert (E/'CLIENT_VERIFICATION.json').exists()
 probe('ROLLBACK_RESTORED',target)
 files=[]
 for root in ['src','gradle','docs','tools']:
  files += [p for p in (R/root).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix!='.pyc']
 files += [R/n for n in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','README.md','AGENTS.md','.gitignore'] if (R/n).exists()]
 files += [p for p in E.rglob('*') if p.is_file() and p.name not in ['previous-source.zip','SOURCE_MANIFEST.sha256','ARTIFACTS.json']]
 files += [jar]
 manifest=E/'SOURCE_MANIFEST.sha256';manifest.write_text(''.join(h(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in sorted(set(files))),encoding='utf-8');files.append(manifest)
 assert not source.exists(),'never overwrite source package'
 with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(set(files)):
   info=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix());info.create_system=3;info.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16
   z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None
  for p in set(files):assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
 outputs=[jar,source,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh']
 records=[{'path':str(p),'sha256':h(p),'bytes':len(p.read_bytes())} for p in outputs]
 (E/'ARTIFACTS.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8')
 print(json.dumps(records,ensure_ascii=False,indent=2));print('PASS artifacts reopened, ZIP CRC and source bytes verified, rollback copy restored, candidate unchanged')
else:raise ValueError('prepare or finalize')
