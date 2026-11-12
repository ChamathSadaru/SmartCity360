$ErrorActionPreference = 'Stop'

$mainOutput = Join-Path $PSScriptRoot 'build/classes'
$testOutput = Join-Path $PSScriptRoot 'build/test-classes'
$libraryOutput = Join-Path $PSScriptRoot 'build/lib'
New-Item -ItemType Directory -Path $mainOutput, $testOutput, $libraryOutput -Force | Out-Null

$dependencies = @{
    'org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar' = 'F8DCE420A6016278497F4F0560F65602B9DB858DE141B541CF91A4644EBE4639'
    'net/java/dev/jna/jna/5.19.1/jna-5.19.1.jar' = '4FB141DD8EF6B0585FFCEEA4BC49602FBC6312FA977E2C488794EA3E6AAFECAE'
    'net/java/dev/jna/jna-platform/5.19.1/jna-platform-5.19.1.jar' = '3B3864F5B449E9C3C24B16861524B622B086563F44E0CD8384C8EFC5A6052F82'
}
foreach ($dependency in $dependencies.GetEnumerator()) {
    $jar = Join-Path $libraryOutput (Split-Path $dependency.Key -Leaf)
    if ((Test-Path -LiteralPath $jar) -and (Get-FileHash -LiteralPath $jar).Hash -eq $dependency.Value) {
        continue
    }
    $download = "$jar.$PID.download"
    try {
        Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/$($dependency.Key)" -OutFile $download -TimeoutSec 90
        if ((Get-FileHash -LiteralPath $download).Hash -ne $dependency.Value) {
            throw "Checksum mismatch for $($dependency.Key)."
        }
        Move-Item -LiteralPath $download -Destination $jar -Force
    } finally {
        if (Test-Path -LiteralPath $download) { Remove-Item -LiteralPath $download }
    }
}
$libraries = Join-Path $libraryOutput '*'

$mainSources = (Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/main/java') -Recurse -Filter '*.java' -File).FullName
$testSources = (Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/test/java') -Recurse -Filter '*.java' -File).FullName

javac --release 17 -encoding UTF-8 -Xlint:all -cp $libraries -d $mainOutput $mainSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$resources = Join-Path $PSScriptRoot 'src/main/resources'
Get-ChildItem -LiteralPath $resources | Copy-Item -Destination $mainOutput -Recurse -Force

$ideaOutput = Join-Path $PSScriptRoot 'build/idea-out/production/main'
New-Item -ItemType Directory -Path $ideaOutput -Force | Out-Null
Copy-Item -Path "$mainOutput\*" -Destination $ideaOutput -Recurse -Force

javac --release 17 -encoding UTF-8 -Xlint:all -cp "$mainOutput;$libraries" -d $testOutput $testSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java -cp "$mainOutput;$testOutput;$libraries" com.smartcity360.BackendTests
exit $LASTEXITCODE
