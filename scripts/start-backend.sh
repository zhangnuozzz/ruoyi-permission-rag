#!/usr/bin/env bash
set -euo pipefail

# MySQL 8本地账号使用caching_sha2_password时需要允许公钥获取
# author: fufu
# date: 2026-05-13 12:37:47 CST
DB_URL='jdbc:mysql://127.0.0.1:3306/ruoyi?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8' \
DB_USERNAME=fufu \
DB_PASSWORD=Eee2.71828 \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6379 \
./scripts/start-backend-helper.sh
