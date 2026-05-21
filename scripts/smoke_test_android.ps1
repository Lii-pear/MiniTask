param(
    [string]$PackageName = "com.example.minitask",
    [string]$MainActivity = ".MainActivity",
    [int]$ObserveSeconds = 20,
    [string]$ApkPath = "",
    [string]$TempRoot = "D:\temp\MiniTask",
    [string]$LogDir = "",
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[STEP] $Message" -ForegroundColor Cyan
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Gray
}

function Format-ExternalCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    $displayPath = if ($FilePath -match "\s") { "`"$FilePath`"" } else { $FilePath }
    $displayArgs = $Arguments | ForEach-Object {
        if ($_ -match "\s") { "`"$_`"" } else { $_ }
    }
    return (@($displayPath) + $displayArgs) -join " "
}

function Invoke-External {
    param(
        [string]$FilePath,
        [string[]]$Arguments = @(),
        [switch]$Passthrough
    )

    $displayCommand = Format-ExternalCommand -FilePath $FilePath -Arguments $Arguments
    if ($DryRun) {
        Write-Host "[DRYRUN] $displayCommand" -ForegroundColor Yellow
        return @()
    }

    if ($Passthrough) {
        $output = & $FilePath @Arguments
        $exitCode = $LASTEXITCODE
        if ($null -ne $exitCode -and $exitCode -ne 0) {
            throw "Command failed ($exitCode): $displayCommand"
        }
        return $output
    }

    & $FilePath @Arguments | Out-Null
    $exitCode = $LASTEXITCODE
    if ($null -ne $exitCode -and $exitCode -ne 0) {
        throw "Command failed ($exitCode): $displayCommand"
    }
}

function Ensure-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing command: $Name. Install it and add it to PATH."
    }
}

$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$gradlew = Join-Path $workspace "gradlew.bat"

if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $TempRoot "logs"
}

if (-not (Test-Path $TempRoot)) {
    New-Item -ItemType Directory -Path $TempRoot -Force | Out-Null
}
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}

$env:TEMP = $TempRoot
$env:TMP = $TempRoot
$env:GRADLE_USER_HOME = Join-Path $TempRoot "gradle-home"
$javaTmpOption = "-Djava.io.tmpdir=$TempRoot"
if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
    $env:JAVA_TOOL_OPTIONS = $javaTmpOption
} elseif ($env:JAVA_TOOL_OPTIONS -notlike "*$javaTmpOption*") {
    $env:JAVA_TOOL_OPTIONS = "$($env:JAVA_TOOL_OPTIONS) $javaTmpOption"
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = Join-Path $LogDir "smoke_$timestamp.log"

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $workspace "app\build\outputs\apk\release\app-release.apk"
}

Write-Step "Check dependency commands"
Write-Info "Temp root: $TempRoot"
Write-Info "Log file: $logFile"
if (-not $DryRun) {
    Ensure-Command "adb"
}
if (-not $SkipBuild -and -not (Test-Path $gradlew)) {
    throw "Missing gradlew.bat: $gradlew"
}

Write-Step "Check device connection"
$deviceState = if ($DryRun) {
    "device"
} else {
    $state = & adb get-state 2>$null
    if ($LASTEXITCODE -ne 0) { "" } else { $state }
}
if (-not $deviceState -or $deviceState.Trim() -ne "device") {
    throw "No Android device detected. Connect a device and enable USB debugging."
}
Write-Info "Device state: $($deviceState.Trim())"

if (-not $SkipBuild) {
    Write-Step "Build release APK"
    Invoke-External -FilePath $gradlew -Arguments @("assembleRelease")
}

if (-not (Test-Path $ApkPath) -and -not $DryRun) {
    throw "APK does not exist: $ApkPath"
}

if (-not $SkipInstall) {
    Write-Step "Install APK"
    Invoke-External -FilePath "adb" -Arguments @("install", "-r", $ApkPath)
}

Write-Step "Clear logs before launch"
Invoke-External -FilePath "adb" -Arguments @("logcat", "-c")
Invoke-External -FilePath "adb" -Arguments @("shell", "am", "force-stop", $PackageName)

Write-Step "Launch app"
$component = if ($MainActivity.StartsWith(".")) {
    "$PackageName/$PackageName$MainActivity"
} else {
    "$PackageName/$MainActivity"
}
Invoke-External -FilePath "adb" -Arguments @("shell", "am", "start", "-n", $component)

Write-Step "Observe for $ObserveSeconds seconds and capture logs"
if (-not $DryRun) {
    Start-Sleep -Seconds $ObserveSeconds
}
$fullLog = Invoke-External -FilePath "adb" -Arguments @("logcat", "-d") -Passthrough

if (-not $DryRun) {
    $fullLog | Out-File -FilePath $logFile -Encoding UTF8
}

$fatalHit = $false
if (-not $DryRun) {
    $joined = ($fullLog -join [Environment]::NewLine)
    $fatalHit = ($joined -match "FATAL EXCEPTION") -or
        ($joined -match "AndroidRuntime:\s+Process:\s+$([Regex]::Escape($PackageName))")
}

if ($fatalHit) {
    Write-Host "[FAIL] Possible crash detected. See log: $logFile" -ForegroundColor Red
    exit 1
}

Write-Host "[PASS] Smoke flow passed. No app-level crash detected. Log: $logFile" -ForegroundColor Green
