param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
$source=[IO.Path]::GetFullPath($PSScriptRoot+'/../../evidence/v2-01e/BASELINE.jar')
function Hash([string]$p){([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash([IO.File]::ReadAllBytes($p)))).Replace('-','').ToLowerInvariant()}
$targetPath=[IO.Path]::GetFullPath($Target)
if($targetPath -notlike '*.jar' -or $targetPath -match '[\\/](saves|world)[\\/]'){throw 'Target must be a package JAR'}
$expected='3c92f0fb7cf1d7981d3bef1b6cc14787c6eff408d8166cdf87a694c8f6050460'
if((Hash $source) -ne $expected){throw 'Baseline hash mismatch'}
if([IO.File]::Exists($targetPath)){[IO.File]::Copy($targetPath,($targetPath+'.before-'+[guid]::NewGuid().ToString()),$false)}
[IO.File]::Copy($source,$targetPath,$true)
if((Hash $targetPath) -ne $expected){throw 'Restore hash mismatch'}
[Console]::WriteLine('ROLLBACK_PACKAGE_PASS SHA256='+$expected+' WORLD_FILES_TOUCHED=0')
