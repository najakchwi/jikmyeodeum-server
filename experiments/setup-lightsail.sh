#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env.lightsail"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy .env.lightsail.example and fill in the Lightsail public address." >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${ENV_FILE}"

: "${LIGHTSAIL_SSH_HOST:?LIGHTSAIL_SSH_HOST is required}"
: "${LIGHTSAIL_REMOTE_DIR:?LIGHTSAIL_REMOTE_DIR is required}"

REMOTE_COMPOSE="${LIGHTSAIL_REMOTE_DIR}/docker-compose.experiment.yml"
REMOTE_CONFIG_DIR="${LIGHTSAIL_REMOTE_DIR}/experiment-config"
REMOTE_ENV="${LIGHTSAIL_REMOTE_DIR}/.env.experiment"

ssh "${LIGHTSAIL_SSH_HOST}" "mkdir -p '${LIGHTSAIL_REMOTE_DIR}/experiment-logs' '${REMOTE_CONFIG_DIR}' '${LIGHTSAIL_REMOTE_DIR}/experiment-artifacts'"
scp "${SCRIPT_DIR}/docker-compose.experiment.yml" "${LIGHTSAIL_SSH_HOST}:${REMOTE_COMPOSE}"
scp "${SCRIPT_DIR}/../src/main/resources/application-experiment.yml" \
  "${LIGHTSAIL_SSH_HOST}:${REMOTE_CONFIG_DIR}/application-experiment.yml"

if ! ssh "${LIGHTSAIL_SSH_HOST}" "test -f '${REMOTE_ENV}'"; then
  echo "Remote ${REMOTE_ENV} is missing." >&2
  echo "Create it by copying /home/ubuntu/.env and replacing DB_URL, DB_USERNAME, DB_PASSWORD with the Supabase experiment project values." >&2
  exit 2
fi

ssh "${LIGHTSAIL_SSH_HOST}" "cd '${LIGHTSAIL_REMOTE_DIR}' && docker compose --env-file .env.experiment -f '${REMOTE_COMPOSE}' config >/dev/null"

cat <<EOF
Lightsail experiment files are staged successfully.

Start the experiment container with:
  ssh ${LIGHTSAIL_SSH_HOST} 'cd ${LIGHTSAIL_REMOTE_DIR} && docker compose --env-file .env.experiment -f ${REMOTE_COMPOSE} up -d'

Inspect it with:
  ssh ${LIGHTSAIL_SSH_HOST} 'docker logs -f jikmyeodeum-server-experiment'
EOF
