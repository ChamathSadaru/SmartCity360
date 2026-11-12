$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$build = Join-Path $PSScriptRoot 'build'
$destination = Join-Path $build 'windows'
$application = Join-Path $destination 'SmartCity360'
$staging = Join-Path $build ("windows-package-" + [guid]::NewGuid())
$inputFolder = Join-Path $staging 'input'
$outputFolder = Join-Path $staging 'output'
$icon = Join-Path $PSScriptRoot 'src/main/resources/com/smartcity360/ui/app-icon.ico'
$database = Join-Path $PSScriptRoot 'data/smartcity360.db'
$databaseOption = '-Dsmartcity360.database="{0}"' -f $database

New-Item -ItemType Directory -Path $inputFolder, $outputFolder, $destination -Force | Out-Null
try {
    $jarFile = Join-Path $inputFolder 'SmartCity360.jar'
    jar "-J-Djava.io.tmpdir=$staging" --create --file $jarFile --main-class com.smartcity360.ui.DesktopApp -C (Join-Path $build 'classes') .
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the application JAR.' }

    Get-ChildItem -LiteralPath (Join-Path $build 'lib') -Filter '*.jar' -File |
        Copy-Item -Destination $inputFolder
    $packageArgs = @(
        '--type', 'app-image', '--name', 'SmartCity360', '--input', $inputFolder,
        '--dest', $outputFolder, '--main-jar', 'SmartCity360.jar', '--icon', $icon,
        '--main-class', 'com.smartcity360.ui.DesktopApp',
        '--java-options', $databaseOption, '--temp', (Join-Path $staging 'temp')
    )
    jpackage @packageArgs
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the Windows launcher.' }

    $stagedApp = Join-Path $outputFolder 'SmartCity360'
    $previous = Join-Path $staging 'previous'
    if (Test-Path -LiteralPath $application) {
        $existing = Get-Item -LiteralPath $application -Force
        $resolved = (Resolve-Path -LiteralPath $application).ProviderPath
        if ($resolved -ne $application -or $existing.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or
                -not (Test-Path -LiteralPath (Join-Path $application 'SmartCity360.exe') -PathType Leaf)) {
            throw "Refusing to replace unexpected folder: $application"
        }
        Move-Item -LiteralPath $application -Destination $previous
    }
    try {
        Move-Item -LiteralPath $stagedApp -Destination $application
    } catch {
        if (Test-Path -LiteralPath $previous) {
            Move-Item -LiteralPath $previous -Destination $application
        }
        throw
    }
    Write-Output (Join-Path $application 'SmartCity360.exe')
} finally {
    $stagingFolder = Get-Item -LiteralPath $staging -Force
    $backupNeedsRecovery = (Test-Path -LiteralPath (Join-Path $staging 'previous')) -and
            -not (Test-Path -LiteralPath $application)
    if (-not $backupNeedsRecovery -and (Resolve-Path -LiteralPath $staging).ProviderPath -eq $staging -and
            -not $stagingFolder.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
}
