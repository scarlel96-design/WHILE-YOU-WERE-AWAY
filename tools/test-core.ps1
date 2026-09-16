$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$out=Join-Path $root 'build/core-tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$sources=@(Get-ChildItem (Join-Path $root 'src/main/java/io/github/whileaway/core') -Filter '*.java' | ForEach-Object {$_.FullName})
$sources+=Join-Path $root 'src/test/java/io/github/whileaway/core/CoreTests.java'
$sources+=Join-Path $root 'src/test/java/io/github/whileaway/core/ActorPresenceTests.java'
& javac -encoding UTF-8 -d $out @sources
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& java -cp $out io.github.whileaway.core.CoreTests
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& java -cp $out io.github.whileaway.core.ActorPresenceTests
exit $LASTEXITCODE
