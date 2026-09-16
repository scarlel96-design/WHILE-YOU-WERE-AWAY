$ErrorActionPreference='Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
$jdk='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin'
$folder='build/npc-world-fact-probe'
New-Item -ItemType Directory -Force $folder | Out-Null
$cp=@((Resolve-Path build/classes/java/main).Path,(Resolve-Path build/moddev/artifacts/neoforge-21.1.249-merged.jar).Path)
$cp+=Get-Content build/moddev/actorExceptionsCopy1LegacyClasspath.txt
$cp=$cp -join ';'
# Argument files avoid Windows command-line length limits; use slash paths for Java parsing.
@('-encoding','UTF-8','-cp',('"'+$cp.Replace('\','/')+'"'),'-d',$folder,'tools/NpcWorldFactProbe.java') | Set-Content "$folder/compile.args" -Encoding utf8NoBOM
& "$jdk/javac.exe" "@$folder/compile.args"
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
@('-cp',('"'+($folder+';'+$cp).Replace('\','/')+'"'),'io.github.whileaway.NpcWorldFactProbe','evidence/npc-lifecycle/client/CutA.dat',("$folder/data-"+[DateTime]::UtcNow.Ticks)) | Set-Content "$folder/run.args" -Encoding utf8NoBOM
& "$jdk/java.exe" "@$folder/run.args" *> evidence/npc-path/npc-facts-probe.log
$rc=$LASTEXITCODE
Get-Content evidence/npc-path/npc-facts-probe.log -Tail 20
exit $rc

