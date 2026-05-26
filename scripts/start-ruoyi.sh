#!/bin/bash

cd "$(dirname "$0")/.."

echo "正在启动若依后端服务..."
echo "项目目录：$(pwd)"

export PATH=$JAVA_HOME/bin:$PATH

echo "Java 版本："
java -version
# 1. 在项目根目录（RAGproject）重新编译所有模块
mvn clean install -U -DskipTests

# 2. 进入 Admin 模块启动后端
cd ruoyi-admin
mvn spring-boot:run
