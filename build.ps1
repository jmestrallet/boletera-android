param([switch]$SkipChecks)
$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
Push-Location $taskRoot
try {
    $jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'
    if (Test-Path $jdk) { $env:JAVA_HOME = $jdk }
    $env:GRADLE_USER_HOME = Join-Path $taskRoot '.tools\gradle-cache'
    $gradle = Join-Path $taskRoot '.tools\gradle-8.11.1\bin\gradle.bat'
    if (-not (Test-Path $gradle)) { throw 'Falta Gradle. Consultá README.md para preparar el entorno.' }
    if (-not $SkipChecks) {
        & npm test
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas del adaptador.' }
    }
    if (-not (Test-Path '.tools/signing.properties')) { throw 'Falta la firma local de prueba. Consultá README.md.' }
    $tasks = @(':app:assembleRelease')
    if (-not $SkipChecks) { $tasks += ':app:testDebugUnitTest', ':app:lintDebug' }
    & $gradle --no-daemon --console=plain @tasks
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o sus comprobaciones.' }
    New-Item -ItemType Directory -Force outputs | Out-Null
    Copy-Item app/build/outputs/apk/release/app-release.apk outputs/boletera-prueba-0.2.7.apk -Force
    Get-FileHash outputs/boletera-prueba-0.2.7.apk -Algorithm SHA256
} finally { Pop-Location }
