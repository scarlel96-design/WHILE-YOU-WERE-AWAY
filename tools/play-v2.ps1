param([switch]$Preview)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    New-Item -ItemType Directory -Force evidence/v2-01d | Out-Null
    & tools/select-test-monitor.ps1 -OutputPath evidence/v2-01d/monitor.json
    & tools/build-monitor-agent.ps1
    if($LASTEXITCODE -ne 0){throw 'Monitor launcher build failed'}
    $env:GRADLE_USER_HOME=Join-Path (Split-Path -Parent $projectRoot) 'gradle-home'
    $task=if($Preview){'runV2TownPreview'}else{'runV2PlayableManual'}
    & .\gradlew.bat --offline --no-daemon --init-script tools/ci/v2-playable-manual.init.gradle $task --console=plain
    $result=$LASTEXITCODE
} finally { Pop-Location }
exit $result
