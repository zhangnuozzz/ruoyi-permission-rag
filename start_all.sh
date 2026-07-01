#!/bin/bash

BASE_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"
LOG_DIR="$BASE_DIR/logs-local"
RAG_SERVER_DIR="$BASE_DIR/RuoYi-Vue-ragold-fufu-week4/rag_server"

RUOYI_JAVA_HOME="$(/usr/libexec/java_home -v 1.8 2>/dev/null || /usr/libexec/java_home -v 8 2>/dev/null)"
RAG_JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)"

mkdir -p "$LOG_DIR"
cd "$BASE_DIR" || exit 1

is_listen() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

wait_port() {
  local port="$1"
  local name="$2"
  local max="${3:-60}"

  for i in $(seq 1 "$max"); do
    if is_listen "$port"; then
      echo "$name $port 已启动"
      return 0
    fi
    echo "等待 $name $port 启动中... $i"
    sleep 2
  done

  echo "$name $port 启动超时，请查看 logs-local 日志"
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

echo "========== 1. 清理旧服务 =========="
lsof -tiTCP:8080 -sTCP:LISTEN | xargs -r kill -9
lsof -tiTCP:8081 -sTCP:LISTEN | xargs -r kill -9
lsof -tiTCP:1024 -sTCP:LISTEN | xargs -r kill -9
pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true
sleep 2

echo "========== 2. 启动 Docker =========="
if ! docker info >/dev/null 2>&1; then
  echo "Docker 未启动，正在打开 Docker Desktop..."
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

echo "========== 3. 启动 MariaDB / Redis =========="
brew services start mariadb >/dev/null 2>&1 || true
brew services start redis >/dev/null 2>&1 || true

echo "========== 4. 启动 RAG 依赖容器 =========="
if docker info >/dev/null 2>&1; then
  for name in rag-etcd rag-minio rag-milvus rag-attu; do
    if docker ps -a --format "{{.Names}}" | grep -qx "$name"; then
      echo "启动容器：$name"
      docker start "$name" >/dev/null 2>&1 || true
    fi
  done
fi


echo "========== Java 环境检查 =========="
echo "若依后端 Java 8：$RUOYI_JAVA_HOME"
echo "RAG Server Java 17：$RAG_JAVA_HOME"

if [ ! -x "$RUOYI_JAVA_HOME/bin/java" ]; then
  echo "错误：若依后端 Java 8 路径不可用：$RUOYI_JAVA_HOME"
  echo "请检查 /usr/libexec/java_home -V 是否能看到 Java 8"
  exit 1
fi

if [ ! -x "$RAG_JAVA_HOME/bin/java" ]; then
  echo "错误：RAG Server Java 17 路径不可用：$RAG_JAVA_HOME"
  echo "请检查 /usr/libexec/java_home -V 是否能看到 Java 17"
  exit 1
fi

echo "========== 5. 启动 RAG Server 8081 =========="
if [ ! -f "$RAG_SERVER_DIR/pom.xml" ]; then
  echo "未找到 RAG Server：$RAG_SERVER_DIR"
  exit 1
fi

if [ -f "$RAG_SERVER_DIR/src/main/resources/application.yml" ]; then
  sed -i '' 's#jdbc:mariadb://localhost:3306/ruoyi?#jdbc:mariadb://localhost:3306/ry-vue-320?#g' "$RAG_SERVER_DIR/src/main/resources/application.yml"
  sed -i '' 's#jdbc:mysql://localhost:3306/ruoyi?#jdbc:mysql://localhost:3306/ry-vue-320?#g' "$RAG_SERVER_DIR/src/main/resources/application.yml"
fi

rm -rf "$RAG_SERVER_DIR/target"

cd "$RAG_SERVER_DIR" || exit 1
JAVA_HOME="$RAG_JAVA_HOME" PATH="$RAG_JAVA_HOME/bin:$PATH" nohup mvn spring-boot:run > "$LOG_DIR/rag_server.log" 2>&1 &
cd "$BASE_DIR" || exit 1

wait_port 8081 "RAG Server" 45

echo "========== 6. 启动若依后端 8080 =========="
JAVA_HOME="$RUOYI_JAVA_HOME" PATH="$RUOYI_JAVA_HOME/bin:$PATH" nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > "$LOG_DIR/ruoyi_backend.log" 2>&1 &

wait_port 8080 "若依后端" 60
wait_http "http://localhost:8080/captchaImage" "若依后端 captchaImage" 30

echo "========== 7. 启动若依前端 1024 =========="
cd "$BASE_DIR/ruoyi-ui" || exit 1
nohup npm run dev > "$LOG_DIR/ruoyi_frontend.log" 2>&1 &
cd "$BASE_DIR" || exit 1

wait_port 1024 "若依前端" 60
wait_http "http://localhost:1024" "若依前端页面" 60

echo "========== 8. 启动完成 =========="
echo "若依前端：http://localhost:1024"
echo "若依后端：http://localhost:8080"
echo "RAG Server：http://localhost:8081"
echo "日志目录：$LOG_DIR"

open "http://localhost:1024" >/dev/null 2>&1 || true
