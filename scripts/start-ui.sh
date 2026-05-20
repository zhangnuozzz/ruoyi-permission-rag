#!/bin/bash

cd "$(dirname "$0")/../ruoyi-ui"

echo "正在启动若依前端服务..."
echo "项目目录：$(pwd)"

if command -v nvm >/dev/null 2>&1; then
  nvm use 16
else
  export NVM_DIR="$HOME/.nvm"
  [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
  nvm use 16
fi

npm run dev
