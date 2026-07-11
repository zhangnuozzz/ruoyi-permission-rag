#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA8_HOME="${JAVA8_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
MAVEN_REPO="$BASE_DIR/.m2/repository"

cd "$BASE_DIR"
mkdir -p "$MAVEN_REPO"

echo "正在启动若依后端服务..."
echo "项目目录：$(pwd)"

if [ ! -x "$JAVA8_HOME/bin/java" ]; then
  echo "错误：若依主体必须使用 Java 8，请安装 Java 8 或设置 JAVA8_HOME"
  exit 1
fi

export JAVA_HOME="$JAVA8_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java 版本："
java -version

mvn -Dmaven.repo.local="$MAVEN_REPO" -pl ruoyi-admin spring-boot:run
