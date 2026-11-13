#!/usr/bin/env bash
# YunX-Allsystem：Debian / Linux 构建入口。
# 用法: ./run.sh [build|run|package|deb]
#   build    只编译（等价于上游 .\run.ps1 build）
#   run      编译并启动（需要图形会话）
#   package  便携 tar.gz
#   deb      便携 tar.gz + Debian amd64 软件包
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" && -x /usr/lib/jvm/java-17-openjdk-amd64/bin/java ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

CMD="${1:-build}"
shift || true

case "$CMD" in
  build)
    echo ">>> Building YunX-Allsystem (compile only)..."
    ./gradlew :desktop:jar --console=plain "$@"
    ;;
  run)
    echo ">>> Compiling and launching YunX-Allsystem..."
    ./gradlew :desktop:run --console=plain "$@"
    ;;
  package)
    exec "$ROOT/packaging/package-linux.sh" "$@"
    ;;
  deb)
    exec "$ROOT/packaging/package-linux.sh" --deb "$@"
    ;;
  *)
    echo "用法: ./run.sh [build|run|package|deb]" >&2
    exit 2
    ;;
esac
