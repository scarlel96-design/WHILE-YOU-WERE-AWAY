param(
  [string]$Message = "Update While You Were Away"
)
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repo
if (-not (git rev-parse --is-inside-work-tree 2>$null)) { throw "Not a Git worktree: $repo" }
$remote = git remote get-url origin
if ($remote -ne "https://github.com/scarlel96-design/WHILE-YOU-WERE-AWAY.git") { throw "Unexpected origin: $remote" }
$branch = git branch --show-current
if ([string]::IsNullOrWhiteSpace($branch)) { throw "No current branch" }
git add -A
git diff --cached --quiet
if ($LASTEXITCODE -eq 0) { Write-Output "No changes to upload."; exit 0 }
git commit -m $Message
git push -u origin $branch
Write-Output "UPLOAD PASS: $branch -> origin/$branch"
