#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$BASE_DIR/logs-local"
RUN_DIR="$BASE_DIR/.run-local"
RAG_SERVER_DIR="$BASE_DIR/rag_server"

mkdir -p "$LOG_DIR" "$RUN_DIR" "$BASE_DIR/uploadPath"

cd "$BASE_DIR"

# ============================================================
# Java 环境自动识别
# ============================================================

detect_java8() {
  if [ -n "${JAVA8_HOME:-}" ] && [ -x "$JAVA8_HOME/bin/java" ]; then
    echo "$JAVA8_HOME"
    return
  fi

  if [ "$(uname -s)" = "Darwin" ]; then
    /usr/libexec/java_home -v 1.8 2>/dev/null && return
  fi

  if [ -x "/usr/lib/jvm/java-8-openjdk-amd64/bin/java" ]; then
    echo "/usr/lib/jvm/java-8-openjdk-amd64"
    return
  fi

  return 1
}

detect_java17() {
  if [ -n "${JAVA17_HOME:-}" ] && [ -x "$JAVA17_HOME/bin/java" ]; then
    echo "$JAVA17_HOME"
    return
  fi

  if [ "$(uname -s)" = "Darwin" ]; then
    /usr/libexec/java_home -v 17 2>/dev/null && return
  fi

  if [ -x "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" ]; then
    echo "/usr/lib/jvm/java-17-openjdk-amd64"
    return
  fi

  return 1
}

RUOYI_JAVA_HOME="$(detect_java8 || true)"
RAG_JAVA_HOME="$(detect_java17 || true)"

# ============================================================
# 基础函数
# ============================================================

is_listen() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

kill_port() {
  local port="$1"
  local pids

  pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"

  if [ -n "$pids" ]; then
    echo "$pids" | xargs kill -9 >/dev/null 2>&1 || true
  fi
}

wait_port() {
  local port="$1"
  local name="$2"
  local max="${3:-40}"

  for i in $(seq 1 "$max"); do
    if is_listen "$port"; then
      echo "✅ $name $port 已启动"
      return 0
    fi

    sleep 1
  done

  echo "❌ $name $port 启动超时"
  return 1
}

wait_http() {
  local url="$1"
  local name="$2"
  local max="${3:-40}"

  for i in $(seq 1 "$max"); do
    if curl --noproxy '*' -fsS \
      --connect-timeout 2 \
      --max-time 5 \
      "$url" >/dev/null 2>&1; then

      echo "✅ $name 可访问：$url"
      return 0
    fi

    sleep 1
  done

  echo "❌ $name 访问超时：$url"
  return 1
}

wait_cmd() {
  local name="$1"
  local max="$2"

  shift 2

  for i in $(seq 1 "$max"); do
    if "$@" >/dev/null 2>&1; then
      echo "✅ $name 已就绪"
      return 0
    fi

    sleep 1
  done

  echo "❌ $name 就绪超时"
  return 1
}

compose() {
  docker compose -f "$BASE_DIR/docker-compose.rag.yml" "$@"
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

  if [ ! -f "$pid_file" ]; then
    echo "❌ $name PID 文件不存在"
    return 1
  fi

  local pid
  pid="$(cat "$pid_file")"

  if ! kill -0 "$pid" >/dev/null 2>&1; then
    echo "❌ $name 已退出，请查看 $LOG_DIR"
    return 1
  fi
}

# ============================================================
# 判断是否需要重新构建
# ============================================================

ruoyi_need_build() {
  local jar="$BASE_DIR/ruoyi-admin/target/ruoyi-admin.jar"

  if [ ! -f "$jar" ]; then
    return 0
  fi

  if find \
      "$BASE_DIR/ruoyi-admin/src" \
      "$BASE_DIR/ruoyi-common/src" \
      "$BASE_DIR/ruoyi-framework/src" \
      "$BASE_DIR/ruoyi-system/src" \
      "$BASE_DIR/ruoyi-quartz/src" \
      "$BASE_DIR/ruoyi-generator/src" \
      -type f -newer "$jar" -print -quit 2>/dev/null | grep -q .; then
    return 0
  fi

  if [ "$BASE_DIR/pom.xml" -nt "$jar" ]; then
    return 0
  fi

  return 1
}

rag_need_build() {
  local jar="$RAG_SERVER_DIR/target/rag-server-1.0.0.jar"

  if [ ! -f "$jar" ]; then
    return 0
  fi

  if find \
      "$RAG_SERVER_DIR/src" \
      -type f -newer "$jar" -print -quit 2>/dev/null | grep -q .; then
    return 0
  fi

  if [ "$RAG_SERVER_DIR/pom.xml" -nt "$jar" ]; then
    return 0
  fi

  return 1
}

# ============================================================
# 1. 清理旧进程
# ============================================================

echo "========== 1. 清理旧服务 =========="

kill_port 1024
kill_port 8080
kill_port 8081

pkill -f "vue-cli-service serve" >/dev/null 2>&1 || true

rm -f \
  "$RUN_DIR/ruoyi_frontend.pid" \
  "$RUN_DIR/ruoyi_backend.pid" \
  "$RUN_DIR/rag_server.pid"

sleep 1

# ============================================================
# 2. Java 环境
# ============================================================

echo "========== 2. Java 环境检查 =========="

if [ -z "$RUOYI_JAVA_HOME" ] || [ ! -x "$RUOYI_JAVA_HOME/bin/java" ]; then
  echo "❌ 未找到 Java 8"
  echo "请安装 Java 8 或设置 JAVA8_HOME"
  exit 1
fi

if [ -z "$RAG_JAVA_HOME" ] || [ ! -x "$RAG_JAVA_HOME/bin/java" ]; then
  echo "❌ 未找到 Java 17"
  echo "请安装 Java 17 或设置 JAVA17_HOME"
  exit 1
fi

echo "Java 8 ：$RUOYI_JAVA_HOME"
echo "Java 17：$RAG_JAVA_HOME"

# ============================================================
# 3. 本机若依依赖
# ============================================================

echo "========== 3. 检查若依本机数据库和 Redis =========="

if ! lsof -nP -iTCP:3306 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "MariaDB 3306 未启动，尝试通过 Homebrew 启动..."

  if command -v brew >/dev/null 2>&1; then
    brew services start mariadb >/dev/null 2>&1 || true
  fi
fi

if ! lsof -nP -iTCP:6379 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Redis 6379 未启动，尝试通过 Homebrew 启动..."

  if command -v brew >/dev/null 2>&1; then
    brew services start redis >/dev/null 2>&1 || true
  fi
fi

for i in $(seq 1 15); do
  if lsof -nP -iTCP:3306 -sTCP:LISTEN >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

for i in $(seq 1 15); do
  if lsof -nP -iTCP:6379 -sTCP:LISTEN >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! lsof -nP -iTCP:3306 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "❌ MariaDB 3306 未启动"
  exit 1
fi

if ! lsof -nP -iTCP:6379 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "❌ Redis 6379 未启动"
  exit 1
fi

echo "✅ MariaDB 3306 已就绪"
echo "✅ Redis 6379 已就绪"

# ============================================================
# 4. Docker RAG 环境
# ============================================================

echo "========== 4. 启动 RAG Docker 依赖 =========="

if ! docker info >/dev/null 2>&1; then
  if [ "$(uname -s)" = "Darwin" ]; then
    echo "Docker Desktop 未启动，正在打开..."
    open -a Docker >/dev/null 2>&1 || true

    echo "等待 Docker Desktop..."

    for i in $(seq 1 60); do
      if docker info >/dev/null 2>&1; then
        break
      fi
      sleep 2
    done
  fi
fi

if ! docker info >/dev/null 2>&1; then
  echo "❌ Docker 不可用"
  exit 1
fi

compose up -d

wait_cmd \
  "RAG MariaDB 3307" \
  30 \
  docker exec rag-mariadb mariadb-admin -uroot -p001106 ping

wait_cmd \
  "RAG Redis 6380" \
  30 \
  redis-cli -h 127.0.0.1 -p 6380 ping

wait_cmd \
  "Milvus" \
  60 \
  docker exec rag-milvus \
  bash -lc "test -S /run/milvus/standalone.sock || pgrep -f 'milvus run standalone'"

wait_http \
  "http://localhost:9000/minio/health/live" \
  "MinIO" \
  30

# ============================================================
# 5. 仅在需要时构建
# ============================================================

echo "========== 5. 检查后端产物 =========="

if ruoyi_need_build; then
  echo "检测到若依代码有更新，正在构建..."

  JAVA_HOME="$RUOYI_JAVA_HOME" \
  PATH="$RUOYI_JAVA_HOME/bin:$PATH" \
  mvn -DskipTests package

  echo "✅ 若依构建完成"
else
  echo "✅ 若依 jar 已是最新，跳过构建"
fi

if rag_need_build; then
  echo "检测到 RAG Server 代码有更新，正在构建..."

  cd "$RAG_SERVER_DIR"

  JAVA_HOME="$RAG_JAVA_HOME" \
  PATH="$RAG_JAVA_HOME/bin:$PATH" \
  mvn -DskipTests package

  cd "$BASE_DIR"

  echo "✅ RAG Server 构建完成"
else
  echo "✅ RAG Server jar 已是最新，跳过构建"
fi

# ============================================================
# 6. RAG Server
# ============================================================

echo "========== 6. 启动 RAG Server 8081 =========="

start_service \
  "$RUN_DIR/rag_server.pid" \
  env \
  JAVA_HOME="$RAG_JAVA_HOME" \
  PATH="$RAG_JAVA_HOME/bin:$PATH" \
  java \
  -jar \
  "$RAG_SERVER_DIR/target/rag-server-1.0.0.jar" \
  --server.address=0.0.0.0 \
  > "$LOG_DIR/rag_server.log" 2>&1

wait_port 8081 "RAG Server" 40
assert_running "$RUN_DIR/rag_server.pid" "RAG Server"

# ============================================================
# 7. 若依后端
# ============================================================

echo "========== 7. 启动若依后端 8080 =========="

start_service \
  "$RUN_DIR/ruoyi_backend.pid" \
  env \
  JAVA_HOME="$RUOYI_JAVA_HOME" \
  PATH="$RUOYI_JAVA_HOME/bin:$PATH" \
  java \
  -jar \
  "$BASE_DIR/ruoyi-admin/target/ruoyi-admin.jar" \
  --server.address=0.0.0.0 \
  > "$LOG_DIR/ruoyi_backend.log" 2>&1

wait_port 8080 "若依后端" 40

wait_http \
  "http://localhost:8080/captchaImage" \
  "若依后端" \
  30

assert_running \
  "$RUN_DIR/ruoyi_backend.pid" \
  "若依后端"

# ============================================================
# 8. 前端
# ============================================================

echo "========== 8. 启动若依前端 1024 =========="

cd "$BASE_DIR/ruoyi-ui"

if [ ! -d node_modules ]; then
  echo "首次启动，安装前端依赖..."
  npm install
fi

start_service \
  "$RUN_DIR/ruoyi_frontend.pid" \
  env \
  NO_PROXY='*' \
  no_proxy='*' \
  npm run dev \
  -- \
  --host 0.0.0.0 \
  --port 1024 \
  > "$LOG_DIR/ruoyi_frontend.log" 2>&1

cd "$BASE_DIR"

wait_port 1024 "若依前端" 40

wait_http \
  "http://localhost:1024" \
  "若依前端" \
  40

assert_running \
  "$RUN_DIR/ruoyi_frontend.pid" \
  "若依前端"

# ============================================================
# 9. 完成
# ============================================================

echo
echo "============================================"
echo "              ✅ 系统启动完成"
echo "============================================"
echo
echo "若依前端：    http://localhost:1024"
echo "若依后端：    http://localhost:8080"
echo "RAG Server：  http://localhost:8081"
echo "MinIO：       http://localhost:9001"
echo
echo "若依数据库：  localhost:3306 / ry-vue-320"
echo "RAG数据库：   localhost:3307"
echo "Milvus：      localhost:19530"
echo
echo "日志目录："
echo "$LOG_DIR"
echo
echo "============================================"

if [ "$(uname -s)" = "Darwin" ]; then
  open "http://localhost:1024" >/dev/null 2>&1 || true
fi
