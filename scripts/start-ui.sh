#!/bin/bash

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "$PROJECT_ROOT/ruoyi-ui"


echo "正在启动若依前端服务..."
echo "项目目录：$(pwd)"

export NODE_OPTIONS="--require $PROJECT_ROOT/scripts/node-networkinterfaces-shim.js ${NODE_OPTIONS:-}"

export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  . "$NVM_DIR/nvm.sh"
fi

if command -v nvm >/dev/null 2>&1; then
  nvm use 16
fi

npm run dev
