#!/bin/bash

BASE_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"

echo "========== 0. 进入项目目录 =========="
cd "$BASE_DIR" || exit 1

echo "========== 1. 关闭若依前端 1024 =========="
PID_1024=$(lsof -ti:1024)
if [ -n "$PID_1024" ]; then
  echo "关闭 1024 端口进程：$PID_1024"
  kill -9 $PID_1024
else
  echo "1024 未运行"
fi

echo "========== 2. 关闭若依后端 8080 =========="
PID_8080=$(lsof -ti:8080)
if [ -n "$PID_8080" ]; then
  echo "关闭 8080 端口进程：$PID_8080"
  kill -9 $PID_8080
else
  echo "8080 未运行"
fi

echo "========== 3. 关闭 RAG Server 8081 =========="
PID_8081=$(lsof -ti:8081)
if [ -n "$PID_8081" ]; then
  echo "关闭 8081 端口进程：$PID_8081"
  kill -9 $PID_8081
else
  echo "8081 未运行"
fi

echo "========== 4. 关闭 MinIO / Milvus / Etcd / Attu 等 Docker 容器 =========="
if docker info >/dev/null 2>&1; then
  for name in $(docker ps --format "{{.Names}}" | grep -Ei "minio|milvus|etcd|attu|standalone|pulsar"); do
    echo "停止容器：$name"
    docker stop "$name" >/dev/null 2>&1 || true
  done
else
  echo "Docker 当前未启动，跳过 Docker 容器关闭"
fi

echo "========== 5. 是否关闭 Redis / MariaDB =========="
echo "默认不强制关闭 MariaDB 和 Redis，避免影响你其他项目。"
echo "如果你确定今天不用了，可以手动执行："
echo "brew services stop redis"
echo "brew services stop mariadb"

echo "========== 6. 最终检查端口 =========="
echo "1024 若依前端："
lsof -i:1024 || echo "1024 已关闭"

echo "8080 若依后端："
lsof -i:8080 || echo "8080 已关闭"

echo "8081 RAG Server："
lsof -i:8081 || echo "8081 已关闭"

echo "9000 MinIO API："
lsof -i:9000 || echo "9000 已关闭或未映射"

echo "9001 MinIO Console："
lsof -i:9001 || echo "9001 已关闭或未映射"

echo "19530 Milvus："
lsof -i:19530 || echo "19530 已关闭或未映射"

echo "========== 关闭完成 =========="
