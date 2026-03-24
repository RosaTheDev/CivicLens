#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COLIMA_PROFILE="civiclens"
COLIMA_CONTEXT="colima-${COLIMA_PROFILE}"
RUNTIME=""
COMPOSE_TOOL=""
ENV_FILE="$ROOT_DIR/.env.local"
HOOK_TEMPLATE="$ROOT_DIR/.githooks/pre-commit"
GIT_HOOK_PATH="$ROOT_DIR/.git/hooks/pre-commit"

log() {
  printf "\n[setup-mac] %s\n" "$1"
}

require_cmd() {
  local cmd="$1"
  local install_hint="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[setup-mac] Missing required command: $cmd"
    echo "[setup-mac] $install_hint"
    exit 1
  fi
}

is_installed() {
  local cmd="$1"
  command -v "$cmd" >/dev/null 2>&1
}

random_secret() {
  if is_installed openssl; then
    openssl rand -hex 24
    return
  fi
  date +%s%N | shasum | awk '{print $1}'
}

load_env_file() {
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  fi
}

write_env_file() {
  umask 077
  cat > "$ENV_FILE" <<EOF
CIVICLENS_DB_USER=${CIVICLENS_DB_USER}
CIVICLENS_DB_PASSWORD=${CIVICLENS_DB_PASSWORD}
CIVICLENS_DB_NAME=${CIVICLENS_DB_NAME}
EOF
}

ensure_db_secrets() {
  load_env_file
  CIVICLENS_DB_USER="${CIVICLENS_DB_USER:-civiclens}"
  CIVICLENS_DB_NAME="${CIVICLENS_DB_NAME:-civiclens}"
  if [[ -z "${CIVICLENS_DB_PASSWORD:-}" ]]; then
    CIVICLENS_DB_PASSWORD="$(random_secret)"
    write_env_file
    log "Created local DB credentials at $ENV_FILE (gitignored)."
  fi
  export CIVICLENS_DB_USER CIVICLENS_DB_PASSWORD CIVICLENS_DB_NAME
  export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:55432/${CIVICLENS_DB_NAME}"
  export SPRING_DATASOURCE_USERNAME="${CIVICLENS_DB_USER}"
  export SPRING_DATASOURCE_PASSWORD="${CIVICLENS_DB_PASSWORD}"
}

ensure_gitleaks_installed() {
  if is_installed gitleaks; then
    return
  fi
  if is_installed brew; then
    log "Installing gitleaks for secret scanning..."
    brew install gitleaks
    return
  fi
  echo "[setup-mac] gitleaks is required for pre-commit secret scanning."
  echo "[setup-mac] Install Homebrew and run: brew install gitleaks"
  exit 1
}

install_pre_commit_hook() {
  if [[ ! -d "$ROOT_DIR/.git/hooks" ]]; then
    echo "[setup-mac] .git/hooks not found. Skipping hook installation."
    return
  fi
  if [[ ! -f "$HOOK_TEMPLATE" ]]; then
    echo "[setup-mac] Hook template missing at $HOOK_TEMPLATE"
    return
  fi
  cp "$HOOK_TEMPLATE" "$GIT_HOOK_PATH"
  chmod +x "$GIT_HOOK_PATH"
  log "Installed pre-commit hook (gitleaks secret scan)."
}

install_runtime() {
  local choice="$1"
  if ! is_installed brew; then
    echo "[setup-mac] Homebrew is required to auto-install runtimes. Install it from https://brew.sh and retry."
    exit 1
  fi
  case "$choice" in
    colima)
      log "Installing Colima + Docker CLI..."
      brew install colima docker docker-compose
      ;;
    docker)
      log "Installing Docker Desktop..."
      brew install --cask docker
      ;;
    rancher)
      log "Installing Rancher Desktop..."
      brew install --cask rancher
      ;;
    podman)
      log "Installing Podman..."
      brew install podman podman-compose
      ;;
    *)
      echo "[setup-mac] Unsupported runtime choice: $choice"
      exit 1
      ;;
  esac
}

choose_runtime_if_missing() {
  local has_colima="no"
  local has_docker="no"
  local has_podman="no"

  is_installed colima && has_colima="yes"
  is_installed docker && has_docker="yes"
  is_installed podman && has_podman="yes"

  if [[ "$has_colima" == "yes" || "$has_docker" == "yes" || "$has_podman" == "yes" ]]; then
    return
  fi

  log "No supported container runtime found."
  echo "[setup-mac] Choose a runtime to install:"
  echo "  1) Colima"
  echo "  2) Docker Desktop"
  echo "  3) Rancher Desktop"
  echo "  4) Podman"
  read -r -p "[setup-mac] Enter choice [1-4] (default 1): " selection

  case "${selection:-1}" in
    1) install_runtime colima ;;
    2) install_runtime docker ;;
    3) install_runtime rancher ;;
    4) install_runtime podman ;;
    *)
      echo "[setup-mac] Invalid selection."
      exit 1
      ;;
  esac
}

ensure_colima_runtime() {
  local project_status
  project_status="$(colima status -p "$COLIMA_PROFILE" 2>/dev/null || true)"
  if [[ "$project_status" != *"Running"* ]]; then
    log "Starting dedicated Colima profile: $COLIMA_PROFILE"
    colima start -p "$COLIMA_PROFILE" --runtime docker
  fi

  if is_installed docker; then
    log "Switching Docker context to $COLIMA_CONTEXT"
    docker context use "$COLIMA_CONTEXT" >/dev/null
  fi
}

ensure_docker_desktop_runtime() {
  if ! is_installed docker; then
    echo "[setup-mac] Docker CLI is not available. Install Docker Desktop and retry."
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    log "Starting Docker Desktop..."
    open -a Docker || true
    for _ in {1..60}; do
      if docker info >/dev/null 2>&1; then
        return
      fi
      sleep 2
    done
    echo "[setup-mac] Docker engine did not become ready in time."
    exit 1
  fi
}

ensure_podman_runtime() {
  if ! is_installed podman; then
    echo "[setup-mac] Podman is not available."
    exit 1
  fi
  local machine_status
  machine_status="$(podman machine list 2>/dev/null || true)"
  if [[ "$machine_status" != *"Currently running"* ]]; then
    log "Starting Podman machine..."
    podman machine init >/dev/null 2>&1 || true
    podman machine start
  fi
}

select_runtime() {
  local has_colima="false"
  local has_docker="false"
  local has_podman="false"
  local rancher_app="/Applications/Rancher Desktop.app"

  is_installed colima && has_colima="true"
  is_installed docker && has_docker="true"
  is_installed podman && has_podman="true"

  # Priority: Colima (if installed), then Docker-compatible runtime, then Podman.
  if [[ "$has_colima" == "true" ]]; then
    RUNTIME="colima"
    COMPOSE_TOOL="docker"
    ensure_colima_runtime
    return
  fi

  if [[ "$has_docker" == "true" ]]; then
    # If Rancher Desktop is installed, this docker CLI may be backed by Rancher or Docker Desktop.
    if [[ -d "$rancher_app" ]]; then
      RUNTIME="docker-or-rancher"
    else
      RUNTIME="docker"
    fi
    COMPOSE_TOOL="docker"
    ensure_docker_desktop_runtime
    return
  fi

  if [[ "$has_podman" == "true" ]]; then
    RUNTIME="podman"
    COMPOSE_TOOL="podman"
    ensure_podman_runtime
    return
  fi

  echo "[setup-mac] Unable to determine a usable container runtime."
  exit 1
}

resolve_compose_cmd() {
  if [[ "$COMPOSE_TOOL" == "docker" ]]; then
    if docker compose version >/dev/null 2>&1; then
      COMPOSE_CMD=("docker" "compose")
      return
    fi
    if command -v docker-compose >/dev/null 2>&1; then
      COMPOSE_CMD=("docker-compose")
      return
    fi
    echo "[setup-mac] Docker Compose was not found. Install Docker CLI/Compose and retry."
    exit 1
  fi

  if [[ "$COMPOSE_TOOL" == "podman" ]]; then
    if podman compose version >/dev/null 2>&1; then
      COMPOSE_CMD=("podman" "compose")
      return
    fi
    if command -v podman-compose >/dev/null 2>&1; then
      COMPOSE_CMD=("podman-compose")
      return
    fi
    echo "[setup-mac] Podman compose was not found. Install podman-compose and retry."
    exit 1
  fi

  echo "[setup-mac] Unknown compose tool selection."
  exit 1
}

stop_port_conflicts() {
  log "Checking for container conflicts on port 55432..."
  if [[ "$COMPOSE_TOOL" == "docker" ]]; then
    local ids
    ids="$(docker ps --filter "publish=55432" --format "{{.ID}}")"
    if [[ -n "$ids" ]]; then
      while IFS= read -r id; do
        [[ -z "$id" ]] && continue
        log "Stopping conflicting container: $id"
        docker stop "$id" >/dev/null
      done <<< "$ids"
    fi
    return
  fi

  local ids
  ids="$(podman ps --filter "publish=55432" --format "{{.ID}}" 2>/dev/null || true)"
  if [[ -n "$ids" ]]; then
    while IFS= read -r id; do
      [[ -z "$id" ]] && continue
      log "Stopping conflicting Podman container: $id"
      podman stop "$id" >/dev/null
    done <<< "$ids"
  fi
}

stop_host_port_conflicts() {
  local ports=("8080" "5173" "55432")
  log "Checking for host process conflicts on ports: ${ports[*]}"
  for port in "${ports[@]}"; do
    local pids
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    if [[ -z "$pids" ]]; then
      continue
    fi
    while IFS= read -r pid; do
      [[ -z "$pid" ]] && continue
      if [[ "$pid" == "$$" ]]; then
        continue
      fi
      log "Stopping process $pid listening on $port"
      kill "$pid" >/dev/null 2>&1 || true
      sleep 1
      if kill -0 "$pid" >/dev/null 2>&1; then
        log "Process $pid still running; forcing stop"
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    done <<< "$pids"
  done
}

wait_for_http_ready() {
  local url="$1"
  local label="$2"
  local max_attempts="${3:-90}"
  local attempt=1

  while (( attempt <= max_attempts )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "$label is ready at $url"
      return 0
    fi
    sleep 1
    ((attempt++))
  done
  echo "[setup-mac] Timed out waiting for $label at $url"
  return 1
}

open_frontend_in_browser() {
  local frontend_url="http://localhost:5173"
  # Cursor does not always open URL tabs reliably from shell commands.
  # We still best-effort ping Cursor, but always open the system default browser.
  if [[ -n "${CURSOR_TRACE_ID:-}" || -n "${CURSOR_SESSION_ID:-}" ]]; then
    open -a "Cursor" "$frontend_url" >/dev/null 2>&1 || true
  fi
  if open "$frontend_url" >/dev/null 2>&1; then
    log "Opened frontend in your default browser."
    return 0
  fi
  echo "[setup-mac] Could not open browser automatically. Visit $frontend_url"
  return 1
}

print_success_banner() {
  echo
  echo "==========================================================="
  echo "  CivicLens is running!"
  echo "  Frontend: http://localhost:5173"
  echo "  Backend:  http://localhost:8080"
  echo "  Swagger:  http://localhost:8080/swagger-ui.html"
  echo "==========================================================="
  echo
}

cleanup() {
  if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "$FRONTEND_PID" >/dev/null 2>&1; then
    log "Stopping frontend process ($FRONTEND_PID)..."
    kill "$FRONTEND_PID" >/dev/null 2>&1 || true
  fi
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    log "Stopping backend process ($BACKEND_PID)..."
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT INT TERM

log "Checking prerequisites..."
require_cmd npm "Install Node.js LTS from https://nodejs.org/"
require_cmd mvn "Install Maven (brew install maven) and Java 17."
require_cmd curl "Install curl to support startup health checks."
ensure_gitleaks_installed
install_pre_commit_hook
ensure_db_secrets
choose_runtime_if_missing
select_runtime
resolve_compose_cmd
stop_port_conflicts
stop_host_port_conflicts

log "Using runtime: $RUNTIME"

log "Installing frontend dependencies..."
(cd "$ROOT_DIR/frontend" && npm install)

log "Pre-fetching backend Maven dependencies..."
(cd "$ROOT_DIR/backend" && mvn -DskipTests dependency:go-offline >/dev/null)

log "Starting Postgres via Docker Compose..."
(
  cd "$ROOT_DIR/infra"
  "${COMPOSE_CMD[@]}" --project-name civiclens down --remove-orphans >/dev/null 2>&1 || true
  "${COMPOSE_CMD[@]}" --project-name civiclens up -d
)

log "Starting backend API on http://localhost:8080 ..."
(cd "$ROOT_DIR/backend" && mvn spring-boot:run) &
BACKEND_PID=$!

log "Starting frontend on http://localhost:5173 ..."
(cd "$ROOT_DIR/frontend" && npm run dev) &
FRONTEND_PID=$!

wait_for_http_ready "http://localhost:8080/api/health" "Backend API" 120
wait_for_http_ready "http://localhost:5173" "Frontend" 120
print_success_banner
open_frontend_in_browser || true

wait "$FRONTEND_PID"
