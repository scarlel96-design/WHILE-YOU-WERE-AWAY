$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot; Set-Location $root
$java='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe'
$javac='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\javac.exe'
$e=Join-Path $root 'evidence/npc-world'
$base=Join-Path $e 'baseline/previous.jar'
$mod=Join-Path $root 'dist/npc-world/whileaway-0.3.1-npc-world.jar'
New-Item -ItemType Directory -Force build/npc-world-archive-probe,build/rollback-npc-world | Out-Null
& $javac -encoding UTF-8 -d build/npc-world-archive-probe tools/PresenceArchiveProbe.java tools/NpcWorldArchiveProbe.java
if($LASTEXITCODE -ne 0){throw 'Probe compilation failed'}
$commands=@()
foreach($label in @('BASELINE','MODIFIED')){
    $file=if($label -eq 'BASELINE'){$base}else{$mod};$mode=$label.ToLower()
    $result=& $java -cp build/npc-world-archive-probe NpcWorldArchiveProbe $file $mode 2>&1
    $rc=$LASTEXITCODE
    $commands+=@{label=$label;command="java -cp build/npc-world-archive-probe NpcWorldArchiveProbe `"$file`" $mode";input='8 ActorPresencePolicy cases; explicitLoader/writeGuard true for both; campaign NPC in both; NPC witness guard in both; world/copy smoke harnesses expected only in modified; canonical recovery retained in both';output=($result -join "`n");exit=$rc}
    if($rc -ne 0){throw "$label probe failed"}
}
$target=Join-Path $root 'build/rollback-npc-world/test.jar'
Copy-Item -LiteralPath $mod -Destination $target
$result=& 'C:\Program Files\Git\bin\bash.exe' -lc 'chmod +x evidence/npc-world/ROLLBACK.sh && test -x evidence/npc-world/ROLLBACK.sh && evidence/npc-world/ROLLBACK.sh build/rollback-npc-world/test.jar' 2>&1
$rc=$LASTEXITCODE
$result | Set-Content "$e/rollback.log" -Encoding utf8
if($rc -ne 0){throw "Rollback failed $rc; preserve rollback.log before retry"}
$commands+=@{label='ROLLBACK';command='bash -lc "chmod +x evidence/npc-world/ROLLBACK.sh && test -x evidence/npc-world/ROLLBACK.sh && evidence/npc-world/ROLLBACK.sh build/rollback-npc-world/test.jar"';input=$target;output=($result -join "`n");exit=$rc}
$result=& $java -cp build/npc-world-archive-probe NpcWorldArchiveProbe $target baseline 2>&1;$rc=$LASTEXITCODE
$commands+=@{label='RESTORED';command='java -cp build/npc-world-archive-probe NpcWorldArchiveProbe build/rollback-npc-world/test.jar baseline';input='same 8 policy cases and baseline fixture revision';output=($result -join "`n");exit=$rc}
if($rc -ne 0){throw 'Restored behavior mismatch'}
$restored=(Get-FileHash $target).Hash.ToLower();$original=(Get-FileHash $base).Hash.ToLower();$changed=(Get-FileHash $mod).Hash.ToLower()
if($restored -ne $original -or $changed -eq $original){throw 'Rollback hash mismatch or modified file no longer changed'}
@{commands=$commands;baseline_hash=$original;modified_hash=$changed;restored_hash=$restored;world_operations='none';scope='Packaged policy and fixture revision only. Actual gameplay evidence is the separate current client suites.'} | ConvertTo-Json -Depth 5 | Set-Content "$e/transaction.json" -Encoding utf8
$commands | ForEach-Object { Write-Output ($_.label+': '+$_.output+'; exit='+$_.exit) }

