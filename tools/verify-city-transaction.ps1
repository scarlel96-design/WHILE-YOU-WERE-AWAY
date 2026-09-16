$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$rows=[System.Collections.Generic.List[object]]::new()
function Probe([string]$phase,[string]$path,[string]$version) {
    $result=(& python .\tools\probe-release.py $path $version 2>&1 | Out-String).Trim();$code=$LASTEXITCODE
    $rows.Add(@{phase=$phase;command="python .\tools\probe-release.py $path $version";input="JAR=$path; expected version=$version";output=$result;exit=$code})
    Write-Output "$phase $result exit=$code";if($code -ne 0){throw "$phase probe failed"}
}
$mod='.\dist\whileaway-0.2.0-dev.1.jar';$original=(Get-FileHash $mod).Hash
Probe 'BASELINE' '.\evidence\city\baseline\whileaway-0.1.0-dev.1.jar' '0.1.0-dev.1'
Probe 'MODIFIED' $mod '0.2.0-dev.1'
New-Item -ItemType Directory -Force '.\build\rollback-city' | Out-Null
Copy-Item -LiteralPath $mod -Destination '.\build\rollback-city\whileaway.jar' -Force
$bash='C:\Program Files\Git\bin\bash.exe'
$cmd='chmod +x evidence/city/ROLLBACK.sh && test -x evidence/city/ROLLBACK.sh && evidence/city/ROLLBACK.sh build/rollback-city/whileaway.jar'
$result=(& $bash -c $cmd 2>&1 | Out-String).Trim();$code=$LASTEXITCODE
$rows.Add(@{phase='ROLLBACK';command="& '$bash' -c '$cmd'";input='build/rollback-city/whileaway.jar (modified copy)';output=$result;exit=$code})
Write-Output "$result exit=$code";if($code -ne 0){throw 'JAR rollback failed'}
Probe 'ROLLBACK' '.\build\rollback-city\whileaway.jar' '0.1.0-dev.1'
if((Get-FileHash '.\build\rollback-city\whileaway.jar').Hash -ne (Get-FileHash '.\evidence\city\baseline\whileaway-0.1.0-dev.1.jar').Hash){throw 'Baseline hash differs'}
if((Get-FileHash $mod).Hash -ne $original){throw 'Modified release changed'}
$rows | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 '.\evidence\city\transaction.json'
Write-Output 'PASS rollback copy restored; 0.2 release retained'
