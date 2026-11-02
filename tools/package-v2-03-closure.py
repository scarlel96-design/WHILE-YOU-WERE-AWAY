"""Package the verified V2-03 source and JAR. Never modify world files."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'dist/v2-03-closure'
BASE='82beffac6347c60258e4b9db0098eecad423b176'
BASE_JAR='7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6'
BASE_SOURCE='c7d47351c297a1647eed6216a6172bd78839152d2cc28195eb29099d7bcdaa9d'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def main():
    OUT.mkdir(exist_ok=True,parents=True)
    assert sha(OUT/'BASELINE.jar')==BASE_JAR
    assert sha(OUT/'BASELINE-source.zip')==BASE_SOURCE
    head=git('rev-parse','HEAD').decode().strip()
    modified=OUT/'whileaway-0.3.1-v2-03-content.jar'
    shutil.copy2(ROOT/'build/libs/whileaway-0.3.1-dev.1.jar',modified)
    names=git('ls-files','-z').decode().split('\0')
    fixtures=['evidence/reuse/baseline/legacy-v'+str(i)+'.dat' for i in (2,3)]+['evidence/audio-assets.json']
    names=[n for n in names if n and (n.startswith(('src/','docs/','tools/','gradle/')) or n in fixtures or n in ['AGENTS.md','README.md','LICENSE','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'])]
    assert len(names)>200,'Source inventory unexpectedly small'
    assert all(n in names for n in fixtures)
    source_manifest={'implementation_commit':head,'files':{n:sha(ROOT/n) for n in names}}
    (OUT/'SOURCE_MANIFEST.json').write_text(json.dumps(source_manifest,indent=2)+'\n',encoding='utf-8')
    with zipfile.ZipFile(OUT/'whileaway-0.3.1-v2-03-content-source.zip','w',zipfile.ZIP_DEFLATED) as z:
        for n in names:
            info=zipfile.ZipInfo(n,(2026,11,3,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=(0o100755 if n=='gradlew' else 0o100644)<<16
            z.writestr(info,(ROOT/n).read_bytes())
        z.write(OUT/'SOURCE_MANIFEST.json','SOURCE_MANIFEST.json')
    (OUT/'DIFF.patch').write_bytes(git('diff','--binary',BASE,head,'--','src','docs','tools','evidence/audio-assets.json'))
    ps=r"""param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
function Hash([string]$Path) {
    $algorithm=[Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($algorithm.ComputeHash([IO.File]::ReadAllBytes($Path)))).Replace('-','').ToLowerInvariant() }
    finally { $algorithm.Dispose() }
}
$source=Join-Path $PSScriptRoot 'BASELINE.jar'
$modified=Join-Path $PSScriptRoot 'whileaway-0.3.1-v2-03-content.jar'
if((Hash $source) -ne '@BASE@'){throw 'Baseline hash mismatch'}
$resolved=(Resolve-Path -LiteralPath $Target).ProviderPath
if([IO.Path]::GetExtension($resolved) -ne '.jar'){throw 'Target must be an existing JAR copy'}
if($resolved -eq $source -or $resolved -eq $modified){throw 'Use a deployed JAR or a separate test copy'}
$current=Hash $resolved
if($current -eq '@BASE@'){Write-Output 'ROLLBACK ALREADY RESTORED sha256=@BASE@ worlds=UNCHANGED';exit 0}
if($current -ne '@MODIFIED@'){throw 'Target does not match the verified modified package'}
Copy-Item -LiteralPath $source -Destination $resolved
if((Hash $resolved) -ne '@BASE@'){throw 'Restored hash mismatch'}
Write-Output 'ROLLBACK PASS sha256=@BASE@ worlds=UNCHANGED'
"""
    (OUT/'ROLLBACK.ps1').write_text(ps.replace('@BASE@',BASE_JAR).replace('@MODIFIED@',sha(modified)),encoding='utf-8')
    (OUT/'ROLLBACK.sh').write_text("""#!/usr/bin/env bash
set -euo pipefail
HERE=$(cd -- "$(dirname -- "$0")" && pwd)
if [ "$#" -ne 1 ]; then printf '%s\n' 'Usage: ROLLBACK.sh EXISTING_JAR_COPY'; exit 64; fi
exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$HERE/ROLLBACK.ps1" -Target "$1"
""",encoding='utf-8',newline='\n')
    (OUT/'ROLLBACK.sh').chmod(0o755)
    result={p.name:{'size':p.stat().st_size,'sha256':sha(p)} for p in OUT.iterdir() if p.is_file() and p.name!='ARTIFACTS.json'}
    (OUT/'ARTIFACTS.json').write_text(json.dumps({'baseline':BASE,'implementation':head,'artifacts':result},indent=2)+'\n',encoding='utf-8')
    print('PASS packaged implementation='+head)
    for name in ['whileaway-0.3.1-v2-03-content.jar','whileaway-0.3.1-v2-03-content-source.zip']:
        print(name+' sha256='+result[name]['sha256']+' bytes='+str(result[name]['size']))
if __name__=='__main__':main()
