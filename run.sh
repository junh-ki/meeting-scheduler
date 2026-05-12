#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# ANSI palette — mirrors Spring Boot / IntelliJ Logback colours
DIM='\033[2m'; MAGENTA='\033[35m'; CYAN='\033[36m'
GREEN='\033[32m'; YELLOW='\033[33m'; RED='\033[31m'; RESET='\033[0m'

_log() {
  local lvl="$1" clr="$2"; shift 2
  printf "${DIM}%s${RESET}  ${clr}%5s${RESET} ${MAGENTA}%s${RESET} ${DIM}---${RESET} [${DIM}  script${RESET}] ${CYAN}run${RESET}${DIM}:${RESET} %b\n" \
    "$(date '+%Y-%m-%dT%H:%M:%S')" "$lvl" "$$" "$*"
}
info()  { _log  INFO "$GREEN"  "$@"; }
warn()  { _log  WARN "$YELLOW" "$@"; }
error() { _log ERROR "$RED"    "$@"; }

# ── shutdown ──────────────────────────────────────────────────────────────────
APP_PID=""

cleanup() {
  trap - INT TERM EXIT
  echo
  warn "Interrupt received — shutting down gracefully..."
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    info "Sending SIGTERM to Spring Boot (PID ${MAGENTA}${APP_PID}${RESET})..."
    kill -TERM "$APP_PID"
    wait "$APP_PID" 2>/dev/null || true
    info "Spring Boot stopped."
  fi
  info "Tearing down Postgres container ${DIM}(volumes preserved)${RESET}..."
  docker compose down
  info "All done."
}
trap cleanup INT TERM EXIT

# ── 1. postgres ───────────────────────────────────────────────────────────────
info "Starting Postgres..."
docker compose up -d

info "Waiting for Postgres to accept connections..."
until docker compose exec -T postgres \
    pg_isready -U meeting_scheduler -d meeting_scheduler > /dev/null 2>&1; do
  sleep 1
done
info "Postgres is ${GREEN}ready${RESET}."

# ── 2. build ──────────────────────────────────────────────────────────────────
info "Building application ${DIM}(tests skipped)${RESET}..."
./mvnw clean package -DskipTests -q
info "Build ${GREEN}complete${RESET}."

# ── 3. run ────────────────────────────────────────────────────────────────────
JAR=$(find target -maxdepth 1 -name "meeting-scheduler-*.jar" ! -name "*sources*" | head -1)
info "Launching ${CYAN}meeting-scheduler${RESET} — ${DIM}${JAR}${RESET}"
info "Debugger listening on ${CYAN}localhost:5005${RESET} ${DIM}(suspend=n)${RESET}"

java -Dspring.output.ansi.enabled=always \
     -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     -jar "$JAR" &
APP_PID=$!
wait "$APP_PID"
