param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
function Hash([string]$p){
 $sha=[Security.Cryptography.SHA256]::Create()
 try { [BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($p))).Replace('-','').ToLowerInvariant() }
 finally { $sha.Dispose() }
}
$b=Join-Path $PSScriptRoot 'BASELINE.jar'
$t=[IO.Path]::GetFullPath($Target)
if($t -eq [IO.Path]::GetFullPath($b) -or $t -eq [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'whileaway-0.3.1-v2-03.jar'))){throw 'Preserved artifact rejected'}
if([IO.Path]::GetExtension($t) -ne '.jar' -or $t -match '[\\/]saves[\\/]'){throw 'Only a package JAR copy is accepted'}
if((Hash $b) -ne '5adb5f964d42e1e9355a79fc491db8c32340de5e9a172748a26c8d926c7bcc4a'){throw 'Baseline hash mismatch'}
if((Hash $t) -ne '7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6'){throw 'Expected modified JAR copy'}
Copy-Item -LiteralPath $b -Destination $t -Force
if((Hash $t) -ne '5adb5f964d42e1e9355a79fc491db8c32340de5e9a172748a26c8d926c7bcc4a'){throw 'Restored hash mismatch'}
Write-Output 'ROLLBACK JAR PASS; WORLD_FILES_TOUCHED=0'
