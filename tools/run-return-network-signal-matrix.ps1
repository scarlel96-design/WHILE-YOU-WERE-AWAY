param([switch]$Cuts,[switch]$Exceptions,[switch]$ResumeAfterFirstCut,[switch]$ResumeAfterGap2)
$ErrorActionPreference='Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
$seed=(Resolve-Path '../return-network-client/saves/network-seed-preserved').Path
$saves=(Resolve-Path '../return-network-signal-client/saves').Path
function Fresh([string]$name,[string]$inputPath=$seed){$p=Join-Path $saves $name;if(Test-Path -LiteralPath $p){throw "Preserve existing world $name"};Copy-Item -LiteralPath $inputPath -Destination $p -Recurse}
if($Cuts){
 $i=2
 $modes=@('CutBefore','CutGap1','CutGap2','CutLong','CutEnd','CutE','CutF','CutG')
 if($ResumeAfterFirstCut){& ./tools/run-return-network-signal.ps1 -Mode Reopen -World signal-CutBefore -Attempt 2;if($LASTEXITCODE -ne 0){throw 'First recovery failed'};$i=3;$modes=$modes[1..7]}
 if($ResumeAfterGap2){$i=6;$modes=$modes[3..7]}
 foreach($m in $modes) {
  $world="signal-$m";Fresh $world
  & ./tools/run-return-network-signal.ps1 -Mode $m -World $world -Attempt 1
  if($LASTEXITCODE -ne 0){throw "Cut failed $m"}
  & ./tools/run-return-network-signal.ps1 -Mode Reopen -World $world -Attempt $i
  if($LASTEXITCODE -ne 0){throw "Recovery failed $m"};$i++
 }
}
if($Exceptions){
 foreach($m in @('Availability','EnvironmentMatrix','Nether','Chunk','Death4Off')) {
  $world="signal-$m";Fresh $world
  & ./tools/run-return-network-signal.ps1 -Mode $m -World $world -Attempt 1
  if($LASTEXITCODE -ne 0){throw "Exception failed $m"}
 }
 Fresh 'npc-CutB' (Resolve-Path '../return-network-closure-client/saves/network').Path
 & ./tools/run-return-network-signal.ps1 -Mode ReturnLight -World npc-CutB -Attempt 1
 if($LASTEXITCODE -ne 0){throw 'Return-Light regression failed'}
}
