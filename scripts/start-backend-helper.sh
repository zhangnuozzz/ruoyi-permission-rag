#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${LOG_DIR:-${ROOT_DIR}/logs}"
UPLOAD_DIR="${RUOYI_PROFILE:-${ROOT_DIR}/uploadPath}"

cd "${ROOT_DIR}"

if [ -x "${ROOT_DIR}/mvnw" ]; then
  MVN_CMD=("${ROOT_DIR}/mvnw")
elif command -v mvn >/dev/null 2>&1; then
  MVN_CMD=(mvn)
else
  echo "Error: Maven is not installed or not in PATH."
  exit 1
fi

mkdir -p "${LOG_DIR}" "${UPLOAD_DIR}"
export LOG_PATH="${LOG_DIR}"
export RUOYI_PROFILE="${UPLOAD_DIR}"

echo "Starting backend module ruoyi-admin from ${ROOT_DIR}"
echo "Cleaning and installing current workspace modules..."
"${MVN_CMD[@]}" -pl ruoyi-admin -am -DskipTests clean install

JVM_ARGS="-Druoyi.profile=${UPLOAD_DIR}"

if [ -n "${DB_URL:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.datasource.druid.master.url=${DB_URL}"
fi
if [ -n "${DB_USERNAME:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.datasource.druid.master.username=${DB_USERNAME}"
fi
if [ -n "${DB_PASSWORD:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.datasource.druid.master.password=${DB_PASSWORD}"
fi
if [ -n "${REDIS_HOST:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.data.redis.host=${REDIS_HOST}"
fi
if [ -n "${REDIS_PORT:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.data.redis.port=${REDIS_PORT}"
fi
if [ -n "${REDIS_PASSWORD:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.data.redis.password=${REDIS_PASSWORD}"
fi
if [ -n "${REDIS_DATABASE:-}" ]; then
  JVM_ARGS="${JVM_ARGS} -Dspring.data.redis.database=${REDIS_DATABASE}"
fi

echo "Backend URL: http://localhost:8080"
exec "${MVN_CMD[@]}" -f "${ROOT_DIR}/ruoyi-admin/pom.xml" spring-boot:run -Dspring-boot.run.jvmArguments="${JVM_ARGS}" "$@"
