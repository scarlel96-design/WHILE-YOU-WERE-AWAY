param([int[]]$Kinds=@(1,2,4),[string[]]$Modes=@())
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot'
$env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path
$e=Join-Path $root 'evidence/reuse'
New-Item -ItemType Directory -Force "$e/snapshots" | Out-Null
foreach($kind in $Kinds){$steps=if($Modes.Count -gt 0){$Modes}elseif($kind -eq 4){@('A','B','D','E','Verify')}else{@('D','E','Verify')};foreach($mode in $steps){
    $task="runBoundary$kind$mode"
    & ./gradlew.bat --offline $task *> "$e/$task.log"
    $rc=$LASTEXITCODE
    Add-Content "$e/runtime-commands.txt" "COMMAND ./gradlew.bat --offline $task`nEXIT $rc" -Encoding utf8
    Get-Content "$e/$task.log" -Tail 8
    if($rc -ne 0){throw "$task failed; retained log"}
    $world=(Get-Content "$e/boundary-world-$kind.txt" -Raw).Trim()
    if($world -notmatch '^whileaway-boundary-[124]-[0-9]+$'){throw 'Unexpected isolated world name'}
    $source=Join-Path $root "../../work/boundary-smoke/saves/$world/data/whileaway_story.dat"
    Copy-Item -LiteralPath $source -Destination "$e/snapshots/kind-$kind-$mode.dat"
}}
Write-Output 'PASS boundary pipeline completed'
