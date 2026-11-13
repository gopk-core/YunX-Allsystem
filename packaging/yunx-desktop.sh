#!/usr/bin/env bash
# 便携版 / .deb 共用的启动器。工作目录固定为脚本所在目录，方便找到 app/ 与 runtime/。
set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
export YUNX_LAUNCHER="$HERE/yunx-desktop"
cd "$HERE"

if [[ -x "$HERE/runtime/bin/java" ]]; then
  JAVA="$HERE/runtime/bin/java"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="$(command -v java || true)"
fi
if [[ -z "${JAVA:-}" || ! -x "$JAVA" ]]; then
  echo "找不到 java。请安装 openjdk-17-jre，或使用带 runtime/ 的便携包。" >&2
  exit 1
fi

MOD=""
CP=""
shopt -s nullglob
for jar in "$HERE"/app/*.jar; do
  base="$(basename "$jar")"
  case "$base" in
    javafx-*) MOD="${MOD:+$MOD:}$jar" ;;
    *) CP="${CP:+$CP:}$jar" ;;
  esac
done
shopt -u nullglob

if [[ -z "$CP" ]]; then
  echo "app/ 下没有 jar。这个目录不是完整的 YunX-Allsystem 发行包。" >&2
  exit 1
fi

ARGS=()
if [[ -n "$MOD" ]]; then
  ARGS+=(--module-path "$MOD" --add-modules javafx.base,javafx.graphics,javafx.media,javafx.swing)
fi

exec "$JAVA" \
  -Dfile.encoding=UTF-8 \
  -Dsun.jnu.encoding=UTF-8 \
  -Xms64m -Xmx1024m \
  "${ARGS[@]}" \
  -cp "$CP" \
  com.yunx.app.MainKt "$@"
