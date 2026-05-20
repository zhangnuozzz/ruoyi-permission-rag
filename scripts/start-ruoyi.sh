#!/bin/bash

cd "$(dirname "$0")/.."

echo "正在启动若依后端服务..."
echo "项目目录：$(pwd)"

export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH=$JAVA_HOME/bin:$PATH

echo "Java 版本："
java -version

mvn -pl ruoyi-admin spring-boot:run
