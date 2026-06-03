#!/bin/zsh
set -u

PROJECT_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"

echo "========== 0. 进入项目目录 =========="
cd "$PROJECT_DIR" || exit 1

stop_port() {
  local port="$1"
  local name="$2"

  echo "========== 关闭 $name $port =========="
  local pids
  pids=$(lsof -ti :"$port" 2>/dev/null || true)

  if [ -z "$pids" ]; then
    echo "$port 未运行"
    return 0
  fi

  echo "尝试正常关闭 $port 端口进程：$pids"
  kill $pids 2>/dev/null || true

  for i in {1..10}; do
    if ! lsof -ti :"$port" >/dev/null 2>&1; then
      echo "$port 已正常关闭"
      return 0
    fi
    sleep 1
  done

  pids=$(lsof -ti :"$port" 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "$port 未正常退出，执行强制关闭：$pids"
    kill -9 $pids 2>/dev/null || true
  fi
}

stop_port 1024 "若依前端"
stop_port 8080 "若依后端"
stop_port 8081 "RAG Server"

echo "========== 4. 关闭本项目 Docker 容器 =========="
if docker info >/dev/null 2>&1; then
  for name in rag-milvus rag-minio rag-etcd; do
    if docker ps --format "{{.Names}}" | grep -qx "$name"; then
      echo "停止容器：$name"
      docker stop "$name" >/dev/null 2>&1 || true
    else
      echo "$name 未运行"
    fi
  done
else
  echo "Docker 当前未启动，跳过 Docker 容器关闭"
fi

echo "========== 5. MariaDB / Redis 处理 =========="
echo "默认不关闭 MariaDB 和 Redis，避免影响其他项目。"
echo "如需完全关闭，可手动执行："
echo "brew services stop redis"
echo "brew services stop mariadb"

echo "========== 6. 最终检查端口 =========="
for port in 1024 8080 8081 9000 9001 19530; do
  echo "$port："
  lsof -i :"$port" || echo "$port 已关闭或未运行"
done

echo "========== 关闭完成 =========="
