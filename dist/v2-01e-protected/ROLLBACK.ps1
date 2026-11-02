param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
$baseline=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'BASELINE-source.zip'))
$modified=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'MODIFIED-source.zip'))
$destination=[IO.Path]::GetFullPath($Target)
function Hash([string]$p){
    $sha=[Security.Cryptography.SHA256]::Create()
    try { [BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($p))).Replace('-','').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
$expected='f3ab52c1fa83b896b89a9cc72765ab2019374d7f8e27c58ed2868d678878a77c'
if((Hash $baseline) -ne $expected){throw 'Baseline hash mismatch'}
if($destination -eq $baseline -or $destination -eq $modified){throw 'Preserved package rejected'}
if([IO.Path]::GetExtension($destination) -ne '.zip' -or $destination -match '[\/]saves[\/]'){throw 'Only a source ZIP copy is accepted'}
if((Hash $destination) -ne (Hash $modified)){throw 'Expected a copy of the modified source ZIP'}
[IO.File]::Copy($baseline,$destination,$true)
if((Hash $destination) -ne $expected){throw 'Restored hash mismatch'}
Write-Output 'ROLLBACK: source-package=PASS world-files-touched=0'
