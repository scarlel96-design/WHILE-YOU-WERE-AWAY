param([Parameter(Mandatory=$true)][string[]]$Modes, [string]$World)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot'
$env:GRADLE_USER_HOME=(Resolve-Path '..\gradle-home').Path
$e=Join-Path $root 'evidence\return-network-integration'
$saves=(Resolve-Path '..\return-network-client\saves').Path
foreach($mode in $Modes) {
    $task="runNetwork$mode"; $log=Join-Path $e "final-$mode.log"
    $extra=if($World){"-PnetworkWorld=$World"}elseif($mode -eq 'CopyReopen'){'-PnetworkWorld=world-CopyHold'}else{$null}
    $start=Get-Date
    if($extra){ & .\gradlew.bat --offline $task $extra *> $log }else{ & .\gradlew.bat --offline $task *> $log }
    $code=$LASTEXITCODE
    $marker=Join-Path $e "client\$mode.txt"; $pidFile=Join-Path $e "client\$mode.pid"
    $clientPid=if(Test-Path -LiteralPath $pidFile){[int](Get-Content -LiteralPath $pidFile)}else{0}
    $exited=$clientPid -gt 0 -and !(Get-Process -Id $clientPid -ErrorAction SilentlyContinue)
    $pass=(Test-Path -LiteralPath $marker) -and (Select-String -LiteralPath $marker -Pattern "PASS $mode " -Quiet)
    $child=Select-String -LiteralPath $log -Pattern 'NETWORK_CHILD_EXIT=(-?\d+)' | Select-Object -Last 1
    $childExit=if($child){[int64]$child.Matches[0].Groups[1].Value}else{$null}
    $exitOk=$childExit -eq 0 -or ($mode.StartsWith('Cut') -and $childExit -eq -1073740791)
    @{mode=$mode;command="gradlew.bat --offline $task $extra";exit=$code;childExit=$childExit;pid=$clientPid;processExited=$exited;pass=$pass;elapsedSeconds=[int]((Get-Date)-$start).TotalSeconds} | ConvertTo-Json -Compress | Add-Content (Join-Path $e 'client-final-commands.jsonl')
    Write-Output "$mode exit=$code childExit=$childExit marker=$pass processExited=$exited"
    if($code -ne 0 -or !$pass -or !$exited -or !$exitOk){Get-Content -LiteralPath $log -Tail 25;throw "Client gate incomplete: $mode"}
    if($mode -eq 'CutC') {
        $source=Join-Path $saves 'network-verified';$target=Join-Path $saves 'world-CopyHold'
        if(Test-Path -LiteralPath $target){throw 'Copy fixture exists'}
        Copy-Item -LiteralPath $source -Destination $target -Recurse
        @{source=$source;target=$target;sourceStoryHash=(Get-FileHash -LiteralPath (Join-Path $source 'data\whileaway_story.dat')).Hash;copyStoryHash=(Get-FileHash -LiteralPath (Join-Path $target 'data\whileaway_story.dat')).Hash} | ConvertTo-Json | Set-Content (Join-Path $e 'copy-created.json')
    }
    if($mode -eq 'Reopen') {
        foreach($name in @('CorruptBinding','CorruptShared')) {
            $target=Join-Path $saves "world-$name";if(Test-Path -LiteralPath $target){throw 'Corruption fixture exists'}
            Copy-Item -LiteralPath (Join-Path $saves 'network-verified') -Destination $target -Recurse
        }
    }
}
