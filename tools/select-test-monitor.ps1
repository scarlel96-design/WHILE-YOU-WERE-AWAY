param([string]$OutputPath='evidence/campaign/monitor.json')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Windows.Forms
$screen=@([System.Windows.Forms.Screen]::AllScreens | Where-Object DeviceName -eq '\\.\DISPLAY1')
if($screen.Count -ne 1 -or $screen[0].Primary){throw 'Expected user-selected right secondary DISPLAY1; display configuration changed'}
$b=$screen[0].WorkingArea
@{device=$screen[0].DeviceName;left=$b.X;top=$b.Y;width=$b.Width;height=$b.Height;x=$b.X+120;y=$b.Y+120;windowWidth=1100;windowHeight=700} | ConvertTo-Json | Set-Content $OutputPath -Encoding utf8NoBOM
Write-Output "PASS selected right secondary DISPLAY1 bounds=$b"
