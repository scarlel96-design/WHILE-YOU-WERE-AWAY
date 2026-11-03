param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference='Stop'
function Hash([string]$Path) {
    $algorithm=[Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($algorithm.ComputeHash([IO.File]::ReadAllBytes($Path)))).Replace('-','').ToLowerInvariant() }
    finally { $algorithm.Dispose() }
}
$source=Join-Path $PSScriptRoot 'BASELINE.jar'
$modified=Join-Path $PSScriptRoot 'whileaway-0.3.1-v2-03-terrain.jar'
if((Hash $source) -ne '72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05'){throw 'Baseline hash mismatch'}
$resolved=(Resolve-Path -LiteralPath $Target).ProviderPath
if([IO.Path]::GetExtension($resolved) -ne '.jar'){throw 'Target must be an existing JAR copy'}
if($resolved -eq $source -or $resolved -eq $modified){throw 'Use a deployed JAR or a separate test copy'}
$current=Hash $resolved
if($current -eq '72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05'){Write-Output 'ROLLBACK ALREADY RESTORED sha256=72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05 worlds=UNCHANGED';exit 0}
if($current -ne 'bbc8ffdf438d9394fcfb57fe1103c4677f1deb1ad394fe415424410ee17b4283'){throw 'Target does not match the verified modified package'}
Copy-Item -LiteralPath $source -Destination $resolved
if((Hash $resolved) -ne '72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05'){throw 'Restored hash mismatch'}
Write-Output 'ROLLBACK PASS sha256=72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05 worlds=UNCHANGED'
