# Red de pruebas de Boletera: correr ANTES de armar cualquier APK.
#   .\verificar.ps1            -> npm test + pruebas JVM (Lab)
#   .\verificar.ps1 -Compilar  -> además :app:assembleDebug (que la app compile)
# Sale con código 0 si todo está en verde y con 1 y un mensaje claro si algo se rompe.
# OJO: no redirigir 2>&1 dentro de PowerShell (una advertencia del SDK se toma como error).
param([switch]$Compilar)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$fallos = New-Object System.Collections.Generic.List[string]
$inicio = Get-Date

function Buscar-Herramientas {
    # .tools está ignorado por git: vive en la carpeta principal del repo, no en cada worktree.
    $dir = $root
    while ($dir) {
        if (Test-Path (Join-Path $dir '.tools\gradle-8.11.1\bin\gradle.bat')) { return (Join-Path $dir '.tools') }
        $dir = Split-Path $dir -Parent
    }
    return $null
}

function Paso($nombre) { Write-Host ""; Write-Host "=== $nombre ===" -ForegroundColor Cyan }

Push-Location $root
try {
    $tools = Buscar-Herramientas
    if (-not $tools) { throw 'No encuentro .tools\gradle-8.11.1 (ni acá ni en las carpetas de arriba). Consultá README.md para preparar el entorno.' }

    # 1) Pruebas del adaptador / pago (JavaScript)
    Paso '1/2 npm test (pruebas de tests/*.test.cjs)'
    & npm test
    if ($LASTEXITCODE -ne 0) { $fallos.Add('npm test: fallaron pruebas del adaptador o del pago (arriba está cuál).') }

    # 2) Pruebas unitarias JVM (Kotlin)
    Paso '2/2 pruebas JVM (:app:testDebugUnitTest, canal lab)'
    $jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'
    if (Test-Path $jdk) { $env:JAVA_HOME = $jdk }
    $env:GRADLE_USER_HOME = Join-Path $tools 'gradle-cache'
    if (-not (Test-Path (Join-Path $root 'local.properties'))) {
        $sdk = Join-Path $tools 'android-sdk'
        if (Test-Path $sdk) { $env:ANDROID_HOME = $sdk; $env:ANDROID_SDK_ROOT = $sdk }
    }
    $gradle = Join-Path $tools 'gradle-8.11.1\bin\gradle.bat'
    $tareas = @(':app:testDebugUnitTest')
    if ($Compilar) { $tareas += ':app:assembleDebug' }
    & $gradle --offline --no-daemon --console=plain '-PboleteraChannel=lab' @tareas
    if ($LASTEXITCODE -ne 0) {
        $fallos.Add('Gradle: fallaron pruebas Kotlin o no compiló (el detalle está arriba; los informes en app\build\reports\tests).')
    }
} catch {
    $fallos.Add("No se pudo correr la verificación: $($_.Exception.Message)")
} finally {
    Pop-Location
}

$seg = [int]((Get-Date) - $inicio).TotalSeconds
Write-Host ""
if ($fallos.Count -eq 0) {
    Write-Host "VERIFICACION EN VERDE ($seg s): se puede armar el APK." -ForegroundColor Green
    exit 0
}
Write-Host "VERIFICACION EN ROJO ($seg s): NO armes el APK todavia." -ForegroundColor Red
foreach ($f in $fallos) { Write-Host " - $f" -ForegroundColor Red }
exit 1
