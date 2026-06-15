#!/bin/bash

BASE_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"
LOG_DIR="$BASE_DIR/logs-local"
RAG_SERVER_DIR="$BASE_DIR/RuoYi-Vue-ragold-fufu-week4/rag_server"

RUOYI_JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home"
RAG_JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home"

mkdir -p "$LOG_DIR"

is_listen() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

show_listen() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN || echo "$1 没有 LISTEN"
}

wait_port() {
  local port="$1"
  local name="$2"
  local max="${3:-60}"

  for i in $(seq 1 "$max"); do
    if is_listen "$port"; then
      echo "$name $port 已监听"
      return 0
    fi
    echo "等待 $name $port 启动中... $i"
    sleep 2
  done

  echo "$name $port 启动超时"
  return 1
}

wait_http() {
  local url="$1"
  local name="$2"
  local max="${3:-60}"

  for i in $(seq 1 "$max"); do
    if curl -s --connect-timeout 2 "$url" >/dev/null 2>&1; then
      echo "$name 可访问：$url"
      return 0
    fi
    echo "等待 $name 可访问中... $i"
    sleep 2
  done

  echo "$name 访问超时：$url"
  return 1
}

echo "========== 0. 进入项目目录 =========="
cd "$BASE_DIR" || exit 1

echo "========== Java 环境 =========="
echo "若依后端 Java 8：$RUOYI_JAVA_HOME"
echo "RAG Server Java 17：$RAG_JAVA_HOME"

echo "========== 1. 启动 Docker Desktop =========="
if ! docker info >/dev/null 2>&1; then
  echo "Docker 没启动，正在打开 Docker Desktop..."
  open -a Docker
  for i in {1..60}; do
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

echo "========== 2. 启动 MariaDB =========="
brew services start mariadb >/dev/null 2>&1 || true

echo "========== 3. 启动 Redis =========="
brew services start redis >/dev/null 2>&1 || true

echo "========== 4. 启动 Docker 容器 =========="
if docker info >/dev/null 2>&1; then
  docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "rag-|milvus|minio|etcd|attu|NAMES" || true
  for name in rag-etcd rag-minio rag-milvus rag-attu; do
    if docker ps -a --format "{{.Names}}" | grep -qx "$name"; then
      echo "启动容器：$name"
      docker start "$name" >/dev/null 2>&1 || true
    fi
  done
fi

echo "========== 5. 启动 RAG Server 8081 =========="
if is_listen 8081; then
  echo "RAG Server 8081 已经启动，跳过"
else
  if [ ! -f "$RAG_SERVER_DIR/pom.xml" ]; then
    echo "未找到 RAG Server：$RAG_SERVER_DIR"
  else
    echo "使用 RAG Server：$RAG_SERVER_DIR"

    if [ -f "$RAG_SERVER_DIR/src/main/resources/application.yml" ]; then
      echo "修正 RAG Server 数据库连接为 ry-vue-320"
      sed -i '' 's#jdbc:mariadb://localhost:3306/ruoyi?#jdbc:mariadb://localhost:3306/ry-vue-320?#g' "$RAG_SERVER_DIR/src/main/resources/application.yml"
      sed -i '' 's#jdbc:mysql://localhost:3306/ruoyi?#jdbc:mysql://localhost:3306/ry-vue-320?#g' "$RAG_SERVER_DIR/src/main/resources/application.yml"
    fi

    echo "删除 RAG Server target，避免读取旧 application.yml"
    rm -rf "$RAG_SERVER_DIR/target"

    cd "$RAG_SERVER_DIR" || exit 1
    JAVA_HOME="$RAG_JAVA_HOME" PATH="$RAG_JAVA_HOME/bin:$PATH" nohup mvn spring-boot:run > "$LOG_DIR/rag_server.log" 2>&1 &
    cd "$BASE_DIR" || exit 1
  fi
fi

wait_port 8081 "RAG Server" 45

echo "========== 6. 启动若依后端 8080 =========="
if is_listen 8080; then
  echo "若依后端 8080 已经启动，跳过"
else
  cd "$BASE_DIR" || exit 1
  JAVA_HOME="$RUOYI_JAVA_HOME" PATH="$RUOYI_JAVA_HOME/bin:$PATH" nohup mvn -pl ruoyi-admin spring-boot:run > "$LOG_DIR/ruoyi_backend.log" 2>&1 &
fi

wait_port 8080 "若依后端" 60
wait_http "http://localhost:8080/captchaImage" "若依后端 captchaImage" 30

echo "========== 7. 启动若依前端 1024 =========="
if is_listen 1024 && curl -s --connect-timeout 2 http://localhost:1024 >/dev/null 2>&1; then
  echo "若依前端 1024 已经启动并可访问，跳过"
else
  echo "若依前端未真正可访问，重新启动前端"
  pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true
  sleep 2

  cd "$BASE_DIR/ruoyi-ui" || exit 1
  nohup npm run dev > "$LOG_DIR/ruoyi_frontend.log" 2>&1 &
  cd "$BASE_DIR" || exit 1
fi

wait_port 1024 "若依前端" 60
wait_http "http://localhost:1024" "若依前端页面" 60

echo "========== 8. 最终端口 LISTEN 检查 =========="
echo "8080 若依后端："
show_listen 8080

echo "8081 RAG Server："
show_listen 8081

echo "1024 若依前端："
show_listen 1024

echo "9000 MinIO API："
show_listen 9000

echo "9001 MinIO Console："
show_listen 9001

echo "19530 Milvus："
show_listen 19530

echo "========== 9. 检查 RAG Server 运行时数据库配置 =========="
if [ -f "$RAG_SERVER_DIR/target/classes/application.yml" ]; then
  grep -n "jdbc:mariadb\|jdbc:mysql" "$RAG_SERVER_DIR/target/classes/application.yml" || true
else
  echo "RAG Server target/classes/application.yml 暂未生成"
fi

echo "========== 10. 访问地址 =========="
echo "若依前端：http://localhost:1024"
echo "若依后端：http://localhost:8080"
echo "RAG Server：http://localhost:8081"
echo "MinIO 控制台：http://localhost:9001"

echo "========== 11. 日志位置 =========="
echo "若依后端日志：$LOG_DIR/ruoyi_backend.log"
echo "若依前端日志：$LOG_DIR/ruoyi_frontend.log"
echo "RAG Server 日志：$LOG_DIR/rag_server.log"

echo "========== 12. 自动打开若依前端 =========="
open "http://localhost:1024" >/dev/null 2>&1 || true

echo "========== 启动完成 =========="
