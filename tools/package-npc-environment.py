from pathlib import Path
import hashlib,zipfile,difflib,shutil,subprocess,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-environment';D=R/'dist/npc-environment';D.mkdir(exist_ok=True)
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
base=E/'baseline/previous.jar';src=E/'baseline/previous-source.zip';mod=D/'whileaway-0.3.1-npc-environment.jar'
assert h(base)=='9096e4dc9276c8d1eae7aaf2446950e4f76edc5f0f58ee906d7ae70d7268530f'
assert h(src)=='519e7f5b12098357628b311ffa8b9ad5cd1974a2e5474751255c978318fcdc87'
assert 'All 58 required tests passed' in (E/'baseline-tests.log').read_text(encoding='utf-8-sig')
log=(E/'tests.log').read_text(encoding='utf-8-sig');assert 'All 61 required tests passed' in log and 'PASS core assertions=176857' in log
shutil.copyfile(R/'build/libs/whileaway-0.3.1-dev.1.jar',mod)
files=[p for f in ['src','tools','docs','gradle'] for p in (R/f).rglob('*') if p.is_file() and '__pycache__' not in p.parts]+[R/n for n in ['AGENTS.md','build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','README.md','.gitignore']]
diff=[]
with zipfile.ZipFile(src) as z:
 for p in sorted(files):
  try:after=p.read_text(encoding='utf-8');key='while-you-were-away/'+p.relative_to(R).as_posix();before=z.read(key).decode('utf-8').replace('\r\n','\n') if key in z.namelist() else ''
  except UnicodeError:continue
  diff.extend(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+p.relative_to(R).as_posix(),tofile='b/'+p.relative_to(R).as_posix()))
(E/'DIFF.patch').write_text(''.join(diff),encoding='utf-8')
with zipfile.ZipFile(base) as a,zipfile.ZipFile(mod) as b:
 changed=[n for n in a.namelist() if n.endswith('.class') and a.read(n)!=b.read(n)];assert changed==['io/github/whileaway/NpcEvents.class'],changed
 assert all(a.read(n)==b.read(n) for n in a.namelist() if n.startswith(('assets/','data/')))
 assert b.testzip() is None
rb='''#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="$HERE/baseline/previous.jar"
TARGET="${1:?Pass modified JAR copy}"
[[ -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "BASE_HASH" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "MOD_HASH" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: npc-world JAR restored; world files untouched'
'''.replace('BASE_HASH',h(base)).replace('MOD_HASH',h(mod));(E/'ROLLBACK.sh').write_text(rb,newline='\n')
commands=[]
def run(label,cmd):
 p=subprocess.run(cmd,cwd=R,capture_output=True,text=True);commands.append(dict(label=label,command=cmd,exit=p.returncode,output=p.stdout+p.stderr));assert p.returncode==0,commands[-1];print(label,p.stdout.strip(),flush=True)
java=r'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe'
for label,p in [('BASELINE',base),('MODIFIED',mod)]:run(label,[java,'-cp','build/npc-world-archive-probe','PresenceArchiveProbe',str(p),'present'])
target=R/'build/rollback-npc-environment/test.jar';target.parent.mkdir(exist_ok=True);shutil.copyfile(mod,target)
run('ROLLBACK',[r'C:\Program Files\Git\bin\bash.exe','-lc','chmod +x evidence/npc-environment/ROLLBACK.sh && test -x evidence/npc-environment/ROLLBACK.sh && evidence/npc-environment/ROLLBACK.sh build/rollback-npc-environment/test.jar'])
assert target.read_bytes()==base.read_bytes() and mod.read_bytes()!=base.read_bytes()
run('RESTORED',[java,'-cp','build/npc-world-archive-probe','PresenceArchiveProbe',str(target),'present'])
(E/'transaction.json').write_text(json.dumps(commands,indent=2))
report=f'''Current: 0.3.1-dev.1 / npc-environment / PARTIAL / safe pause at observed 5h93%, weekly61%.
Input JAR {base} SHA256 {h(base)}
Input source {src} SHA256 {h(src)}
Save4 / optional actorSchema1 / art2 unchanged. Original npc-world distribution preserved.
Changes: NpcEnvironment.inspect read-only local standing geometry and return-light dependency classification. Residence VALID/TEMPORARILY_BLOCKED/MISSING/CONFLICT/CHUNK_UNAVAILABLE. Path NOT_ASSESSED/CHUNK_UNAVAILABLE/DESTINATION_BLOCKED/DESTINATION_INVALID/DESTINATION_MISSING. Light VALID/MISSING/WRONG_BLOCK/CHUNK_UNAVAILABLE.
VALID means local standing geometry, NOT path reachability. UNREACHABLE and bounded retry/navigation policy remain unimplemented. isReturnLight centralizes logical dependency binding.
NpcEvents.update checks dependency before reactivating suspended event; stops navigation and uses existing checkpoint suspension before completion. Completed resident facts/checkpoint are never reverted. No new permanent facts, no migration change, no block writes or NPC teleport. Only existing class changed: NpcEvents.
NpcEnvironmentGameTests adds3 tests: geometry missing/blocked/flooded/restored; light missing/wrong/restored; actual ungenerated chunk remains ungenerated and is not labeled missing. These are GameTests, not real-client lifecycle/recovery proof.
BASELINE ./gradlew.bat --offline runGameTestServer -> All 58 required tests passed :); BUILD SUCCESSFUL in 1m; exit0.
MODIFIED ./gradlew.bat --offline compileJava runGameTestServer build -> All 61 required tests passed :); PASS core assertions=176857; BUILD SUCCESSFUL in 58s; exit0.
No new actual clients (0), no environment restart/corruption/copy/death final regression, no fresh LoadGuard/NPC storage probe this slice. Previous npc-world results are historical, not promoted to new runtime PASS. No failures in current build/GameTests.
All environment integration/recovery gates PARTIAL or NOT TESTED. NPC/campaign PARTIAL; complex NOT STARTED;0.3.2/art/Caveman HOLD. Primary agent only; no Terra/Luna; no token saving experiment. No shutdown.
NEXT: preserve this candidate; implement bounded path retries with CHUNK_UNAVAILABLE vs TEMPORARY_FAILURE vs UNREACHABLE; then real blocked/light-missing/wrong/fixed client + fresh-process cases; actual NPC corruption; latest full lifecycle regression. Do not use old npc-world packager/verifier against changed production source.
MODIFIED_FILE={mod} SHA256={h(mod)}
DIFF_FILE={E/'DIFF.patch'}
VERIFICATION={E/'VERIFICATION.txt'}
ROLLBACK={E/'ROLLBACK.sh'}
Rollback target {target}; restored hash {h(target)}. Restored whole JAR byte-equals npc-world, so environment guard removed; packaged policy checks pass. Modified JAR remains changed. No save downgrade or gameplay rollback claim.
Exact package commands/results/exit (input each explicit JAR, present policy8 cases):
{json.dumps(commands,indent=2)}
'''
(E/'VERIFICATION.txt').write_text(report,encoding='utf-8');(E/'PAUSE_REQUESTED').write_text('Usage-aware safe pause after buildable environment dependency slice. Next bounded path policy then actual client tests. No shutdown.\n')
files+= [p for p in E.rglob('*') if p.is_file()]+[mod];files=sorted(set(files));manifest=E/'SOURCE_MANIFEST.sha256';files=[p for p in files if p!=manifest];manifest.write_text(''.join(h(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in files));files.append(manifest)
archive=D/'whileaway-0.3.1-npc-environment-source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in files:
  i=zipfile.ZipInfo('while-you-were-away/'+p.relative_to(R).as_posix());i.create_system=3;i.external_attr=(0o100755 if p.name in ['ROLLBACK.sh','gradlew'] else 0o100644)<<16;z.writestr(i,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
with zipfile.ZipFile(archive) as z:
 assert z.testzip() is None
 for p in files:assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
for p in [mod,archive,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh']:print(str(p),'SHA256='+h(p),'bytes='+str(len(p.read_bytes())))
print('PASS all artifacts reopened; source manifest/CRC verified; rollback on copy; original protected; overall PARTIAL')
