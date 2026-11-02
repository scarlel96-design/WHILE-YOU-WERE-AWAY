param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
$baseline=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../v2-01e/whileaway-0.3.1-v2-01e-source.zip'))
$destination=[IO.Path]::GetFullPath($Target)
$expected='bcb05cb3ad5b1c93bd4db28d221a823d418bbe3453dba63ddb48940617e1c370'
function Hash([string]$p){
    $sha=[Security.Cryptography.SHA256]::Create()
    try { [BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($p))).Replace('-','').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
if((Hash $baseline) -ne $expected){throw 'Baseline hash mismatch'}
if($destination -eq $baseline -or $destination -eq (Join-Path $PSScriptRoot 'whileaway-0.3.1-v2-01e-continuation-source.zip')){throw 'Preserved artifact target rejected'}
if([IO.Path]::GetExtension($destination) -ne '.zip' -or $destination -match '[\\/]saves[\\/]'){throw 'Only a package ZIP target is accepted'}
[IO.File]::Copy($baseline,$destination,$true)
$actual=Hash $destination
if($actual -ne $expected){throw 'Restore hash mismatch'}
Write-Output "ROLLBACK_SOURCE_PACKAGE_SHA256=$actual"
Write-Output 'WORLD_FILES_TOUCHED=0'
