param([int[]]$Repetitions=@(1,2))
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot;Set-Location $root
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot'
$env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path
foreach($repeat in $Repetitions){
    if($repeat -notin @(1,2)){throw 'Unexpected fixture repetition'}
    foreach($boundary in @('A','B','C','D','E','Verify')) {
        $task="runActor${boundary}${repeat}"
        & ./gradlew.bat --offline $task *> "evidence/lifecycle/$task.log"
        $rc=$LASTEXITCODE
        Add-Content evidence/lifecycle/runtime-commands.txt "./gradlew.bat --offline $task | input=isolated actor fixture $repeat / $boundary | exit=$rc"
        Write-Output "$task exit=$rc"
        if($rc -ne 0){Get-Content "evidence/lifecycle/$task.log" -Tail 50;throw "$task failed"}
        if(!(Get-Content "evidence/lifecycle/$task.txt" -Raw).Contains("PASS $task")){throw 'Missing cut/completion marker'}
    }
}
