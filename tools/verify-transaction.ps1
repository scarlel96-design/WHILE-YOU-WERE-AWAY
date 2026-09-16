$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$rows = [System.Collections.Generic.List[object]]::new()
function Run-Probe([string]$label, [string]$source, [string]$outputDir, [string]$expected) {
    New-Item -ItemType Directory -Force $outputDir | Out-Null
    $compile = (& javac -encoding UTF-8 -d $outputDir $source .\tools\PolicyProbe.java 2>&1 | Out-String).Trim()
    $compileExit = $LASTEXITCODE
    $rows.Add(@{phase=$label;command="javac -encoding UTF-8 -d $outputDir $source .\tools\PolicyProbe.java";input=$source;output=$compile;exit=$compileExit})
    if ($compileExit -ne 0) { throw "$label compile failed: $compile" }
    $result = (& java -cp $outputDir PolicyProbe 3 true $expected 2>&1 | Out-String).Trim()
    $code = $LASTEXITCODE
    $rows.Add(@{phase=$label;command="java -cp $outputDir PolicyProbe 3 true $expected";input="distinctClues=3; signalRestored=true; expected=$expected";output=$result;exit=$code})
    Write-Output "$label $result exit=$code"
    if ($code -ne 0) { throw "$label behavior failed" }
}
$baseline = '.\evidence\baseline\ThreatPolicy.java'
$modified = '.\src\main\java\io\github\whileaway\core\ThreatPolicy.java'
$before = (Get-FileHash -LiteralPath $modified -Algorithm SHA256).Hash
Run-Probe 'BASELINE' $baseline '.\build\probe-baseline' 'false'
Run-Probe 'MODIFIED' $modified '.\build\probe-modified' 'true'
New-Item -ItemType Directory -Force '.\build\rollback-copy' | Out-Null
Copy-Item -LiteralPath $modified -Destination '.\build\rollback-copy\ThreatPolicy.java' -Force
$bash = 'C:\Program Files\Git\bin\bash.exe'
$script = 'chmod +x ./ROLLBACK.sh && test -x ./ROLLBACK.sh && ./ROLLBACK.sh build/rollback-copy/ThreatPolicy.java'
$restored = (& $bash -c $script 2>&1 | Out-String).Trim()
$code = $LASTEXITCODE
$rows.Add(@{phase='ROLLBACK';command="& '$bash' -c '$script'";input='build/rollback-copy/ThreatPolicy.java (modified copy)';output=$restored;exit=$code})
Write-Output "$restored exit=$code"
if ($code -ne 0) { throw 'Rollback execution failed' }
if ((Get-FileHash $baseline).Hash -ne (Get-FileHash '.\build\rollback-copy\ThreatPolicy.java').Hash) { throw 'Restored hash mismatch' }
Run-Probe 'ROLLBACK' '.\build\rollback-copy\ThreatPolicy.java' '.\build\probe-rollback' 'false'
if ($before -ne (Get-FileHash $modified).Hash) { throw 'Working source changed during rollback test' }
$rows | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 '.\evidence\transaction.json'
Write-Output 'PASS baseline/modified/rollback; working source remains modified'
