#!/bin/zsh
set -u

PROJECT_DIR="/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"
RAG_DIR="$PROJECT_DIR/RuoYi-Vue-ragold-fufu-week4/rag_server"
LOG_DIR="$PROJECT_DIR/logs-local"

RUOYI_JAVA_HOME=$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)
RAG_JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)

mkdir -p "$LOG_DIR"

echo "========== 0. 进入项目目录 =========="
cd "$PROJECT_DIR" || exit 1

if [ -z "$RUOYI_JAVA_HOME" ]; then
  echo "错误：未找到 Java 8。若依后端必须使用 Java 8。"
  echo "请先安装 Java 8，或确认 /usr/libexec/java_home -v 1.8 能正常输出。"
  exit 1
fi

echo "若依后端固定 Java 8：$RUOYI_JAVA_HOME"

if [ -n "$RAG_JAVA_HOME" ]; then
  echo "RAG Server 优先使用 Java 17：$RAG_JAVA_HOME"
else
  echo "提示：未检测到 Java 17。若 RAG Server 后续重启失败，请安装 Java 17。"
fi

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

if ! docker info >/dev/null 2>&1; then
  echo "错误：Docker 仍未启动，请手动打开 Docker Desktop 后重试。"
  exit 1
fi

echo "========== 2. 启动 MariaDB =========="
brew services start mariadb >/dev/null 2>&1 || true

echo "========== 3. 启动 Redis =========="
brew services start redis >/dev/null 2>&1 || true

echo "========== 4. 启动 Docker 容器 =========="
echo "当前已有容器："
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "rag-|NAMES" || true

for c in rag-etcd rag-minio rag-milvus; do
  if docker ps -a --format '{{.Names}}' | grep -qx "$c"; then
    if docker ps --format '{{.Names}}' | grep -qx "$c"; then
      echo "$c 已经运行"
    else
      echo "启动容器：$c"
      docker start "$c" >/dev/null
    fi
  else
    echo "警告：容器 $c 不存在，跳过"
  fi
done

echo "========== 5. 检查关键端口占用 =========="
for port in 8080 8081 1024 9000 9001 19530; do
  echo "$port："
  lsof -i :"$port" || true
done

echo "========== 6. 定位并启动 RAG Server 8081 =========="
if lsof -i :8081 >/dev/null 2>&1; then
  echo "RAG Server 8081 已经启动，跳过"
else
  if [ -d "$RAG_DIR" ]; then
    echo "使用 RAG Server 目录：$RAG_DIR"
    cd "$RAG_DIR" || exit 1

    echo "修正 RAG Server 数据库连接为 ry-vue-320"
    if [ -f "src/main/resources/application.yml" ]; then
      sed -i '' 's#jdbc:mariadb://localhost:3306/[^?]*#jdbc:mariadb://localhost:3306/ry-vue-320#g' src/main/resources/application.yml 2>/dev/null || true
      sed -i '' 's#jdbc:mysql://localhost:3306/[^?]*#jdbc:mariadb://localhost:3306/ry-vue-320#g' src/main/resources/application.yml 2>/dev/null || true
    fi

    echo "删除 RAG Server target，避免读取旧 application.yml"
    rm -rf target

    if [ -n "$RAG_JAVA_HOME" ]; then
      (
        export JAVA_HOME="$RAG_JAVA_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "RAG Server 实际 Java 版本：" > "$LOG_DIR/rag_server.log"
        java -version >> "$LOG_DIR/rag_server.log" 2>&1
        nohup mvn spring-boot:run >> "$LOG_DIR/rag_server.log" 2>&1 &
      )
    else
      (
        echo "RAG Server 实际 Java 版本：" > "$LOG_DIR/rag_server.log"
        java -version >> "$LOG_DIR/rag_server.log" 2>&1
        nohup mvn spring-boot:run >> "$LOG_DIR/rag_server.log" 2>&1 &
      )
    fi

    echo "RAG Server 启动命令已执行，日志：$LOG_DIR/rag_server.log"
  else
    echo "警告：RAG Server 目录不存在，跳过：$RAG_DIR"
  fi
fi

echo "========== 7. 启动若依后端 8080 =========="
if lsof -i :8080 >/dev/null 2>&1; then
  echo "若依后端 8080 已经启动，跳过"
else
  cd "$PROJECT_DIR" || exit 1
  (
    export JAVA_HOME="$RUOYI_JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "若依后端实际 Java 版本：" > "$LOG_DIR/ruoyi_backend.log"
    java -version >> "$LOG_DIR/ruoyi_backend.log" 2>&1
    nohup mvn -pl ruoyi-admin spring-boot:run >> "$LOG_DIR/ruoyi_backend.log" 2>&1 &
  )
  echo "若依后端启动命令已执行，日志：$LOG_DIR/ruoyi_backend.log"
fi

echo "========== 8. 启动若依前端 1024 =========="
if lsof -i :1024 >/dev/null 2>&1; then
  echo "若依前端 1024 已经启动，跳过"
else
  cd "$PROJECT_DIR/ruoyi-ui" || exit 1
  nohup npm run dev > "$LOG_DIR/ruoyi_frontend.log" 2>&1 &
  echo "若依前端启动命令已执行，日志：$LOG_DIR/ruoyi_frontend.log"
fi

echo "========== 9. 等待服务启动 =========="
for i in {1..45}; do
  if lsof -i :8080 >/dev/null 2>&1 && lsof -i :8081 >/dev/null 2>&1 && lsof -i :1024 >/dev/null 2>&1; then
    echo "关键服务端口已就绪"
    break
  fi
  echo "等待中... $i"
  sleep 2
done

cd "$PROJECT_DIR" || exit 1

echo "========== 10. 最终端口检查 =========="
for port in 8080 8081 1024 9000 9001 19530; do
  echo "$port："
  lsof -i :"$port" || echo "$port 未启动"
done

echo "========== 10.1 检查 RAG Server 运行时数据库配置 =========="
if [ -f "$RAG_DIR/src/main/resources/application.yml" ]; then
  grep -n "jdbc:.*3306" "$RAG_DIR/src/main/resources/application.yml" || true
fi

echo "========== 10.2 检查若依后端接口 =========="
curl -s -i http://localhost:8080/captchaImage | head -n 15 || true

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
open "http://localhost:1024" >/dev/null 2>&1 || true
