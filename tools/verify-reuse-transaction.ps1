$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot; Set-Location $root
$java='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe'
$javac='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\javac.exe'
$e=Join-Path $root 'evidence/reuse'; $base=Join-Path $e 'baseline/previous.jar'
$mod=Join-Path $root 'dist/stability-work/whileaway-0.3.1-stability-work.jar'
New-Item -ItemType Directory -Force build/reuse-probe,build/rollback-reuse | Out-Null
& $javac -encoding UTF-8 -d build/reuse-probe tools/CheckpointProbe.java
if($LASTEXITCODE -ne 0){throw 'Probe compilation failed'}
$commands=@()
foreach($label in @('BASELINE','MODIFIED')){
    $file=if($label -eq 'BASELINE'){$base}else{$mod};$mode=$label.ToLower()
    $result=& $java -cp build/reuse-probe CheckpointProbe $file $mode 2>&1
    $rc=$LASTEXITCODE
    $commands+=@{label=$label;command="java -cp build/reuse-probe CheckpointProbe `"$file`" $mode";input='ABORTED/4; COMPLETED/2';output=($result -join "`n");exit=$rc}
    if($rc -ne 0){throw "$label probe failed"}
}
$target=Join-Path $root 'build/rollback-reuse/test.jar';Copy-Item -LiteralPath $mod -Destination $target
& 'C:\Program Files\Git\bin\bash.exe' -lc 'chmod +x evidence/reuse/ROLLBACK.sh && test -x evidence/reuse/ROLLBACK.sh && evidence/reuse/ROLLBACK.sh build/rollback-reuse/test.jar' 2>&1 | Set-Content "$e/rollback-output.txt" -Encoding utf8
$rc=$LASTEXITCODE
if($rc -ne 0){throw "Rollback failed $rc"}
$commands+=@{label='ROLLBACK';command='bash -lc "chmod +x evidence/reuse/ROLLBACK.sh && test -x evidence/reuse/ROLLBACK.sh && evidence/reuse/ROLLBACK.sh build/rollback-reuse/test.jar"';input=$target;output=(Get-Content "$e/rollback-output.txt" -Raw).Trim();exit=$rc}
$result=& $java -cp build/reuse-probe CheckpointProbe $target baseline 2>&1;$rc=$LASTEXITCODE
$commands+=@{label='RESTORED';command='java -cp build/reuse-probe CheckpointProbe build/rollback-reuse/test.jar baseline';input='ABORTED/4; COMPLETED/2';output=($result -join "`n");exit=$rc}
if($rc -ne 0){throw 'Restored behavior mismatch'}
$restored=(Get-FileHash $target).Hash.ToLower();$original=(Get-FileHash $base).Hash.ToLower();$changed=(Get-FileHash $mod).Hash.ToLower()
if($restored -ne $original -or $changed -eq $original){throw 'Rollback hash mismatch or modified file no longer changed'}
@{commands=$commands;baseline_hash=$original;modified_hash=$changed;restored_hash=$restored;world_operations='none'} | ConvertTo-Json -Depth 5 | Set-Content "$e/transaction.json" -Encoding utf8
$commands | ForEach-Object { Write-Output ($_.label+': '+$_.output+'; exit='+$_.exit) }
