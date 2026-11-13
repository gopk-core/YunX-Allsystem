#!/usr/bin/env bash
# Build a macOS .dmg from the Compose Desktop app.
# jpackage only produces a .dmg on macOS. This script refuses to pretend otherwise.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "macOS .dmg packages have to be built on a Mac (JDK 17 with jpackage)." >&2
  echo "See docs/MACOS.md." >&2
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  fi
fi

exec ./gradlew :desktop:packageDistributionForCurrentOS --console=plain "$@"
