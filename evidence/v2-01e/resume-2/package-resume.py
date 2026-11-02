"""Package the existing production source and isolated test-tool changes."""
import hashlib,json,shutil,subprocess
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
root=Path(__file__).resolve().parents[3]
out=root/'dist/v2-01e-protected'
out.mkdir(exist_ok=True)
base=root/'dist/v2-01e-continuation/whileaway-0.3.1-v2-01e-continuation-source.zip'
expected='f3ab52c1fa83b896b89a9cc72765ab2019374d7f8e27c58ed2868d678878a77c'
assert hashlib.sha256(base.read_bytes()).hexdigest()==expected
shutil.copy2(base,out/'BASELINE-source.zip')
archive=out/'MODIFIED-source.zip'
with ZipFile(base) as old,ZipFile(archive,'w',ZIP_DEFLATED) as new:
    for name in old.namelist():
        new.writestr(name,(root/name).read_bytes())
jar=root/'dist/v2-01e/whileaway-0.3.1-v2-01e.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='5adb5f964d42e1e9355a79fc491db8c32340de5e9a172748a26c8d926c7bcc4a'
shutil.copy2(jar,out/jar.name)
r=subprocess.run(['git','diff','--binary','7620506f4450c622e21dc3d877c413a3e4067a39','--','tools/ci/v2-01e.init.gradle','tools/validation/play-input/java/wwa/input/InputAssist.java'],cwd=root,capture_output=True,check=True)
(out/'DIFF.patch').write_bytes(r.stdout)
(out/'.gitattributes').write_text('.gitattributes text eol=lf\n*.sh text eol=lf\n*.ps1 text eol=lf\n*.patch text eol=lf\n*.txt text eol=lf\n*.json text eol=lf\n*.jar -text\n*.zip -text\n',encoding='utf-8',newline='\n')
(out/'ROLLBACK.ps1').write_text("""param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
$baseline=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'BASELINE-source.zip'))
$modified=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'MODIFIED-source.zip'))
$destination=[IO.Path]::GetFullPath($Target)
function Hash([string]$p){
    $sha=[Security.Cryptography.SHA256]::Create()
    try { [BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($p))).Replace('-','').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
$expected='"""+expected+"""'
if((Hash $baseline) -ne $expected){throw 'Baseline hash mismatch'}
if($destination -eq $baseline -or $destination -eq $modified){throw 'Preserved package rejected'}
if([IO.Path]::GetExtension($destination) -ne '.zip' -or $destination -match '[\\/]saves[\\/]'){throw 'Only a source ZIP copy is accepted'}
if((Hash $destination) -ne (Hash $modified)){throw 'Expected a copy of the modified source ZIP'}
[IO.File]::Copy($baseline,$destination,$true)
if((Hash $destination) -ne $expected){throw 'Restored hash mismatch'}
Write-Output 'ROLLBACK: source-package=PASS world-files-touched=0'
""",encoding='utf-8',newline='\n')
(out/'ROLLBACK.sh').write_text('#!/usr/bin/env bash\nset -euo pipefail\nhere="$(cd -- "$(dirname -- "$0")" && pwd)"\npowershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$here/ROLLBACK.ps1")" -Target "${1:?Pass a modified source ZIP copy path}"\n',encoding='utf-8',newline='\n')
with ZipFile(archive) as z:
    (out/'SOURCE_MANIFEST.json').write_text(json.dumps([{ 'path':n,'size':len(z.read(n)),'sha256':hashlib.sha256(z.read(n)).hexdigest()} for n in z.namelist()],indent=2)+'\n',encoding='utf-8',newline='\n')
for p in sorted(out.iterdir()):
    if p.is_file():print(p.name,p.stat().st_size,hashlib.sha256(p.read_bytes()).hexdigest())
