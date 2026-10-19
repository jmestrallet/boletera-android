# Runs the daily site watch; when STM, ID Uruguay or the Prex gateway changed, it shows a Windows notification.
$root = Split-Path $PSScriptRoot
Set-Location $root
$out = & node .tools/watch-sites.mjs 2>&1 | Out-String
if ($LASTEXITCODE -eq 1) {
    $text = "Cambió una página que usa Boletera. Revisá .tools/site-watch.log`n" + ($out.Trim() -split "`n" | Select-Object -First 3) -join "`n"
    Add-Type -AssemblyName System.Windows.Forms
    $n = New-Object System.Windows.Forms.NotifyIcon
    $n.Icon = [System.Drawing.SystemIcons]::Warning; $n.Visible = $true
    $n.ShowBalloonTip(20000, 'Boletera: cambió STM o Prex', $text, 'Warning')
    Start-Sleep -Seconds 25; $n.Dispose()
}
