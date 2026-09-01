#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$BASE_DIR/.run-local"

kill_pid_file() {
  local pid_file="$1"
  local name="$2"

  if [ -f "$pid_file" ]; then
    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"

    if [ -n "$pid" ]; then
      kill "$pid" >/dev/null 2>&1 || true
    fi

    rm -f "$pid_file"

    echo "$name 已关闭"
  fi
}

kill_port() {
  local port="$1"
  local pids

  pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"

  if [ -n "$pids" ]; then
    echo "$pids" | xargs kill -9 >/dev/null 2>&1 || true
  fi
}

echo "========== 1. 关闭若依前端 =========="

kill_pid_file \
  "$RUN_DIR/ruoyi_frontend.pid" \
  "若依前端"

kill_port 1024

pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true

echo "========== 2. 关闭若依后端 =========="

kill_pid_file \
  "$RUN_DIR/ruoyi_backend.pid" \
  "若依后端"

kill_port 8080

echo "========== 3. 关闭 RAG Server =========="

kill_pid_file \
  "$RUN_DIR/rag_server.pid" \
  "RAG Server"

kill_port 8081

echo "========== 4. 停止 RAG Docker 依赖 =========="

if docker info >/dev/null 2>&1; then
  docker compose \
    -f "$BASE_DIR/docker-compose.rag.yml" \
    stop
else
  echo "Docker 当前未运行，跳过"
fi

echo
echo "============================================"
echo "              ✅ 系统已关闭"
echo "============================================"
echo
echo "注意："
echo "本机 MariaDB 3306 和 Redis 6379 不关闭。"
echo "这是若依正式开发环境使用的基础服务。"
echo
