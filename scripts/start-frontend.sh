#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
UI_DIR="${ROOT_DIR}/ruoyi-ui"
# RAG独立服务默认使用8081，前端默认避让到8082
# author: fufu
# date: 2026-05-13 12:37:47 CST
FRONTEND_PORT="${FRONTEND_PORT:-8082}"

if ! command -v npm >/dev/null 2>&1; then
  echo "Error: npm is not installed or not in PATH."
  exit 1
fi

cd "${UI_DIR}"

if [ ! -d node_modules ] && [ "${SKIP_NPM_INSTALL:-0}" != "1" ]; then
  echo "node_modules not found, running npm install..."
  npm install
fi

export BROWSER="${BROWSER:-none}"
export port="${port:-${FRONTEND_PORT}}"

echo "Starting frontend from ${UI_DIR}"
echo "Frontend URL: http://localhost:${port}"
exec npm run dev -- "$@"
