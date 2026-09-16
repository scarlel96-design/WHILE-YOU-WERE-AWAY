$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$rows=[System.Collections.Generic.List[object]]::new()
function Probe([string]$phase,[string]$path,[string]$version,[int]$revision) {
    $result=(& python .\tools\probe-art.py $path $version $revision 2>&1 | Out-String).Trim();$code=$LASTEXITCODE
    $rows.Add(@{phase=$phase;command="python .\tools\probe-art.py $path $version $revision";input="JAR=$path; expected version=$version, art_revision=$revision";output=$result;exit=$code})
    Write-Output "$phase $result exit=$code";if($code -ne 0){throw "$phase probe failed"}
}
$mod='.\dist\whileaway-0.3.0-dev.1.jar';$original=(Get-FileHash $mod).Hash
Probe 'BASELINE' '.\evidence\art\baseline\previous.jar' '0.2.0-dev.1' 1
Probe 'MODIFIED' $mod '0.3.0-dev.1' 2
New-Item -ItemType Directory -Force '.\build\rollback-art' | Out-Null
Copy-Item -LiteralPath $mod -Destination '.\build\rollback-art\whileaway.jar' -Force
$bash='C:\Program Files\Git\bin\bash.exe'
$cmd='chmod +x evidence/art/ROLLBACK.sh && test -x evidence/art/ROLLBACK.sh && evidence/art/ROLLBACK.sh build/rollback-art/whileaway.jar'
$result=(& $bash -c $cmd 2>&1 | Out-String).Trim();$code=$LASTEXITCODE
$rows.Add(@{phase='ROLLBACK';command="& '$bash' -c '$cmd'";input='build/rollback-art/whileaway.jar (modified copy)';output=$result;exit=$code})
Write-Output "$result exit=$code";if($code -ne 0){throw 'JAR rollback failed'}
Probe 'ROLLBACK' '.\build\rollback-art\whileaway.jar' '0.2.0-dev.1' 1
if((Get-FileHash '.\build\rollback-art\whileaway.jar').Hash -ne (Get-FileHash '.\evidence\art\baseline\previous.jar').Hash){throw 'Baseline hash differs'}
if((Get-FileHash $mod).Hash -ne $original){throw 'Modified release changed'}
$rows | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 '.\evidence\art\transaction.json'
Write-Output 'PASS rollback copy restored; 0.3 release retained'
