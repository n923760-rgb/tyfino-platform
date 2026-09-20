#!/usr/bin/env bash

set -Eeuo pipefail

readonly RELEASE_SHA="${TYFINO_RELEASE_SHA:-}"
readonly REPOSITORY_URL="https://github.com/n923760-rgb/tyfino-platform.git"
readonly APP_ROOT="/opt/tyfino"
readonly RELEASES_ROOT="${APP_ROOT}/releases"
readonly RELEASE_DIR="${RELEASES_ROOT}/${RELEASE_SHA}"
readonly CURRENT_LINK="${APP_ROOT}/current"
readonly CONFIG_ROOT="/etc/tyfino"
readonly ENV_FILE="${CONFIG_ROOT}/tyfino.env"
readonly OWNER_SETUP_FILE="/root/tyfino-owner-setup.txt"
readonly BACKUP_ROOT="/var/backups/tyfino"

log() {
  printf '[tyfino-deploy] %s\n' "$*"
}

fail() {
  printf '[tyfino-deploy] ERROR: %s\n' "$*" >&2
  exit 1
}

if [[ "${EUID}" -ne 0 ]]; then
  fail "Run this script as root."
fi
if [[ ! "${RELEASE_SHA}" =~ ^[0-9a-f]{40}$ ]]; then
  fail "TYFINO_RELEASE_SHA must be an exact 40-character commit SHA."
fi
if ! id tyfino >/dev/null 2>&1; then
  fail "The tyfino deploy user is missing; run bootstrap-ubuntu.sh first."
fi
for required_command in curl git openssl base32 sudo; do
  if ! command -v "${required_command}" >/dev/null 2>&1; then
    fail "Required command is missing: ${required_command}."
  fi
done
if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  fail "Docker Engine and Compose are required."
fi

umask 077
install -d -m 750 -o tyfino -g tyfino "${RELEASES_ROOT}"
install -d -m 750 -o root -g tyfino "${CONFIG_ROOT}"
install -d -m 700 -o root -g root "${BACKUP_ROOT}"

if [[ ! -f "${ENV_FILE}" ]]; then
  log "Generating first-install secrets locally on the VPS."
  owner_password="$(openssl rand -hex 24)"
  owner_db_password="$(openssl rand -hex 32)"
  app_db_password="$(openssl rand -hex 32)"
  token_pepper="$(openssl rand -hex 32)"
  totp_secret="$(openssl rand 20 | base32 | tr -d '=\n')"

  env_temp="$(mktemp "${CONFIG_ROOT}/tyfino.env.XXXXXX")"
  cat > "${env_temp}" <<EOF
ROOT_DOMAIN=tyfino.online
ADMIN_DOMAIN=admin.tyfino.online
API_DOMAIN=api.tyfino.online

POSTGRES_DB=tyfino
POSTGRES_USER=tyfino_owner
POSTGRES_PASSWORD=${owner_db_password}
POSTGRES_APP_USER=tyfino_app
POSTGRES_APP_PASSWORD=${app_db_password}
DATABASE_URL=postgresql://tyfino_app:${app_db_password}@db:5432/tyfino

API_PORT=3000
LOG_LEVEL=info
TRUST_PROXY=true
APP_ENV=production
ADMIN_ORIGIN=https://admin.tyfino.online

# Pre-production starting limits. Re-qualify from measured traffic before customer release.
GLOBAL_RATE_LIMIT_PER_MINUTE=120
ADMIN_AUTH_RATE_LIMIT_PER_15_MINUTES=8
TRIAL_START_RATE_LIMIT_PER_HOUR=5
ACTIVATION_RATE_LIMIT_PER_15_MINUTES=8
ENTITLEMENT_REFRESH_RATE_LIMIT_PER_HOUR=20
HTTP_REQUEST_TIMEOUT_MS=15000
DATABASE_STATEMENT_TIMEOUT_MS=10000

TOKEN_PEPPER=${token_pepper}
ADMIN_BOOTSTRAP_EMAIL=owner@tyfino.online
ADMIN_BOOTSTRAP_PASSWORD=${owner_password}
ADMIN_OWNER_TOTP_SECRET=${totp_secret}
EOF
  install -m 640 -o root -g tyfino "${env_temp}" "${ENV_FILE}"
  rm -f "${env_temp}"

  cat > "${OWNER_SETUP_FILE}" <<EOF
TYFINO initial owner setup — keep private and delete after verified login

Admin URL: https://admin.tyfino.online
Email: owner@tyfino.online
Initial password: ${owner_password}
Authenticator setup key: ${totp_secret}
Authenticator account: TYFINO (owner@tyfino.online)
EOF
  chmod 600 "${OWNER_SETUP_FILE}"
else
  if grep -q 'CHANGE_ME' "${ENV_FILE}"; then
    fail "Refusing to use placeholder secrets in ${ENV_FILE}."
  fi
  log "Reusing the existing protected environment file."
fi

if [[ ! -d "${RELEASE_DIR}/.git" ]]; then
  log "Fetching the exact release commit."
  install -d -m 750 -o tyfino -g tyfino "${RELEASE_DIR}"
  sudo -u tyfino git -C "${RELEASE_DIR}" init --quiet
  sudo -u tyfino git -C "${RELEASE_DIR}" remote add origin "${REPOSITORY_URL}"
  sudo -u tyfino git -C "${RELEASE_DIR}" fetch --quiet --depth 1 origin "${RELEASE_SHA}"
  sudo -u tyfino git -C "${RELEASE_DIR}" checkout --quiet --detach FETCH_HEAD
fi

actual_sha="$(sudo -u tyfino git -C "${RELEASE_DIR}" rev-parse HEAD)"
if [[ "${actual_sha}" != "${RELEASE_SHA}" ]]; then
  fail "Release directory does not match the requested commit."
fi

ln -sfn "${ENV_FILE}" "${RELEASE_DIR}/.env"
ln -sfn "${RELEASE_DIR}" "${CURRENT_LINK}"
chown -h tyfino:tyfino "${CURRENT_LINK}"

log "Building and starting the pinned release."
docker compose --project-directory "${RELEASE_DIR}" build --pull
docker compose --project-directory "${RELEASE_DIR}" up -d --remove-orphans

api_container="$(docker compose --project-directory "${RELEASE_DIR}" ps -q api)"
if [[ -z "${api_container}" ]]; then
  fail "The API container was not created."
fi

for attempt in $(seq 1 60); do
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${api_container}")"
  if [[ "${health}" == "healthy" ]]; then
    break
  fi
  if [[ "${health}" == "unhealthy" || "${health}" == "exited" || "${health}" == "dead" ]]; then
    docker compose --project-directory "${RELEASE_DIR}" logs --tail 80 api >&2
    fail "API container entered state: ${health}."
  fi
  if [[ "${attempt}" -eq 60 ]]; then
    docker compose --project-directory "${RELEASE_DIR}" logs --tail 80 api >&2
    fail "API did not become healthy within five minutes."
  fi
  sleep 5
done

if grep -q '^ADMIN_BOOTSTRAP_PASSWORD=' "${ENV_FILE}"; then
  sed -i '/^ADMIN_BOOTSTRAP_PASSWORD=/d' "${ENV_FILE}"
  log "Removed the bootstrap password from the runtime environment after owner creation."
  docker compose --project-directory "${RELEASE_DIR}" up -d --no-deps --force-recreate api
  api_container="$(docker compose --project-directory "${RELEASE_DIR}" ps -q api)"
  for attempt in $(seq 1 30); do
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${api_container}")"
    [[ "${health}" == "healthy" ]] && break
    if [[ "${health}" == "unhealthy" || "${health}" == "exited" || "${attempt}" -eq 30 ]]; then
      docker compose --project-directory "${RELEASE_DIR}" logs --tail 80 api >&2
      fail "API failed after removing the bootstrap credential."
    fi
    sleep 5
  done
fi

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${BACKUP_ROOT}/tyfino-initial-${timestamp}.dump"
docker compose --project-directory "${RELEASE_DIR}" exec -T db \
  sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --format=custom --no-owner --no-acl' \
  > "${backup_file}"
chmod 600 "${backup_file}"
docker compose --project-directory "${RELEASE_DIR}" exec -T db pg_restore --list < "${backup_file}" >/dev/null
sha256sum "${backup_file}" > "${backup_file}.sha256"
chmod 600 "${backup_file}.sha256"

for url in \
  https://tyfino.online/ \
  https://api.tyfino.online/readyz \
  https://admin.tyfino.online/; do
  if ! curl --fail --silent --show-error --retry 12 --retry-delay 5 --retry-all-errors "${url}" >/dev/null; then
    fail "HTTPS verification failed for ${url}."
  fi
done

printf '%s\n' "${RELEASE_SHA}" > "${APP_ROOT}/DEPLOYED_SHA"
chmod 640 "${APP_ROOT}/DEPLOYED_SHA"
chown root:tyfino "${APP_ROOT}/DEPLOYED_SHA"

docker compose --project-directory "${RELEASE_DIR}" ps
log "Pre-production deployment and HTTPS checks completed."
log "Initial owner setup is stored only in ${OWNER_SETUP_FILE} with mode 600."
log "Do not send or screenshot that file. Delete it after login and authenticator verification."
