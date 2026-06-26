#!/bin/bash

BASE_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"

echo "========== 1. 关闭若依前端 1024 =========="
lsof -tiTCP:1024 -sTCP:LISTEN | xargs -r kill -9
pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true

echo "========== 2. 关闭若依后端 8080 =========="
lsof -tiTCP:8080 -sTCP:LISTEN | xargs -r kill -9

echo "========== 3. 关闭 RAG Server 8081 =========="
lsof -tiTCP:8081 -sTCP:LISTEN | xargs -r kill -9

echo "========== 4. 停止 RAG Docker 容器 =========="
for name in rag-milvus rag-minio rag-etcd rag-attu; do
  if docker ps --format "{{.Names}}" | grep -qx "$name"; then
    echo "停止容器：$name"
    docker stop "$name" >/dev/null 2>&1 || true
  fi
done

echo "========== 5. MariaDB / Redis 默认不关闭 =========="
echo "如果只是关项目，数据库和 Redis 可以留着，不影响电脑正常使用。"

echo "========== 关闭完成 =========="
