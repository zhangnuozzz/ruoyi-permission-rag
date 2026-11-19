#!/bin/bash
set -euo pipefail

artifact_path="${1:?请指定平台后端包}"
jar_command="${JAVA_HOME:?请设置平台Java环境}/bin/jar"

if [ ! -f "$artifact_path" ] || [ ! -x "$jar_command" ]; then
  echo "平台后端包或Java归档工具不可用。" >&2
  exit 1
fi

case "$artifact_path" in
  /*) ;;
  *) artifact_path="$(cd "$(dirname "$artifact_path")" && pwd)/$(basename "$artifact_path")" ;;
esac

artifact_entries="$("$jar_command" tf "$artifact_path")"
if ! printf '%s\n' "$artifact_entries" | grep -Fx 'BOOT-INF/classes/com/ruoyi/RuoYiApplication.class' >/dev/null; then
  echo "平台后端包缺少启动类。" >&2
  exit 1
fi

if printf '%s\n' "$artifact_entries" | grep -E '[[:space:]][0-9]+\.class$' >/dev/null; then
  echo "平台后端包含编号编译副本，需要清理后重新构建。" >&2
  exit 1
fi

inspection_dir="$(mktemp -d "${TMPDIR:-/tmp}/platform-jar-check.XXXXXX")"
cleanup_inspection() {
  inspection_status=$?
  trap - EXIT
  if ! rm -rf -- "$inspection_dir"; then
    if [ "$inspection_status" -eq 0 ]; then
      inspection_status=1
    fi
  fi
  exit "$inspection_status"
}
trap cleanup_inspection EXIT

for module_name in ruoyi-common ruoyi-framework ruoyi-system ruoyi-quartz ruoyi-generator; do
  module_entries="$(printf '%s\n' "$artifact_entries" | grep -E "^BOOT-INF/lib/${module_name}-[^/]+\.jar$" || true)"
  module_count="$(printf '%s\n' "$module_entries" | awk 'NF { count++ } END { print count+0 }')"
  if [ "$module_count" -ne 1 ]; then
    echo "平台模块缺失或重复：${module_name}。" >&2
    exit 1
  fi
  if ! printf '%s\n' "$module_entries" | grep -E "^BOOT-INF/lib/${module_name}-[A-Za-z0-9._-]+\.jar$" >/dev/null; then
    echo "平台模块归档名称无效：${module_name}。" >&2
    exit 1
  fi
  (
    cd "$inspection_dir"
    "$jar_command" xf "$artifact_path" "$module_entries"
  )
  module_contents="$("$jar_command" tf "$inspection_dir/$module_entries")"
  if printf '%s\n' "$module_contents" | grep -E '[[:space:]][0-9]+\.class$' >/dev/null; then
    echo "平台模块含编号编译副本：${module_name}。" >&2
    exit 1
  fi
done
