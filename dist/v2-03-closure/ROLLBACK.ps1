param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
function Hash([string]$Path) {
    $algorithm=[Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($algorithm.ComputeHash([IO.File]::ReadAllBytes($Path)))).Replace('-','').ToLowerInvariant() }
    finally { $algorithm.Dispose() }
}
$source=Join-Path $PSScriptRoot 'BASELINE.jar'
$modified=Join-Path $PSScriptRoot 'whileaway-0.3.1-v2-03-content.jar'
if((Hash $source) -ne '7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6'){throw 'Baseline hash mismatch'}
$resolved=(Resolve-Path -LiteralPath $Target).ProviderPath
if([IO.Path]::GetExtension($resolved) -ne '.jar'){throw 'Target must be an existing JAR copy'}
if($resolved -eq $source -or $resolved -eq $modified){throw 'Use a deployed JAR or a separate test copy'}
$current=Hash $resolved
if($current -eq '7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6'){Write-Output 'ROLLBACK ALREADY RESTORED sha256=7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6 worlds=UNCHANGED';exit 0}
if($current -ne '72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05'){throw 'Target does not match the verified modified package'}
Copy-Item -LiteralPath $source -Destination $resolved
if((Hash $resolved) -ne '7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6'){throw 'Restored hash mismatch'}
Write-Output 'ROLLBACK PASS sha256=7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6 worlds=UNCHANGED'
