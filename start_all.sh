#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$BASE_DIR/logs-local"
RUN_DIR="$BASE_DIR/.run-local"
RAG_SERVER_DIR="$BASE_DIR/rag_server"
RUOYI_JAVA_HOME="${JAVA8_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
RAG_JAVA_HOME="${JAVA17_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
MAVEN_REPO="$BASE_DIR/.m2/repository"

mkdir -p "$LOG_DIR" "$RUN_DIR" "$BASE_DIR/uploadPath" "$MAVEN_REPO"
cd "$BASE_DIR"

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

  echo "$name $port 启动超时，请查看 $LOG_DIR 日志"
  return 1
}

wait_http() {
  local url="$1"
  local name="$2"
  local max="${3:-60}"

  for i in $(seq 1 "$max"); do
    # 本机健康检查不能继承桌面代理，否则 localhost/127.0.0.1 可能被发往代理端口。
    if curl --noproxy '*' -fsS --connect-timeout 2 --max-time 5 "$url" >/dev/null 2>&1; then
      echo "$name 可访问：$url"
      return 0
    fi
    echo "等待 $name 可访问中... $i"
    sleep 2
  done

  echo "$name 访问超时：$url"
  return 1
}

wait_cmd() {
  local name="$1"
  local max="$2"
  shift 2

  for i in $(seq 1 "$max"); do
    if "$@" >/dev/null 2>&1; then
      echo "$name 已就绪"
      return 0
    fi
    echo "等待 $name 就绪中... $i"
    sleep 2
  done

  echo "$name 就绪超时"
  return 1
}

compose() {
  docker compose -f "$BASE_DIR/docker-compose.rag.yml" "$@"
}

kill_port() {
  lsof -tiTCP:"$1" -sTCP:LISTEN | xargs -r kill -9 || true
}

start_service() {
  local pid_file="$1"
  shift

  nohup "$@" &
  echo $! > "$pid_file"
}

assert_running() {
  local pid_file="$1"
  local name="$2"
  local pid
  pid="$(cat "$pid_file")"
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "错误：$name 进程已退出，请查看 $LOG_DIR 下对应日志"
    return 1
  fi
}

echo "========== 1. 清理旧服务 =========="
kill_port 8080
kill_port 8081
kill_port 1024
pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true
sleep 2

echo "========== 2. Java 环境检查 =========="
echo "若依主体 Java 8：$RUOYI_JAVA_HOME"
echo "RAG Server Java 17：$RAG_JAVA_HOME"

if [ ! -x "$RUOYI_JAVA_HOME/bin/java" ]; then
  echo "错误：若依主体必须使用 Java 8，请安装 Java 8 或设置 JAVA8_HOME"
  exit 1
fi

if [ ! -x "$RAG_JAVA_HOME/bin/java" ]; then
  echo "错误：RAG Server 必须使用 Java 17，请安装 Java 17 或设置 JAVA17_HOME"
  exit 1
fi

"$RUOYI_JAVA_HOME/bin/java" -version
"$RAG_JAVA_HOME/bin/java" -version

echo "========== 3. 启动 Docker 依赖 =========="
if ! docker info >/dev/null 2>&1; then
  echo "错误：Docker 不可用或当前用户无权访问 Docker socket"
  exit 1
fi

compose up -d

wait_cmd "MariaDB" 60 docker exec rag-mariadb mariadb-admin -uroot -p001106 ping
wait_cmd "Redis" 30 redis-cli -h 127.0.0.1 -p 6380 ping
wait_cmd "Milvus" 90 docker exec rag-milvus bash -lc "test -S /run/milvus/standalone.sock || pgrep -f 'milvus run standalone'"
wait_http "http://localhost:9000/minio/health/live" "MinIO 健康检查" 60

echo "========== 4. 构建后端产物 =========="
JAVA_HOME="$RUOYI_JAVA_HOME" PATH="$RUOYI_JAVA_HOME/bin:$PATH" mvn -q -Dmaven.repo.local="$MAVEN_REPO" -DskipTests package
cd "$RAG_SERVER_DIR"
JAVA_HOME="$RAG_JAVA_HOME" PATH="$RAG_JAVA_HOME/bin:$PATH" mvn -q -Dmaven.repo.local="$MAVEN_REPO" -DskipTests package
cd "$BASE_DIR"

echo "========== 5. 启动 RAG Server 8081 =========="
start_service "$RUN_DIR/rag_server.pid" \
  env JAVA_HOME="$RAG_JAVA_HOME" PATH="$RAG_JAVA_HOME/bin:$PATH" \
  java -jar "$RAG_SERVER_DIR/target/rag-server-1.0.0.jar" --server.address=0.0.0.0 \
  > "$LOG_DIR/rag_server.log" 2>&1

wait_port 8081 "RAG Server" 60
wait_http "http://localhost:8081/" "RAG Server 健康入口" 30
assert_running "$RUN_DIR/rag_server.pid" "RAG Server"

echo "========== 6. 启动若依后端 8080 =========="
start_service "$RUN_DIR/ruoyi_backend.pid" \
  env JAVA_HOME="$RUOYI_JAVA_HOME" PATH="$RUOYI_JAVA_HOME/bin:$PATH" \
  java -jar "$BASE_DIR/ruoyi-admin/target/ruoyi-admin.jar" --server.address=0.0.0.0 \
  > "$LOG_DIR/ruoyi_backend.log" 2>&1

wait_port 8080 "若依后端" 60
wait_http "http://localhost:8080/captchaImage" "若依后端 captchaImage" 30
wait_http "http://localhost:8080/" "若依后端前端跳转入口" 30
assert_running "$RUN_DIR/ruoyi_backend.pid" "若依后端"

echo "========== 7. 启动若依前端 1024 =========="
cd "$BASE_DIR/ruoyi-ui"
start_service "$RUN_DIR/ruoyi_frontend.pid" \
  env NO_PROXY='*' no_proxy='*' npm run dev -- --host 0.0.0.0 --port 1024 \
  > "$LOG_DIR/ruoyi_frontend.log" 2>&1
cd "$BASE_DIR"

wait_port 1024 "若依前端" 60
wait_http "http://localhost:1024" "若依前端页面" 60
wait_http "http://localhost:1024/dev-api/captchaImage" "前端到后端代理" 30
assert_running "$RUN_DIR/ruoyi_frontend.pid" "若依前端"

echo "========== 8. 启动完成 =========="
echo "若依前端：http://localhost:1024"
echo "若依后端：http://localhost:8080"
echo "RAG Server：http://localhost:8081"
echo "MinIO 控制台：http://localhost:9001"
echo "日志目录：$LOG_DIR"
echo "如果在 WSL 外的浏览器访问，请优先使用 http://localhost:1024，并将 localhost/127.0.0.1 加入浏览器代理绕过列表。"
