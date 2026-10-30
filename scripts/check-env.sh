#!/usr/bin/env bash
set -u

failed=0
unchecked=0
frontend_url="${PLATFORM_FRONTEND_URL:-http://127.0.0.1:1024}"
backend_url="${PLATFORM_BACKEND_URL:-http://127.0.0.1:8080}"
file_host="${PLATFORM_FILE_HOST:-127.0.0.1}"
file_port="${PLATFORM_FILE_PORT:-8081}"

check_http() {
  local url="$1" label="$2" code
  if ! command -v curl >/dev/null 2>&1; then
    echo "${label}：缺少 curl，未检查"
    unchecked=1
    return
  fi
  code=$(curl --silent --output /dev/null --write-out '%{http_code}' --connect-timeout 2 --max-time 4 "$url" 2>/dev/null) || code=000
  case "${code}" in
    2[0-9][0-9]) echo "${label}：HTTP ${code}" ;;
    *) echo "${label}：检查失败（HTTP ${code}）"; failed=1 ;;
  esac
}

check_tcp() {
  local host="$1" port="$2" label="$3"
  case "$port" in ''|*[!0-9]*) echo "${label}：端口配置无效"; failed=1; return ;; esac
  if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ] || [ -z "$host" ]; then
    echo "${label}：地址或端口配置无效"
    failed=1
    return
  fi
  if ! command -v nc >/dev/null 2>&1; then
    echo "${label}：缺少 nc，未检查"
    unchecked=1
  elif nc -z -w 2 "$host" "$port" >/dev/null 2>&1; then
    echo "${label}：TCP 可连接（未验证鉴权及业务）"
  else
    echo "${label}：TCP 连接失败"
    failed=1
  fi
}

check_configured_tcp() {
  local host="$1" port="$2" label="$3"
  if [ -z "$host" ] || [ -z "$port" ]; then
    echo "${label}：未指定实际应用连接目标，未检查"
    unchecked=1
  else
    check_tcp "$host" "$port" "${label}"
  fi
}

printf '%s\n' '平台运行环境检查（只读）'
check_http "${frontend_url%/}/" '前端入口'
check_http "${backend_url%/}/captchaImage" '平台后端入口'
check_tcp "$file_host" "$file_port" '文件服务端口'
check_configured_tcp "${PLATFORM_DB_HOST:-}" "${PLATFORM_DB_PORT:-}" '应用数据库端口'
check_configured_tcp "${PLATFORM_REDIS_HOST:-}" "${PLATFORM_REDIS_PORT:-}" '应用缓存端口'

if [ "$failed" -ne 0 ]; then
  echo '存在连接失败，环境检查未通过。'
  exit 1
fi
if [ "$unchecked" -ne 0 ]; then
  echo '检查不完整，请指定实际连接目标并补齐检查工具。'
  exit 2
fi
echo '所列入口连接检查通过；不替代数据迁移、权限、模型、存储恢复及业务验收。'
