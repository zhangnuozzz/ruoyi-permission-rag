#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$BASE_DIR/.run-local"

kill_pid_file() {
  local pid_file="$1"
  if [ -f "$pid_file" ]; then
    kill "$(cat "$pid_file")" >/dev/null 2>&1 || true
    rm -f "$pid_file"
  fi
}

kill_port() {
  lsof -tiTCP:"$1" -sTCP:LISTEN | xargs -r kill -9 || true
}

echo "========== 1. 关闭若依前端 1024 =========="
kill_pid_file "$RUN_DIR/ruoyi_frontend.pid"
kill_port 1024
pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true

echo "========== 2. 关闭若依后端 8080 =========="
kill_pid_file "$RUN_DIR/ruoyi_backend.pid"
kill_port 8080

echo "========== 3. 关闭 RAG Server 8081 =========="
kill_pid_file "$RUN_DIR/rag_server.pid"
kill_port 8081

echo "========== 4. 停止 Docker 依赖容器 =========="
if docker info >/dev/null 2>&1; then
  docker compose -f "$BASE_DIR/docker-compose.rag.yml" stop
else
  echo "Docker 不可用，跳过容器停止"
fi

echo "========== 关闭完成 =========="
