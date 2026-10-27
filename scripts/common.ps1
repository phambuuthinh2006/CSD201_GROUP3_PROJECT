# Shared native-command handling for Windows PowerShell 5.1 and PowerShell 7.
Set-StrictMode -Version Latest

function Invoke-CsdCommand {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [string[]]$CsdArguments = @(),
        [string]$LogFile = '',
        [switch]$Quiet
    )
    $null = Get-Command -Name $Executable -CommandType Application -ErrorAction Stop
    $csdPreviousPreference = $ErrorActionPreference
    try {
        # PS 5.1 represents native stderr as error records even for exit code 0.
        $ErrorActionPreference = 'Continue'
        & $Executable @CsdArguments 2>&1 | ForEach-Object {
            $csdLine = $_.ToString()
            if ($LogFile) {
                [IO.File]::AppendAllText($LogFile, $csdLine + [Environment]::NewLine, [Text.Encoding]::UTF8)
            }
            if (-not $Quiet) { Write-Host $csdLine }
        }
        $csdExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $csdPreviousPreference
    }
    if ($csdExitCode -ne 0) {
        if ($Quiet -and $LogFile -and (Test-Path -LiteralPath $LogFile)) {
            Get-Content -LiteralPath $LogFile -Encoding UTF8 | ForEach-Object { Write-Host $_ }
        }
        throw "Command failed (exit $csdExitCode): $Executable $($CsdArguments -join ' ')"
    }
}

function Get-CsdMaven {
    if ($env:CSD_MAVEN_BIN) {
        return (Get-Command -Name $env:CSD_MAVEN_BIN -CommandType Application -ErrorAction Stop).Source
    }
    foreach ($csdName in @('mvn.cmd', 'mvn')) {
        $csdFound = Get-Command -Name $csdName -CommandType Application -ErrorAction SilentlyContinue
        if ($csdFound) { return $csdFound.Source }
    }
    $csdCache = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.cache/csd-tools/apache-maven-3.9.9/bin/mvn'
    if (Test-Path -LiteralPath $csdCache) { return $csdCache }
    throw 'Maven not found. Install Maven, add its bin directory to PATH, or set CSD_MAVEN_BIN to mvn.cmd.'
}

function Get-CsdPython {
    foreach ($csdName in @('py', 'python', 'python3')) {
        $csdFound = Get-Command -Name $csdName -CommandType Application -ErrorAction SilentlyContinue
        if ($csdFound) {
            $csdPrefix = @('-X', 'utf8')
            if ($csdName -eq 'py') { $csdPrefix = @('-3', '-X', 'utf8') }
            return [PSCustomObject]@{ Executable = $csdFound.Source; Prefix = $csdPrefix }
        }
    }
    throw 'Python 3.9+ not found. Install Python and enable its launcher or add Python to PATH.'
}

function Invoke-CsdPython {
    param([Parameter(Mandatory = $true)][string[]]$CsdArguments, [string]$LogFile = '')
    $csdPython = Get-CsdPython
    $csdPythonArguments = @($csdPython.Prefix) + $CsdArguments
    Invoke-CsdCommand -Executable $csdPython.Executable -CsdArguments $csdPythonArguments -LogFile $LogFile
}
