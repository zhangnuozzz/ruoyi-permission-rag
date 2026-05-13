#!/usr/bin/env bash
set -euo pipefail

# RAG独立HTTP服务启动脚本
# author: fufu
# date: 2026-05-13 12:37:47 CST

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}/rag_server"

if [ -x "${ROOT_DIR}/mvnw" ]; then
  MVN_CMD=("${ROOT_DIR}/mvnw")
elif command -v mvn >/dev/null 2>&1; then
  MVN_CMD=(mvn)
else
  echo "Error: Maven is not installed or not in PATH."
  exit 1
fi

echo "Starting independent rag_server from ${ROOT_DIR}/rag_server"
echo "RAG Server URL: http://localhost:8081"
export RAG_DB_URL="${RAG_DB_URL:-jdbc:mariadb://127.0.0.1:3306/ruoyi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai}"
export RAG_DB_USERNAME="${RAG_DB_USERNAME:-fufu}"
export RAG_DB_PASSWORD="${RAG_DB_PASSWORD:-Eee2.71828}"
exec "${MVN_CMD[@]}" spring-boot:run "$@"
