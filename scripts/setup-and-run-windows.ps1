$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ComposeUsesPlugin = $true
$Runtime = ""
$ComposeEngine = ""
$EnvFile = Join-Path $RootDir ".env.local"
$HookTemplate = Join-Path $RootDir ".githooks/pre-commit"
$GitHookPath = Join-Path $RootDir ".git/hooks/pre-commit"

function Write-Log {
  param([string]$Message)
  Write-Host "`n[setup-windows] $Message"
}

function Ensure-Command {
  param(
    [string]$CommandName,
    [string]$WingetId,
    [string]$ManualHint
  )

  if (Get-Command $CommandName -ErrorAction SilentlyContinue) {
    return
  }

  Write-Log "$CommandName not found. Attempting install with winget..."
  if (Get-Command winget -ErrorAction SilentlyContinue) {
    winget install --id $WingetId --exact --accept-package-agreements --accept-source-agreements
  } else {
    throw "[setup-windows] winget not available. $ManualHint"
  }

  if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
    throw "[setup-windows] Unable to find $CommandName after install. Open a new PowerShell session and retry."
  }
}

function New-RandomSecret {
  return ((1..48) | ForEach-Object { "{0:x}" -f (Get-Random -Minimum 0 -Maximum 16) }) -join ""
}

function Load-EnvFile {
  if (-not (Test-Path $EnvFile)) {
    return @{}
  }
  $values = @{}
  $lines = Get-Content $EnvFile -ErrorAction SilentlyContinue
  foreach ($line in $lines) {
    if (-not $line -or $line.Trim().StartsWith("#")) {
      continue
    }
    $parts = $line -split "=", 2
    if ($parts.Count -eq 2) {
      $values[$parts[0].Trim()] = $parts[1].Trim()
    }
  }
  return $values
}

function Save-EnvFile {
  param(
    [string]$DbUser,
    [string]$DbPassword,
    [string]$DbName
  )
  @(
    "CIVICLENS_DB_USER=$DbUser"
    "CIVICLENS_DB_PASSWORD=$DbPassword"
    "CIVICLENS_DB_NAME=$DbName"
  ) | Set-Content -Path $EnvFile -Encoding ascii
}

function Set-DbPassword {
  param([string]$Password)
  $env:CIVICLENS_DB_PASSWORD = $Password
  Save-EnvFile -DbUser $env:CIVICLENS_DB_USER -DbPassword $env:CIVICLENS_DB_PASSWORD -DbName $env:CIVICLENS_DB_NAME
  $env:SPRING_DATASOURCE_PASSWORD = $env:CIVICLENS_DB_PASSWORD
}

function Ensure-DbSecrets {
  $values = Load-EnvFile
  $dbUser = if ($values.ContainsKey("CIVICLENS_DB_USER")) { $values["CIVICLENS_DB_USER"] } else { "civiclens" }
  $dbName = if ($values.ContainsKey("CIVICLENS_DB_NAME")) { $values["CIVICLENS_DB_NAME"] } else { "civiclens" }
  $dbPassword = if ($values.ContainsKey("CIVICLENS_DB_PASSWORD")) { $values["CIVICLENS_DB_PASSWORD"] } else { "" }

  if (-not $dbPassword) {
    $dbPassword = New-RandomSecret
    Save-EnvFile -DbUser $dbUser -DbPassword $dbPassword -DbName $dbName
    Write-Log "Created local DB credentials at $EnvFile (gitignored)."
  }

  $env:CIVICLENS_DB_USER = $dbUser
  $env:CIVICLENS_DB_PASSWORD = $dbPassword
  $env:CIVICLENS_DB_NAME = $dbName
  $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:55432/$dbName"
  $env:SPRING_DATASOURCE_USERNAME = $dbUser
  $env:SPRING_DATASOURCE_PASSWORD = $dbPassword
}

function Test-DbLogin {
  param([string]$Password)
  if ($script:ComposeEngine -eq "docker") {
    docker exec -e PGPASSWORD=$Password -e PGCONNECT_TIMEOUT=2 civiclens-postgres psql -h 127.0.0.1 -p 5432 -U $env:CIVICLENS_DB_USER -d $env:CIVICLENS_DB_NAME -c "select 1" | Out-Null
    return ($LASTEXITCODE -eq 0)
  }
  podman exec -e PGPASSWORD=$Password -e PGCONNECT_TIMEOUT=2 civiclens-postgres psql -h 127.0.0.1 -p 5432 -U $env:CIVICLENS_DB_USER -d $env:CIVICLENS_DB_NAME -c "select 1" | Out-Null
  return ($LASTEXITCODE -eq 0)
}

function Test-DbLoginWithRetry {
  param(
    [string]$Password,
    [int]$Attempts = 20
  )

  for ($i = 0; $i -lt $Attempts; $i++) {
    if (Test-DbLogin -Password $Password) {
      return $true
    }
    Start-Sleep -Seconds 1
  }
  return $false
}

function Sync-DbRolePassword {
  $escapedPassword = $env:CIVICLENS_DB_PASSWORD.Replace("'", "''")
  $sql = "ALTER ROLE `"$($env:CIVICLENS_DB_USER)`" WITH PASSWORD '$escapedPassword';"
  if ($script:ComposeEngine -eq "docker") {
    docker exec civiclens-postgres psql -h 127.0.0.1 -p 5432 -U $env:CIVICLENS_DB_USER -d postgres -v ON_ERROR_STOP=1 -c $sql | Out-Null
    return ($LASTEXITCODE -eq 0)
  }
  podman exec civiclens-postgres psql -h 127.0.0.1 -p 5432 -U $env:CIVICLENS_DB_USER -d postgres -v ON_ERROR_STOP=1 -c $sql | Out-Null
  return ($LASTEXITCODE -eq 0)
}

function Reconcile-DbCredentials {
  Write-Log "Validating database credentials..."
  if (Test-DbLoginWithRetry -Password $env:CIVICLENS_DB_PASSWORD -Attempts 30) {
    if (-not (Sync-DbRolePassword)) {
      throw "[setup-windows] Unable to synchronize database role password."
    }
    Write-Log "Database credentials are valid."
    return
  }

  if ($env:CIVICLENS_DB_PASSWORD -ne "civiclens" -and (Test-DbLoginWithRetry -Password "civiclens" -Attempts 10)) {
    Write-Log "Detected existing DB volume with legacy password; updating local .env.local."
    Set-DbPassword -Password "civiclens"
    if (-not (Sync-DbRolePassword)) {
      throw "[setup-windows] Unable to synchronize database role password after legacy fallback."
    }
    return
  }

  Write-Log "Unable to authenticate with existing DB data. Recreating Postgres volume..."
  Push-Location "$RootDir/infra"
  Invoke-Compose -Args @("-p", "civiclens", "down", "-v", "--remove-orphans")
  Invoke-Compose -Args @("-p", "civiclens", "up", "-d")
  Pop-Location
  if (-not (Test-DbLoginWithRetry -Password $env:CIVICLENS_DB_PASSWORD -Attempts 40) -or -not (Sync-DbRolePassword)) {
    throw "[setup-windows] Postgres credential reconciliation failed."
  }
}

function Ensure-GitleaksInstalled {
  if (Get-Command gitleaks -ErrorAction SilentlyContinue) {
    return
  }
  Ensure-Command -CommandName "gitleaks" -WingetId "Gitleaks.Gitleaks" -ManualHint "Install gitleaks manually from https://github.com/gitleaks/gitleaks"
}

function Install-PreCommitHook {
  if (-not (Test-Path (Join-Path $RootDir ".git/hooks"))) {
    Write-Log ".git/hooks not found. Skipping hook installation."
    return
  }
  if (-not (Test-Path $HookTemplate)) {
    Write-Log "Hook template not found at $HookTemplate"
    return
  }
  Copy-Item -Path $HookTemplate -Destination $GitHookPath -Force
  Write-Log "Installed pre-commit hook (gitleaks secret scan)."
}

function Ensure-ToolInstalled {
  param(
    [string]$Choice
  )
  switch ($Choice) {
    "docker" {
      Ensure-Command -CommandName "docker" -WingetId "Docker.DockerDesktop" -ManualHint "Install Docker Desktop."
    }
    "rancher" {
      if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        if (Get-Command winget -ErrorAction SilentlyContinue) {
          winget install --id "SUSE.RancherDesktop" --exact --accept-package-agreements --accept-source-agreements
        } else {
          throw "[setup-windows] winget not available. Install Rancher Desktop manually."
        }
      }
    }
    "podman" {
      Ensure-Command -CommandName "podman" -WingetId "RedHat.Podman" -ManualHint "Install Podman."
    }
    "colima" {
      throw "[setup-windows] Colima is not supported from native PowerShell. Use WSL2 Linux if you want Colima."
    }
    default {
      throw "[setup-windows] Unsupported runtime choice: $Choice"
    }
  }
}

function Prompt-RuntimeChoice {
  Write-Host "`n[setup-windows] No supported runtime detected."
  Write-Host "[setup-windows] Choose a runtime to install:"
  Write-Host "  1) Docker Desktop"
  Write-Host "  2) Rancher Desktop"
  Write-Host "  3) Podman"
  Write-Host "  4) Colima (WSL2 only)"
  $selection = Read-Host "[setup-windows] Enter choice [1-4] (default 1)"

  switch ($selection) {
    "2" { return "rancher" }
    "3" { return "podman" }
    "4" { return "colima" }
    default { return "docker" }
  }
}

function Ensure-DockerRuntimeReady {
  Write-Log "Ensuring Docker-compatible runtime is running..."
  $dockerReady = $false
  try {
    docker info | Out-Null
    $dockerReady = $true
  } catch {
    $dockerReady = $false
  }

  if (-not $dockerReady) {
    $dockerDesktopExe = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    $rancherDesktopExe = "C:\Program Files\Rancher Desktop\Rancher Desktop.exe"

    if (Test-Path $dockerDesktopExe) {
      Start-Process -FilePath $dockerDesktopExe | Out-Null
    } elseif (Test-Path $rancherDesktopExe) {
      Start-Process -FilePath $rancherDesktopExe | Out-Null
    } else {
      throw "[setup-windows] Docker-compatible engine is not running and could not be started automatically."
    }

    $maxAttempts = 60
    for ($i = 0; $i -lt $maxAttempts; $i++) {
      Start-Sleep -Seconds 2
      try {
        docker info | Out-Null
        $dockerReady = $true
        break
      } catch {
        $dockerReady = $false
      }
    }
  }

  if (-not $dockerReady) {
    throw "[setup-windows] Docker-compatible engine did not become ready in time."
  }
}

function Ensure-PodmanRuntimeReady {
  Write-Log "Ensuring Podman machine is running..."
  $running = $false
  try {
    $machineList = podman machine list
    if ($machineList -match "Currently running") {
      $running = $true
    }
  } catch {
    $running = $false
  }

  if (-not $running) {
    try {
      podman machine init | Out-Null
    } catch {
      # ignore if already initialized
    }
    podman machine start | Out-Null
    $running = $true
  }

  if (-not $running) {
    throw "[setup-windows] Podman machine did not start."
  }
}

function Resolve-Runtime {
  $hasDocker = [bool](Get-Command docker -ErrorAction SilentlyContinue)
  $hasPodman = [bool](Get-Command podman -ErrorAction SilentlyContinue)
  $hasColima = [bool](Get-Command colima -ErrorAction SilentlyContinue)
  $hasRancherApp = Test-Path "C:\Program Files\Rancher Desktop\Rancher Desktop.exe"

  if ($hasColima) {
    $script:Runtime = "colima"
    $script:ComposeEngine = "docker"
    Ensure-DockerRuntimeReady
    return
  }

  if ($hasDocker) {
    $script:Runtime = if ($hasRancherApp) { "docker-or-rancher" } else { "docker" }
    $script:ComposeEngine = "docker"
    Ensure-DockerRuntimeReady
    return
  }

  if ($hasPodman) {
    $script:Runtime = "podman"
    $script:ComposeEngine = "podman"
    Ensure-PodmanRuntimeReady
    return
  }

  $choice = Prompt-RuntimeChoice
  Ensure-ToolInstalled -Choice $choice

  $hasDocker = [bool](Get-Command docker -ErrorAction SilentlyContinue)
  $hasPodman = [bool](Get-Command podman -ErrorAction SilentlyContinue)

  if ($choice -eq "podman" -and $hasPodman) {
    $script:Runtime = "podman"
    $script:ComposeEngine = "podman"
    Ensure-PodmanRuntimeReady
    return
  }

  if (($choice -eq "docker" -or $choice -eq "rancher") -and $hasDocker) {
    $script:Runtime = if ($choice -eq "rancher") { "rancher" } else { "docker" }
    $script:ComposeEngine = "docker"
    Ensure-DockerRuntimeReady
    return
  }

  throw "[setup-windows] Runtime installation completed but runtime command was not found. Open a new PowerShell session and retry."
}

function Resolve-ComposeMode {
  if ($script:ComposeEngine -eq "docker") {
    try {
      docker compose version | Out-Null
      $script:ComposeUsesPlugin = $true
    } catch {
      $script:ComposeUsesPlugin = $false
    }
    return
  }

  # podman path
  try {
    podman compose version | Out-Null
    $script:ComposeUsesPlugin = $true
  } catch {
    $script:ComposeUsesPlugin = $false
  }
}

function Invoke-Compose {
  param(
    [string[]]$Args
  )
  if ($script:ComposeEngine -eq "docker") {
    if ($script:ComposeUsesPlugin) {
      docker compose @Args
    } else {
      docker-compose @Args
    }
    return
  }

  if ($script:ComposeUsesPlugin) {
    podman compose @Args
  } else {
    podman-compose @Args
  }
}

function Stop-ConflictingContainers {
  Write-Log "Checking for container conflicts on port 55432..."
  if ($script:ComposeEngine -eq "docker") {
    $conflictingContainerIds = docker ps --filter "publish=55432" --format "{{.ID}}"
    if ($conflictingContainerIds) {
      foreach ($id in $conflictingContainerIds) {
        if ($id) {
          docker stop $id | Out-Null
        }
      }
    }
    return
  }

  $conflictingContainerIds = podman ps --filter "publish=55432" --format "{{.ID}}"
  if ($conflictingContainerIds) {
    foreach ($id in $conflictingContainerIds) {
      if ($id) {
        podman stop $id | Out-Null
      }
    }
  }
}

function Stop-HostPortConflicts {
  Write-Log "Checking for host process conflicts on ports 8080, 5173, and 55432..."
  $ports = @(8080, 5173, 55432)
  foreach ($port in $ports) {
    $pids = @()
    try {
      $pids = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction Stop |
        Select-Object -ExpandProperty OwningProcess -Unique
    } catch {
      # Fallback for environments where Get-NetTCPConnection isn't available.
      $netstatMatches = netstat -ano -p tcp | Select-String ":$port\s+.*LISTENING\s+(\d+)$"
      foreach ($match in $netstatMatches) {
        $pid = [int]($match.Matches[0].Groups[1].Value)
        if ($pid -gt 0) {
          $pids += $pid
        }
      }
      $pids = $pids | Select-Object -Unique
    }

    foreach ($pid in $pids) {
      if (-not $pid -or $pid -eq $PID) {
        continue
      }
      Write-Log "Stopping process $pid listening on port $port..."
      try {
        Stop-Process -Id $pid -Force -ErrorAction Stop
      } catch {
        Write-Warning "[setup-windows] Could not stop PID $pid on port $port. Try running PowerShell as Administrator."
      }
    }
  }
}

function Wait-HttpReady {
  param(
    [string]$Url,
    [string]$Label,
    [int]$TimeoutSeconds = 120
  )
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 | Out-Null
      Write-Log "$Label is ready at $Url"
      return
    } catch {
      Start-Sleep -Seconds 1
    }
  }
  throw "[setup-windows] Timed out waiting for $Label at $Url"
}

function Open-FrontendInBrowser {
  $frontendUrl = "http://localhost:5173"
  # Best effort: if Cursor CLI is available, try opening there first.
  if (Get-Command cursor -ErrorAction SilentlyContinue) {
    try {
      cursor $frontendUrl | Out-Null
      Write-Log "Opened frontend via Cursor."
      return
    } catch {
      # fallback below
    }
  }
  Start-Process $frontendUrl | Out-Null
  Write-Log "Opened frontend in your default browser."
}

function Show-SuccessBanner {
  Write-Host ""
  Write-Host "==========================================================="
  Write-Host "  CivicLens is running!"
  Write-Host "  Frontend: http://localhost:5173"
  Write-Host "  Backend:  http://localhost:8080"
  Write-Host "  Swagger:  http://localhost:8080/swagger-ui.html"
  Write-Host "==========================================================="
  Write-Host ""
}

Write-Log "Checking prerequisites..."
Ensure-Command -CommandName "node" -WingetId "OpenJS.NodeJS.LTS" -ManualHint "Install Node.js LTS from https://nodejs.org/"
Ensure-Command -CommandName "java" -WingetId "EclipseAdoptium.Temurin.17.JDK" -ManualHint "Install Java 17 JDK."
Ensure-Command -CommandName "mvn" -WingetId "Apache.Maven" -ManualHint "Install Apache Maven."
Ensure-GitleaksInstalled
Install-PreCommitHook
Ensure-DbSecrets
Resolve-Runtime
Resolve-ComposeMode
Stop-ConflictingContainers
Stop-HostPortConflicts

Write-Log "Using runtime: $Runtime"

Write-Log "Installing frontend dependencies..."
Push-Location "$RootDir/frontend"
npm install
Pop-Location

Write-Log "Pre-fetching backend Maven dependencies..."
Push-Location "$RootDir/backend"
mvn -DskipTests dependency:go-offline | Out-Null
Pop-Location

Write-Log "Starting Postgres via Docker Compose..."
Push-Location "$RootDir/infra"
Invoke-Compose -Args @("-p", "civiclens", "down", "--remove-orphans")
Invoke-Compose -Args @("-p", "civiclens", "up", "-d")
Pop-Location
Reconcile-DbCredentials

Write-Log "Starting backend API in background..."
$backendJob = Start-Job -Name "CivicLensBackend" -ScriptBlock {
  param($RepoRoot, $DbUser, $DbPassword, $DbName)
  Set-Location "$RepoRoot/backend"
  $env:CIVICLENS_DB_USER = $DbUser
  $env:CIVICLENS_DB_PASSWORD = $DbPassword
  $env:CIVICLENS_DB_NAME = $DbName
  $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:55432/$DbName"
  $env:SPRING_DATASOURCE_USERNAME = $DbUser
  $env:SPRING_DATASOURCE_PASSWORD = $DbPassword
  mvn spring-boot:run
} -ArgumentList $RootDir, $env:CIVICLENS_DB_USER, $env:CIVICLENS_DB_PASSWORD, $env:CIVICLENS_DB_NAME

$frontendJob = $null
try {
  Write-Log "Starting frontend on http://localhost:5173 in background..."
  $frontendJob = Start-Job -Name "CivicLensFrontend" -ScriptBlock {
    param($RepoRoot)
    Set-Location "$RepoRoot/frontend"
    npm run dev
  } -ArgumentList $RootDir

  Wait-HttpReady -Url "http://localhost:8080/api/health" -Label "Backend API" -TimeoutSeconds 120
  Wait-HttpReady -Url "http://localhost:5173" -Label "Frontend" -TimeoutSeconds 120
  Show-SuccessBanner
  Open-FrontendInBrowser

  while ($true) {
    Receive-Job -Job $backendJob -Keep | Out-Host
    if ($frontendJob) {
      Receive-Job -Job $frontendJob -Keep | Out-Host
    }
    if ($frontendJob -and $frontendJob.State -ne "Running") {
      break
    }
    Start-Sleep -Seconds 2
  }
} finally {
  if ($backendJob -and $backendJob.State -eq "Running") {
    Write-Log "Stopping backend background job..."
    Stop-Job $backendJob | Out-Null
  }
  if ($backendJob) {
    Remove-Job $backendJob -Force | Out-Null
  }
  if ($frontendJob -and $frontendJob.State -eq "Running") {
    Write-Log "Stopping frontend background job..."
    Stop-Job $frontendJob | Out-Null
  }
  if ($frontendJob) {
    Remove-Job $frontendJob -Force | Out-Null
  }
}
