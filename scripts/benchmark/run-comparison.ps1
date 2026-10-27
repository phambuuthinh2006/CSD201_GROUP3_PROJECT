# Run the same Java harness as Linux, without Bash/Linux utilities.
[CmdletBinding()]
param([string]$OutputDirectory = '')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../common.ps1')
$csdRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $csdRoot
try {
    $csdMaven = Get-CsdMaven
    $null = Get-Command java, javac -CommandType Application -ErrorAction Stop
    $null = Get-CsdPython
    $null = New-Item -ItemType Directory -Path 'results' -Force
    if (-not $OutputDirectory) {
        $OutputDirectory = Join-Path $csdRoot ('results/codec-comparison-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    } elseif (-not [IO.Path]::IsPathRooted($OutputDirectory)) {
        $OutputDirectory = Join-Path $csdRoot $OutputDirectory
    }
    if (Test-Path -LiteralPath $OutputDirectory) { throw "Refusing to overwrite an existing directory: $OutputDirectory" }
    $null = New-Item -ItemType Directory -Path $OutputDirectory
    $csdOutput = (Resolve-Path -LiteralPath $OutputDirectory).Path
    Invoke-CsdCommand -Executable $csdMaven -CsdArguments @('-q', '-DskipTests', 'compile')
    $null = New-Item -ItemType Directory -Path 'target/codec-comparison' -Force
    Invoke-CsdCommand -Executable 'javac' -CsdArguments @('--release', '8', '-encoding', 'UTF-8', '-cp', 'target/classes', '-d', 'target/codec-comparison', 'scripts/benchmark/CodecComparison.java')

    $csdEnvironment = Join-Path $csdOutput 'environment.txt'
    $csdInfo = @(
        (Get-Date -Format 'o'),
        ('PowerShell=' + $PSVersionTable.PSVersion),
        ('OS=' + [Environment]::OSVersion),
        ('Machine=' + [Environment]::MachineName),
        ('Logical processors=' + [Environment]::ProcessorCount),
        'JVM flags=-Xms512m -Xmx1g -XX:+UseG1GC'
    )
    if ($env:OS -eq 'Windows_NT') {
        try {
            $csdInfo += (Get-CimInstance Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors | Format-List | Out-String)
            $csdInfo += ('RAM bytes=' + (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory)
        } catch { $csdInfo += 'Hardware detail unavailable via CIM; add CPU/RAM manually.' }
    }
    [IO.File]::WriteAllLines($csdEnvironment, [string[]]$csdInfo, [Text.Encoding]::UTF8)
    Invoke-CsdCommand -Executable 'java' -CsdArguments @('-version') -LogFile $csdEnvironment -Quiet
    if (Get-Command git -CommandType Application -ErrorAction SilentlyContinue) {
        Invoke-CsdCommand -Executable 'git' -CsdArguments @('log', '-1', '--oneline') -LogFile $csdEnvironment -Quiet
        Invoke-CsdCommand -Executable 'git' -CsdArguments @('status', '--short') -LogFile $csdEnvironment -Quiet
    }
    $csdSources = @(
        Get-ChildItem 'src/main/java/vn/csd/compression/codec/*.java'
        Get-ChildItem 'src/main/java/vn/csd/compression/io/*.java'
        Get-Item 'scripts/benchmark/CodecComparison.java'
        Get-Item 'scripts/benchmark/run-comparison.ps1'
        Get-Item 'scripts/common.ps1'
    )
    $csdHashes = foreach ($csdSource in $csdSources) {
        (Get-FileHash -LiteralPath $csdSource.FullName -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + $csdSource.FullName
    }
    [IO.File]::WriteAllLines((Join-Path $csdOutput 'source-sha256.txt'), [string[]]$csdHashes, [Text.Encoding]::UTF8)
    Copy-Item 'scripts/benchmark/CodecComparison.java', 'scripts/benchmark/run-comparison.ps1', 'scripts/common.ps1' -Destination $csdOutput
    $csdClasspath = 'target/classes' + [IO.Path]::PathSeparator + 'target/codec-comparison'
    foreach ($csdFork in 1..3) {
        Invoke-CsdCommand -Executable 'java' -CsdArguments @('-Xms512m', '-Xmx1g', '-XX:+UseG1GC', '-Dfile.encoding=UTF-8', '-cp', $csdClasspath, 'CodecComparison', $csdOutput, [string]$csdFork) -LogFile (Join-Path $csdOutput "fork-$csdFork.log")
    }
    Invoke-CsdPython -CsdArguments @('scripts/benchmark/summarize-comparison.py', $csdOutput)
    foreach ($csdMiB in 1, 4) {
        Write-Host "`nSPEED COMPARISON - $csdMiB MiB"
        Invoke-CsdPython -CsdArguments @('scripts/report-terminal.py', 'benchmark', (Join-Path $csdOutput 'summary.csv'), '--mib', [string]$csdMiB)
    }
    Write-Host "`nResults: $csdOutput"
} finally { Pop-Location }
