#!/bin/bash

echo "========== RAG 权限治理项目环境检查 =========="

check_port() {
  local port=$1
  local name=$2

  if lsof -i :"$port" >/dev/null 2>&1; then
    echo "✅ $name 端口 $port 已启动"
  else
    echo "❌ $name 端口 $port 未启动"
  fi
}

check_http() {
  local url=$1
  local name=$2

  if curl -s --max-time 3 "$url" >/dev/null 2>&1; then
    echo "✅ $name 可访问：$url"
  else
    echo "❌ $name 不可访问：$url"
  fi
}

echo
echo "1. 端口检查"
check_port 1024 "若依前端"
check_port 8080 "若依后端"
check_port 8081 "RAG Server"
check_port 3307 "MariaDB"
check_port 6380 "Redis"
check_port 9000 "MinIO"
check_port 19530 "Milvus"

echo
echo "2. HTTP 健康检查"
check_http "http://localhost:1024" "若依前端"
check_http "http://localhost:8080/captchaImage" "若依后端 captchaImage"
check_http "http://localhost:8081/rag/file/mariadb/list" "RAG Server MariaDB 接口"
check_http "http://localhost:9000/minio/health/live" "MinIO 健康检查"

echo
echo "3. 服务级检查"
if command -v docker >/dev/null 2>&1 && docker ps --format "{{.Names}}" | grep -qx "rag-mariadb"; then
  if docker exec rag-mariadb mariadb-admin -uroot -p001106 ping >/dev/null 2>&1; then
    echo "✅ MariaDB 容器内服务正常"
  else
    echo "❌ MariaDB 容器内服务异常"
  fi
fi

if command -v redis-cli >/dev/null 2>&1; then
  if redis-cli -h 127.0.0.1 -p 6380 ping >/dev/null 2>&1; then
    echo "✅ Redis 可用：127.0.0.1:6380"
  else
    echo "❌ Redis 不可用：127.0.0.1:6380"
  fi
fi

echo
echo "4. Docker 容器检查"
if command -v docker >/dev/null 2>&1; then
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
  echo "❌ 未检测到 docker 命令"
fi

echo
echo "检查完成。"
