param([Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference='Stop'
$baseline=Join-Path $PSScriptRoot 'BASELINE.jar'
$expected='B73F009F52564C7CB495603F51FE549716642A1D4E8A903B36B84933D7F194B4'
if((Get-FileHash -LiteralPath $baseline -Algorithm SHA256).Hash -ne $expected){throw 'BASELINE_HASH_MISMATCH'}
$target=(Resolve-Path -LiteralPath $Destination).Path
if($target -eq (Resolve-Path -LiteralPath $baseline).Path){throw 'BASELINE_IS_IMMUTABLE'}
Copy-Item -LiteralPath $baseline -Destination $target -Force
if((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $expected){throw 'RESTORE_HASH_MISMATCH'}
Write-Output "ROLLBACK_PACKAGE_RESTORED sha256=$($expected.ToLowerInvariant())"
