<#
.SYNOPSIS
    Kuleana Systems - Local Docker Deployment Script (PowerShell)
.DESCRIPTION
    Builds and deploys the Kuleana Systems ecosystem (auth-service, kule-admin UI, local postgres)
    into local Docker containers.
.EXAMPLE
    .\deploy-local.ps1            # Full build & deploy
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
    [string]$NeonDbUrl = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$ComposeFile = Join-Path $ScriptDir "docker-compose.yml"
$FrontendDir = Join-Path (Split-Path -Parent $ScriptDir) "kuleana-frontend"

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
        $backendHealth = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
        Write-Host "  auth-service (8081):   UP ($($backendHealth.status))" -ForegroundColor Green
    } catch {
        Write-Host "  auth-service (8081):   OFFLINE / STARTING" -ForegroundColor Yellow
    }

    try {
        $uiHealth = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
        Write-Host "  kule-admin UI (3000):  UP (HTTP $($uiHealth.StatusCode))" -ForegroundColor Green
    } catch {
        Write-Host "  kule-admin UI (3000):  OFFLINE / STARTING" -ForegroundColor Yellow
    }
    Write-Host ""
    exit 0
}

# --- BUILD STEP ---
Write-Host "`n===============================================" -ForegroundColor Cyan
Write-Host "   Kuleana Systems - Local Docker Deployer    " -ForegroundColor Cyan
Write-Host "===============================================`n" -ForegroundColor Cyan

if (-not $NoBuild) {
    Write-Host "[1/3] Building Backend Executable JARs (Java 25)..." -ForegroundColor Yellow
    & ".\gradlew.bat" :services:auth-service:bootJar -x test
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[Kuleana] Backend build failed! Aborting deployment."
        exit 1
    }
    Write-Host "       Backend JAR built successfully." -ForegroundColor Green

    Write-Host "`n[2/3] Building Frontend Production Bundles (Vite)..." -ForegroundColor Yellow
    if (Test-Path $FrontendDir) {
        Push-Location $FrontendDir
        try {
            npx turbo run build
            if ($LASTEXITCODE -ne 0) {
                Write-Error "[Kuleana] Frontend build failed! Aborting deployment."
                exit 1
            }
        } finally {
            Pop-Location
        }
        Write-Host "       Frontend bundle built successfully." -ForegroundColor Green
    } else {
        Write-Warning "[Kuleana] Frontend directory not found at $FrontendDir, skipping frontend build."
    }
} else {
    Write-Host "[Kuleana] Skipping build step (-NoBuild flag specified)." -ForegroundColor Gray
}

# --- DEPLOY STEP ---
Write-Host "`n[3/3] Deploying Docker Containers..." -ForegroundColor Yellow

# Set DB_URL if provided
if ($NeonDbUrl) {
    $env:DB_URL = $NeonDbUrl
    Write-Host "       Using external database: $NeonDbUrl" -ForegroundColor Cyan
}

docker compose -f $ComposeFile up -d --build

if ($LASTEXITCODE -ne 0) {
    Write-Error "[Kuleana] Docker compose up failed!"
    exit 1
}

Write-Host "`n===============================================" -ForegroundColor Green
Write-Host "   Deployment Complete! All services running.   " -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "  • Kuleana Admin UI:      http://localhost:3000" -ForegroundColor Cyan
Write-Host "  • Auth Service API:      http://localhost:8081" -ForegroundColor Cyan
Write-Host "  • Actuator Health:       http://localhost:8081/actuator/health" -ForegroundColor Cyan
Write-Host "  • Local Postgres:        localhost:5432 (db: kuleanadb, user: kuleana_user)" -ForegroundColor Cyan
Write-Host "`nCommands:" -ForegroundColor Gray
Write-Host "  .\deploy-local.ps1 -Logs    # View live logs"
Write-Host "  .\deploy-local.ps1 -Status  # Health checks"
Write-Host "  .\deploy-local.ps1 -Down    # Stop all containers`n"
