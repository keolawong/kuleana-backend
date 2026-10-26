<#
.SYNOPSIS
    Kuleana Systems - Local Docker Deployment Script (PowerShell)
.DESCRIPTION
    Builds and deploys the Kuleana Systems ecosystem (auth-service, kule-admin UI)
    targeting the Neon dev branch.
.EXAMPLE
    .\deploy-local.ps1            # Full build & deploy (targets Neon dev branch)
    .\deploy-local.ps1 -NoBuild   # Spin up without recompiling
    .\deploy-local.ps1 -Down      # Stop and remove containers
    .\deploy-local.ps1 -Logs      # Stream container logs
    .\deploy-local.ps1 -Status    # Check health status
#>
[CmdletBinding()]
param (
    [switch]$Down,
    [switch]$Stop,
    [switch]$Logs,
    [switch]$Status,
    [switch]$NoBuild,
    [switch]$LocalDb,
    [string]$DbUrl = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$ComposeFile = Join-Path $ScriptDir "docker-compose.yml"
$FrontendDir = Join-Path (Split-Path -Parent $ScriptDir) "kuleana-frontend"
$EnvFile = Join-Path $ScriptDir ".env"

# Auto-load .env into environment
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
        }
    }
}

# --- TEARDOWN ---
if ($Down -or $Stop) {
    Write-Host "`n[Kuleana] Stopping and removing containers..." -ForegroundColor Yellow
    docker compose -f $ComposeFile down
    Write-Host "[Kuleana] All local containers stopped.`n" -ForegroundColor Green
    exit 0
}

# --- LOGS ---
if ($Logs) {
    Write-Host "`n[Kuleana] Streaming container logs (Ctrl+C to exit)..." -ForegroundColor Cyan
    docker compose -f $ComposeFile logs -f
    exit 0
}

# --- STATUS ---
if ($Status) {
    Write-Host "`n=== Kuleana Local Docker Status ===" -ForegroundColor Cyan
    docker compose -f $ComposeFile ps
    Write-Host "`nChecking health endpoints..." -ForegroundColor Gray
    try {
        $backendHealth = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -TimeoutSec 4 -ErrorAction SilentlyContinue
        Write-Host "  auth-service (8081):   UP ($($backendHealth.status)) [Database: $($backendHealth.components.db.details.database)]" -ForegroundColor Green
    } catch {
        Write-Host "  auth-service (8081):   OFFLINE / STARTING" -ForegroundColor Yellow
    }

    try {
        $assetsServiceHealth = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -TimeoutSec 4 -ErrorAction SilentlyContinue
        Write-Host "  assets-service (8082): UP ($($assetsServiceHealth.status)) [Database: $($assetsServiceHealth.components.db.details.database)]" -ForegroundColor Green
    } catch {
        Write-Host "  assets-service (8082): OFFLINE / STARTING" -ForegroundColor Yellow
    }

    try {
        $uiHealth = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
        Write-Host "  kule-admin UI (3000):  UP (HTTP $($uiHealth.StatusCode))" -ForegroundColor Green
    } catch {
        Write-Host "  kule-admin UI (3000):  OFFLINE / STARTING" -ForegroundColor Yellow
    }

    try {
        $assetsHealth = Invoke-WebRequest -Uri "http://localhost:3001" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
        Write-Host "  kule-assets UI (3001): UP (HTTP $($assetsHealth.StatusCode))" -ForegroundColor Green
    } catch {
        Write-Host "  kule-assets UI (3001): OFFLINE / STARTING" -ForegroundColor Yellow
    }
    Write-Host ""
    exit 0
}

# --- BUILD STEP ---
Write-Host "`n===============================================" -ForegroundColor Cyan
Write-Host "   Kuleana Systems - Local Docker Deployer    " -ForegroundColor Cyan
Write-Host "   Target: Neon PostgreSQL (dev branch)       " -ForegroundColor Cyan
Write-Host "===============================================`n" -ForegroundColor Cyan

if (-not $NoBuild) {
    Write-Host "[1/2] Building Backend Executable JARs (Java 25)..." -ForegroundColor Yellow
    & ".\gradlew.bat" :services:auth-service:bootJar :services:assets-service:bootJar -x test
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[Kuleana] Backend build failed! Aborting deployment."
        exit 1
    }
    Write-Host "       Backend JARs built successfully." -ForegroundColor Green

    Write-Host "`n[2/2] Building Frontend Production Bundles (Vite)..." -ForegroundColor Yellow
    if (Test-Path $FrontendDir) {
        $adminDir = Join-Path $FrontendDir "apps\kule-admin"
        if (Test-Path $adminDir) {
            Write-Host "       Building kule-admin bundle..." -ForegroundColor Gray
            Push-Location $adminDir
            try {
                npx vite build
                if ($LASTEXITCODE -ne 0) {
                    Write-Error "[Kuleana] Frontend build for kule-admin failed! Aborting deployment."
                    exit 1
                }
            } finally {
                Pop-Location
            }
        }

        $assetsDir = Join-Path $FrontendDir "apps\kule-assets"
        if (Test-Path $assetsDir) {
            Write-Host "       Building kule-assets bundle..." -ForegroundColor Gray
            Push-Location $assetsDir
            try {
                npx vite build
                if ($LASTEXITCODE -ne 0) {
                    Write-Error "[Kuleana] Frontend build for kule-assets failed! Aborting deployment."
                    exit 1
                }
            } finally {
                Pop-Location
            }
        }
        Write-Host "       Frontend bundles built successfully." -ForegroundColor Green
    }
} else {
    Write-Host "[Kuleana] Skipping build step (-NoBuild flag specified)." -ForegroundColor Gray
}

# --- DEPLOY STEP ---
Write-Host "`n[Deploy] Starting Docker Containers targeting Neon dev branch..." -ForegroundColor Yellow

if ($DbUrl) {
    $env:SPRING_DATASOURCE_URL = $DbUrl
    $env:SPRING_FLYWAY_URL = $DbUrl
    Write-Host "         Custom DB override: $DbUrl" -ForegroundColor Cyan
}

$composeArgs = @("-f", $ComposeFile)
if ($LocalDb) {
    $composeArgs += @("--profile", "local-db")
    Write-Host "         Local Postgres container profile enabled." -ForegroundColor Yellow
}
$composeArgs += @("up", "-d", "--build")

docker compose @composeArgs

if ($LASTEXITCODE -ne 0) {
    Write-Error "[Kuleana] Docker compose up failed!"
    exit 1
}

Write-Host "`n===============================================" -ForegroundColor Green
Write-Host "   Deployment Complete! All services running.   " -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "  • Kuleana Admin UI:      http://localhost:3000" -ForegroundColor Cyan
Write-Host "  • Kuleana Assets UI:     http://localhost:3001" -ForegroundColor Cyan
Write-Host "  • Auth Service API:      http://localhost:8081" -ForegroundColor Cyan
Write-Host "  • Assets Service API:    http://localhost:8082" -ForegroundColor Cyan
Write-Host "  • Database Target:       Neon dev branch ($($env:DB_HOST))" -ForegroundColor Cyan
Write-Host "  • Actuator Health:       http://localhost:8081/actuator/health" -ForegroundColor Cyan
Write-Host "`nCommands:" -ForegroundColor Gray
Write-Host "  .\deploy-local.ps1 -Logs    # View live logs"
Write-Host "  .\deploy-local.ps1 -Status  # Health checks"
Write-Host "  .\deploy-local.ps1 -Down    # Stop all containers`n"
