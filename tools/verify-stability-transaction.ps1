$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$rows=[System.Collections.Generic.List[object]]::new()
function Probe([string]$phase,[string]$path,[string]$version,[int]$revision) {
    $result=(& python .\tools\probe-stability.py $path $version $revision 2>&1 | Out-String).Trim();$code=$LASTEXITCODE
    $rows.Add(@{phase=$phase;command="python .\tools\probe-stability.py $path $version $revision";input="JAR=$path; expected version=$version, checkpoint_journal=$revision";output=$result;exit=$code})
    Write-Output "$phase $result exit=$code";if($code -ne 0){throw "$phase probe failed"}
}
$mod='.\dist\whileaway-0.3.1-dev.1.jar';$original=(Get-FileHash $mod).Hash
Probe 'BASELINE' '.\evidence\stability\baseline\previous.jar' '0.3.0-dev.1' 0
Probe 'MODIFIED' $mod '0.3.1-dev.1' 1
New-Item -ItemType Directory -Force '.\build\rollback-stability' | Out-Null
Copy-Item -LiteralPath $mod -Destination '.\build\rollback-stability\whileaway.jar' -Force
$bash='C:\Program Files\Git\bin\bash.exe'
$cmd='chmod +x evidence/stability/ROLLBACK.sh && test -x evidence/stability/ROLLBACK.sh && evidence/stability/ROLLBACK.sh build/rollback-stability/whileaway.jar'
$result=(& $bash -c $cmd 2>&1 | Out-String).Trim();$code=$LASTEXITCODE
$rows.Add(@{phase='ROLLBACK';command="& '$bash' -c '$cmd'";input='build/rollback-stability/whileaway.jar (modified copy)';output=$result;exit=$code})
Write-Output "$result exit=$code";if($code -ne 0){throw 'JAR rollback failed'}
Probe 'ROLLBACK' '.\build\rollback-stability\whileaway.jar' '0.3.0-dev.1' 0
if((Get-FileHash '.\build\rollback-stability\whileaway.jar').Hash -ne (Get-FileHash '.\evidence\stability\baseline\previous.jar').Hash){throw 'Baseline hash differs'}
if((Get-FileHash $mod).Hash -ne $original){throw 'Modified release changed'}
$rows | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 '.\evidence\stability\transaction.json'
Write-Output 'PASS rollback copy restored; 0.3.1 release retained'
