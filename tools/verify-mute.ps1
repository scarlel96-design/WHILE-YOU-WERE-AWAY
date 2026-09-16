param([string]$OptionsPath = (Join-Path $PSScriptRoot '..\..\..\work\client-smoke\options.txt'))
$ErrorActionPreference = 'Stop'
$lines = Get-Content -LiteralPath $OptionsPath
foreach ($category in @('master','music','record','weather','block','hostile','neutral','player','ambient','voice')) {
    $values = @($lines | Where-Object { $_.StartsWith("soundCategory_${category}:") })
    if ($values.Count -ne 1 -or $values[0] -ne "soundCategory_${category}:0.0") { throw "Not muted: $category" }
}
if (@($lines | Where-Object { $_ -eq 'narrator:0' }).Count -ne 1) { throw 'Narrator not disabled' }
Write-Output 'PASS muted options: 10 sound categories=0.0; narrator=0; isolated profile'
