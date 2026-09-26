param([Parameter(Mandatory=$true)][string]$Mode,[string]$World,[int]$Attempt=1)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot'
$env:GRADLE_USER_HOME=(Resolve-Path '..\gradle-home').Path
$e=Join-Path $root 'evidence/return-network-signal'
$label=if($Mode -eq 'ReturnLight'){'ResumeB'}else{$Mode}
$run="$Mode-$Attempt"; $dir=if($Mode -eq 'ReturnLight'){Join-Path $e "client/$run/ReturnLight"}else{Join-Path $e "client/$run"}
$log=Join-Path $e "client-$Mode-$Attempt.log"
if(Test-Path -LiteralPath $log){throw 'Preserve existing log; choose a new attempt'}
$sources=@(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -File | Sort-Object FullName | ForEach-Object { @{path=$_.FullName.Substring($root.Length+1);hash=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash} })
$sources | ConvertTo-Json | Set-Content -Encoding utf8NoBOM (Join-Path $e "sources-$Mode-$Attempt.json")
$argsList=@('--offline','-I','tools/ci/return-network-signal.init.gradle',"runSignal$Mode","-PsignalRun=$run")
if($World){$argsList+="-PsignalWorld=$World"}
$start=Get-Date
& .\gradlew.bat @argsList *> $log
$code=$LASTEXITCODE
$pidFile=Join-Path $dir "$label.pid"
$clientPid=if(Test-Path -LiteralPath $pidFile){[int](Get-Content -LiteralPath $pidFile)}else{0}
$exited=$clientPid -gt 0 -and !(Get-Process -Id $clientPid -ErrorAction SilentlyContinue)
$marker=Join-Path $dir "$label.txt"
$pass=(Test-Path -LiteralPath $marker) -and (Select-String -LiteralPath $marker -SimpleMatch "PASS $label " -Quiet) -and !(Select-String -LiteralPath $marker -SimpleMatch 'FAIL ' -Quiet)
$text=if(Test-Path -LiteralPath $marker){Get-Content -Raw -LiteralPath $marker}else{''}
$child=(Select-String -LiteralPath $log -Pattern 'SIGNAL_CHILD_EXIT=(-?\d+)' | Select-Object -Last 1)
$childExit=if($child){[long]$child.Matches[0].Groups[1].Value}else{$null}
$expectedExit=($childExit -eq 0) -or ($Mode.StartsWith('Cut') -and $childExit -eq -1073740791 -and $text.Contains('boundary=') -and $text.Contains('worldFlushBeforeCut=false'))
$sourceUnchanged=$true
foreach($s in $sources){if((Get-FileHash -LiteralPath (Join-Path $root $s.path)).Hash -ne $s.hash){$sourceUnchanged=$false}}
$row=@{mode=$Mode;attempt=$Attempt;world=$World;command="gradlew.bat $($argsList -join ' ')";exit=$code;childExit=$childExit;expectedExit=$expectedExit;pid=$clientPid;processExited=$exited;pass=$pass;muted=$text.Contains('all sound categories=0');rightMonitor=$text.Contains('PASS monitor DISPLAY1');sourceUnchanged=$sourceUnchanged;elapsedSeconds=[int]((Get-Date)-$start).TotalSeconds}
$row | ConvertTo-Json -Compress | Add-Content -Encoding utf8NoBOM (Join-Path $e 'commands.jsonl')
$row | ConvertTo-Json -Compress
if($code -ne 0 -or !$exited -or !$pass -or !$expectedExit -or !$sourceUnchanged){Get-Content -LiteralPath $log -Tail 20;throw 'Signal client gate incomplete'}
