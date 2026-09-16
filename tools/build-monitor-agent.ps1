$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location $root
$java='C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin'
New-Item -ItemType Directory -Force -Path build/monitor-agent | Out-Null
& "$java/javac.exe" --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -d build/monitor-agent tools/MonitorAgent.java
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
"Premain-Class: MonitorAgent`n" | Set-Content build/monitor-agent/MANIFEST.MF -Encoding ascii
& "$java/jar.exe" --create --file build/monitor-agent.jar --manifest build/monitor-agent/MANIFEST.MF -C build/monitor-agent .
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
Write-Output 'PASS development monitor agent built; not bundled into mod JAR'
