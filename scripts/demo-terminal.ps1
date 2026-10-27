# Windows PowerShell 5.1 / PowerShell 7 entry point. Default: test and measure all codecs.
[CmdletBinding()]
param([ValidateSet('all', 'tv5')][string]$Mode = 'all')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
$csdRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$csdTranscriptStarted = $false
$csdOldOutputEncoding = [Console]::OutputEncoding
$csdFailed = $false
Push-Location $csdRoot
try {
    [Console]::OutputEncoding = New-Object Text.UTF8Encoding $false
    $csdMaven = Get-CsdMaven
    $null = Get-Command java, javac -CommandType Application -ErrorAction Stop
    Invoke-CsdPython -CsdArguments @('--version')
    $csdRunDir = Join-Path $csdRoot ('results/video-windows-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    $null = New-Item -ItemType Directory -Path $csdRunDir
    $null = Start-Transcript -Path (Join-Path $csdRunDir 'terminal.log')
    $csdTranscriptStarted = $true
    Write-Host "CSD201 TERMINAL TEST AND SPEED DEMO - mode=$Mode"
    Write-Host "Results: $csdRunDir"
    Invoke-CsdCommand -Executable 'java' -CsdArguments @('-version')
    Invoke-CsdCommand -Executable $csdMaven -CsdArguments @('-version')
    $csdMavenArguments = @('-B', '-Dstyle.color=never', '-Dfile.encoding=UTF-8')
    if ($Mode -eq 'tv5') { $csdMavenArguments += '-Dtest=LZ4CodecTest,LZ4Rq3RunnerTest' }
    $csdMavenArguments += @('clean', 'verify')
    Invoke-CsdCommand -Executable $csdMaven -CsdArguments $csdMavenArguments
    # Inspect XML too: shared pom may ignore test failures.
    Invoke-CsdPython -CsdArguments @('scripts/report-terminal.py', 'tests', '--details')
    Copy-Item 'target/surefire-reports/TEST-*.xml' -Destination $csdRunDir
    if ($Mode -eq 'all') {
        Write-Host "`nGENERATOR C1/C2/C3 TESTS"
        $csdTestClasspath = 'target/classes' + [IO.Path]::PathSeparator + 'target/test-classes'
        $csdGeneratorLog = Join-Path $csdRunDir 'generator.log'
        Invoke-CsdCommand -Executable 'java' -CsdArguments @('-Xmx1g', '-Dfile.encoding=UTF-8', '-cp', $csdTestClasspath, 'vn.csd.compression.DataGeneratorTest') -LogFile $csdGeneratorLog -Quiet
        Get-Content -LiteralPath $csdGeneratorLog -Encoding UTF8 | Select-String -Pattern '---|Info|size 0|size 1|cung seed|khac seed|Tu vung dung|C2 sinh|C3 sinh|CHUC MUNG|\[FAIL\]' | ForEach-Object { Write-Host $_.Line }
        Write-Host "`nMEASURING ALL CODECS: RLE / Huffman / LZW / LZ4-12 / LZ4-16"
        & (Join-Path $PSScriptRoot 'benchmark/run-comparison.ps1') -OutputDirectory (Join-Path $csdRunDir 'comparison')
        Write-Host '840 measured rounds + 600 warm-up rounds, SHA-256 verified. Exploratory benchmark, not complete RQ3.'
    } else {
        $csdDemoCsv = Join-Path $csdRunDir 'lz4-demo.csv'
        Write-Host ((Get-FileHash 'README.md' -Algorithm SHA256).Hash.ToLowerInvariant() + '  README.md')
        Invoke-CsdCommand -Executable 'java' -CsdArguments @('-Xmx1g', '-Dfile.encoding=UTF-8', '-cp', 'target/classes', 'vn.csd.compression.metrics.LZ4Rq3Runner', $csdDemoCsv, '1', '2', 'README.md')
        Invoke-CsdPython -CsdArguments @('scripts/report-terminal.py', 'csv', $csdDemoCsv)
        Invoke-CsdPython -CsdArguments @('scripts/report-terminal.py', 'csv', $csdDemoCsv, '--speed')
    }
    Write-Host "`nCompleted. Log/XML/CSV: $csdRunDir"
} catch {
    $csdFailed = $true
    Write-Host ("FAILED: " + $_.Exception.Message) -ForegroundColor Red
} finally {
    if ($csdTranscriptStarted) { $null = Stop-Transcript }
    [Console]::OutputEncoding = $csdOldOutputEncoding
    Pop-Location
}
if ($csdFailed) { exit 1 }
