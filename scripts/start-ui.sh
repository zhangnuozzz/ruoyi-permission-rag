#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../ruoyi-ui"

echo "正在启动若依前端服务..."
echo "项目目录：$(pwd)"

if command -v nvm >/dev/null 2>&1; then
  nvm use 16 || true
else
  export NVM_DIR="$HOME/.nvm"
  [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" && nvm use 16 || true
fi

npm run dev -- --port 1024
