param(
    [string]$Repository = 'scarlel96-design/WHILE-YOU-WERE-AWAY',
    [string]$Branch = 'work/v2-playable-core',
    [string]$BaseCommit = '87917ce518b5dbdd0d9631e600033f5060523b0a'
)

$ErrorActionPreference = 'Stop'
$credentialFile = Join-Path $env:USERPROFILE '.git-credentials'
$credentialUri = [uri](Get-Content -LiteralPath $credentialFile | Where-Object { $_ -match 'github\.com' } | Select-Object -First 1)
$credentialPair = [uri]::UnescapeDataString($credentialUri.UserInfo)
$credentialHeader = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($credentialPair))
$headers = @{ Authorization = "Basic $credentialHeader"; 'User-Agent' = 'WhileAway-Publisher'; Accept = 'application/vnd.github+json' }
$api = "https://api.github.com/repos/$Repository"

function Invoke-GitHubJson([string]$Method, [string]$Path, $Payload) {
    $args = @{ Method = $Method; Uri = "$api/$Path"; Headers = $headers }
    if ($null -ne $Payload) {
        $args.Body = ConvertTo-Json -InputObject $Payload -Depth 20 -Compress
        $args.ContentType = 'application/json'
    }
    Invoke-RestMethod @args
}

function Read-GitBlobBytes([string]$Sha) {
    $start = [Diagnostics.ProcessStartInfo]::new('git', "cat-file blob $Sha")
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.UseShellExecute = $false
    $process = [Diagnostics.Process]::Start($start)
    $buffer = [IO.MemoryStream]::new()
    $process.StandardOutput.BaseStream.CopyTo($buffer)
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "git cat-file failed: $Sha" }
    $buffer.ToArray()
}

$currentBranch = Invoke-GitHubJson 'Get' "branches/$Branch" $null
if ($currentBranch.commit.sha -ne $BaseCommit) {
    throw "Remote branch changed: $($currentBranch.commit.sha)"
}

$paths = @(git diff --name-only --diff-filter=ACMR $BaseCommit HEAD)
if ($LASTEXITCODE -ne 0 -or $paths.Count -eq 0) { throw 'No commit paths found' }
$treeEntries = @()
foreach ($path in $paths) {
    $index = git ls-files -s -- $path
    if ($LASTEXITCODE -ne 0 -or $index -notmatch '^([0-9]{6})\s+([0-9a-f]{40})\s+\d+\t') {
        throw "Index entry missing: $path"
    }
    $mode = $Matches[1]
    $expectedBlob = $Matches[2]
    $bytes = Read-GitBlobBytes $expectedBlob
    $blob = Invoke-GitHubJson 'Post' 'git/blobs' @{ content = [Convert]::ToBase64String($bytes); encoding = 'base64' }
    if ($blob.sha -ne $expectedBlob) { throw "Blob hash mismatch: $path" }
    $treeEntries += @{ path = $path.Replace('\', '/'); mode = $mode; type = 'blob'; sha = $blob.sha }
    Write-Output "BLOB_OK $path $($blob.sha) $($bytes.Length)"
}

$newTree = Invoke-GitHubJson 'Post' 'git/trees' @{ base_tree = $currentBranch.commit.commit.tree.sha; tree = $treeEntries }
$commit = Invoke-GitHubJson 'Post' 'git/commits' @{
    message = 'Publish V2 playable opening, tests, and verified package'
    tree = $newTree.sha
    parents = @($BaseCommit)
}
$null = Invoke-GitHubJson 'Patch' "git/refs/heads/$Branch" @{ sha = $commit.sha; force = $false }
$confirmed = Invoke-GitHubJson 'Get' "branches/$Branch" $null
if ($confirmed.commit.sha -ne $commit.sha) { throw 'Remote ref did not confirm the commit' }
$remoteTree = Invoke-GitHubJson 'Get' "git/trees/$($newTree.sha)?recursive=1" $null
foreach ($entry in $treeEntries) {
    $found = $remoteTree.tree | Where-Object { $_.path -eq $entry.path } | Select-Object -First 1
    if ($found.sha -ne $entry.sha) { throw "Remote tree mismatch: $($entry.path)" }
}
Write-Output "REMOTE_COMMIT $($commit.sha)"
Write-Output "REMOTE_FILES_VERIFIED $($treeEntries.Count)"
