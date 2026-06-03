#!/bin/bash

BASE_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"
LOG_DIR="$BASE_DIR/logs-local"

mkdir -p "$LOG_DIR"

echo "========== 0. 进入项目目录 =========="
cd "$BASE_DIR" || exit 1

echo "========== 1. 启动 Docker Desktop =========="
if ! docker info >/dev/null 2>&1; then
  echo "Docker 没启动，正在打开 Docker Desktop..."
  open -a Docker

  echo "等待 Docker 启动..."
  for i in {1..90}; do
    if docker info >/dev/null 2>&1; then
      echo "Docker 已启动"
      break
    fi
    echo "等待 Docker 中... $i"
    sleep 2
  done
else
  echo "Docker 已经启动"
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker 启动失败，请手动打开 Docker Desktop 后重试"
  exit 1
fi

echo "========== 2. 启动 MariaDB =========="
if command -v brew >/dev/null 2>&1; then
  brew services start mariadb >/dev/null 2>&1 || true
else
  echo "未检测到 brew，跳过 MariaDB 自动启动"
fi

echo "========== 3. 启动 Redis =========="
if command -v brew >/dev/null 2>&1; then
  brew services start redis >/dev/null 2>&1 || true
else
  echo "未检测到 brew，跳过 Redis 自动启动"
fi

echo "========== 4. 启动 Docker 容器 =========="
echo "当前已有容器："
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo "尝试启动已有的 MinIO / Milvus / Etcd / Attu 等容器..."
for name in $(docker ps -a --format "{{.Names}}" | grep -Ei "minio|milvus|etcd|attu|standalone|pulsar|mysql|mariadb|redis"); do
  echo "启动容器：$name"
  docker start "$name" >/dev/null 2>&1 || true
done

echo "========== 5. 检查关键端口占用 =========="
echo "8080 若依后端："
lsof -i:8080 || true

echo "8081 RAG Server："
lsof -i:8081 || true

echo "1024 若依前端："
lsof -i:1024 || true

echo "9000 MinIO API："
lsof -i:9000 || true

echo "9001 MinIO Console："
lsof -i:9001 || true

echo "19530 Milvus："
lsof -i:19530 || true

echo "========== 6. 启动 RAG Server 8081 =========="
if lsof -i:8081 >/dev/null 2>&1; then
  echo "RAG Server 8081 已经启动，跳过"
else
  RAG_SERVER_DIR=""

  if [ -f "$BASE_DIR/RuoYi-Vue-ragold-fufu-week4/rag_server/pom.xml" ]; then
    RAG_SERVER_DIR="$BASE_DIR/RuoYi-Vue-ragold-fufu-week4/rag_server"
  elif [ -f "$BASE_DIR/RuoYi-Vue-fufu-week4/rag_server/pom.xml" ]; then
    RAG_SERVER_DIR="$BASE_DIR/RuoYi-Vue-fufu-week4/rag_server"
  elif [ -f "$BASE_DIR/rag_server/pom.xml" ]; then
    RAG_SERVER_DIR="$BASE_DIR/rag_server"
  fi

  if [ -z "$RAG_SERVER_DIR" ]; then
    echo "未找到 rag_server 目录，跳过 RAG Server"
    echo "如果需要 RAG Server，请先确认 fufu_week4 worktree 是否存在"
  else
    echo "使用 RAG Server 目录：$RAG_SERVER_DIR"
    cd "$RAG_SERVER_DIR" || exit 1
    nohup mvn spring-boot:run > "$LOG_DIR/rag_server.log" 2>&1 &
    echo "RAG Server 启动命令已执行，日志：$LOG_DIR/rag_server.log"
  fi
fi

echo "========== 7. 启动若依后端 8080 =========="
cd "$BASE_DIR" || exit 1

if lsof -i:8080 >/dev/null 2>&1; then
  echo "若依后端 8080 已经启动，跳过"
else
  nohup mvn -pl ruoyi-admin spring-boot:run > "$LOG_DIR/ruoyi_backend.log" 2>&1 &
  echo "若依后端启动命令已执行，日志：$LOG_DIR/ruoyi_backend.log"
fi

echo "========== 8. 启动若依前端 1024 =========="
cd "$BASE_DIR/ruoyi-ui" || exit 1

if lsof -i:1024 >/dev/null 2>&1; then
  echo "若依前端 1024 已经启动，跳过"
else
  nohup npm run dev > "$LOG_DIR/ruoyi_frontend.log" 2>&1 &
  echo "若依前端启动命令已执行，日志：$LOG_DIR/ruoyi_frontend.log"
fi

echo "========== 9. 等待服务启动 =========="
sleep 8

echo "========== 10. 最终端口检查 =========="
echo "8080 若依后端："
lsof -i:8080 || echo "8080 未启动"

echo "8081 RAG Server："
lsof -i:8081 || echo "8081 未启动"

echo "1024 若依前端："
lsof -i:1024 || echo "1024 未启动"

echo "9000 MinIO API："
lsof -i:9000 || echo "9000 未启动或未映射"

echo "9001 MinIO Console："
lsof -i:9001 || echo "9001 未启动或未映射"

echo "19530 Milvus："
lsof -i:19530 || echo "19530 未启动或未映射"

echo "========== 11. 访问地址 =========="
echo "若依前端：http://localhost:1024"
echo "若依后端：http://localhost:8080"
echo "RAG Server：http://localhost:8081"
echo "MinIO 控制台：http://localhost:9001"

echo "========== 12. 日志位置 =========="
echo "若依后端日志：$LOG_DIR/ruoyi_backend.log"
echo "若依前端日志：$LOG_DIR/ruoyi_frontend.log"
echo "RAG Server 日志：$LOG_DIR/rag_server.log"

echo "========== 启动脚本执行完成 =========="
