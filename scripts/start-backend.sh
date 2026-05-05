#!/usr/bin/env bash
#
# 一键重新打包并启动后端服务
# author: fufu
# date: 2026-05-05

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/logs"
PID_FILE="${LOG_DIR}/ruoyi-admin.pid"
JAR_FILE="${PROJECT_ROOT}/ruoyi-admin/target/ruoyi-admin.jar"
JAVA_OPTS="${JAVA_OPTS:-}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-druid}"

mkdir -p "${LOG_DIR}"

echo "== RuoYi backend startup =="
echo "Project root: ${PROJECT_ROOT}"
echo "Build time: $(date '+%Y-%m-%d %H:%M:%S')"

cd "${PROJECT_ROOT}"
echo
echo "[1/3] Repackaging backend from current workspace..."
mvn -pl ruoyi-admin -am -DskipTests clean package

if [[ ! -f "${JAR_FILE}" ]]; then
  echo "ERROR: packaged jar not found: ${JAR_FILE}" >&2
  exit 1
fi

echo
echo "[2/3] Stopping previous backend process from pid file if present..."
if [[ -f "${PID_FILE}" ]]; then
  OLD_PID="$(cat "${PID_FILE}")"
  if [[ -n "${OLD_PID}" ]] && kill -0 "${OLD_PID}" 2>/dev/null; then
    echo "Stopping pid ${OLD_PID}"
    kill "${OLD_PID}"
    for _ in {1..30}; do
      if ! kill -0 "${OLD_PID}" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    if kill -0 "${OLD_PID}" 2>/dev/null; then
      echo "Process ${OLD_PID} is still running; please stop it manually if it is not this backend."
    fi
  fi
fi

echo
echo "[3/3] Starting backend jar..."
nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" \
  --spring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
  > "${LOG_DIR}/ruoyi-admin.log" 2>&1 &
NEW_PID="$!"
echo "${NEW_PID}" > "${PID_FILE}"

echo "Backend started."
echo "PID: ${NEW_PID}"
echo "Log: ${LOG_DIR}/ruoyi-admin.log"
echo "URL: http://localhost:${SERVER_PORT:-8080}"

echo
echo "Waiting for backend readiness..."
for _ in {1..60}; do
  if curl --retry 0 -fsS "http://localhost:${SERVER_PORT:-8080}/captchaImage" >/dev/null 2>&1; then
    echo "Backend is ready."
    exit 0
  fi
  sleep 1
done

echo "Backend process was started, but readiness check did not pass within 60 seconds."
echo "Please inspect: ${LOG_DIR}/ruoyi-admin.log"
exit 1
