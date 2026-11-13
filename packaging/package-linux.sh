#!/usr/bin/env bash
# 打 Debian amd64 便携 tar.gz，可选再打 .deb。
# 产物在 release/ 下，不进 git。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
WITH_DEB=0
if [[ "${1:-}" == "--deb" ]]; then
  WITH_DEB=1
fi

if [[ -z "${JAVA_HOME:-}" && -x /usr/lib/jvm/java-17-openjdk-amd64/bin/jlink ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "需要带 jlink 的 JDK 17（设置 JAVA_HOME）。" >&2
  exit 1
fi

VERSION="$(head -n 1 version.txt | tr -d '[:space:]')"
NAME="YunX-Allsystem"
STAGE="$(mktemp -d)"
APP="$STAGE/$NAME"
trap 'rm -rf "$STAGE"' EXIT

echo ">>> Compiling and resolving runtime classpath..."
LOG="$(mktemp)"
./gradlew :desktop:jar :desktop:printRuntimeClasspath --console=plain >"$LOG"
CP_LINE="$(grep -E '^RUNTIME_CP=' "$LOG" | tail -n 1 || true)"
rm -f "$LOG"
if [[ -z "$CP_LINE" ]]; then
  echo "没有拿到 RUNTIME_CP。Gradle 输出里应该有这一行。" >&2
  exit 1
fi
IFS=':' read -r -a DEP_JARS <<< "${CP_LINE#RUNTIME_CP=}"

mkdir -p "$APP/app" "$ROOT/release"
cp -a packaging/yunx-desktop.sh "$APP/yunx-desktop"
chmod 755 "$APP/yunx-desktop"

shopt -s nullglob
cp -a desktop/build/libs/*.jar "$APP/app/"
shopt -u nullglob
for jar in "${DEP_JARS[@]}"; do
  [[ -f "$jar" ]] || continue
  cp -a "$jar" "$APP/app/"
done

echo ">>> jlink runtime (JDK 17)..."
"$JAVA_HOME/bin/jlink" \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules java.base,java.datatransfer,java.xml,java.prefs,java.desktop,java.logging,jdk.crypto.ec,java.sql,java.naming,jdk.httpserver,jdk.unsupported,jdk.unsupported.desktop,jdk.charsets,jdk.localedata,java.management,jdk.zipfs \
  --strip-debug --no-header-files --no-man-pages --compress=2 \
  --output "$APP/runtime"

ICON_SRC="desktop/src/main/resources/icon.png"
if [[ -f "$ICON_SRC" ]]; then
  mkdir -p "$APP/share/icons/hicolor/128x128/apps"
  cp -a "$ICON_SRC" "$APP/share/icons/hicolor/128x128/apps/yunx-desktop.png"
fi

cat >"$APP/README-LINUX.txt" <<EOF
YunX-Allsystem ${VERSION}
Unofficial Debian/Linux port of https://github.com/tidain/YunX-Desktop
License: GNU AGPL-3.0. See LICENSE in the source repository.

Run: ./yunx-desktop
Data: ~/.local/share/yunx-desktop
Cache: ~/.cache/yunx-desktop
Config: ~/.config/yunx-desktop

Before the first launch, install Chromium/JCEF libraries. See docs/LINUX.md.
EOF
cp -a LICENSE "$APP/LICENSE"

TAR="$ROOT/release/${NAME}-${VERSION}-linux-amd64.tar.gz"
tar -C "$STAGE" -czf "$TAR" "$NAME"
echo ">>> Portable tarball: $TAR"

if [[ "$WITH_DEB" != 1 ]]; then
  exit 0
fi

echo ">>> Building .deb..."
DEB_ROOT="$(mktemp -d)"
trap 'rm -rf "$STAGE" "$DEB_ROOT"' EXIT
mkdir -p "$DEB_ROOT/opt" "$DEB_ROOT/usr/bin" "$DEB_ROOT/usr/share/applications" \
  "$DEB_ROOT/usr/share/icons/hicolor/128x128/apps" "$DEB_ROOT/usr/share/doc/yunx-allsystem" \
  "$DEB_ROOT/DEBIAN"
cp -a "$APP" "$DEB_ROOT/opt/yunx-allsystem"
ln -s ../../opt/yunx-allsystem/yunx-desktop "$DEB_ROOT/usr/bin/yunx-desktop"
if [[ -f "$APP/share/icons/hicolor/128x128/apps/yunx-desktop.png" ]]; then
  cp -a "$APP/share/icons/hicolor/128x128/apps/yunx-desktop.png" \
    "$DEB_ROOT/usr/share/icons/hicolor/128x128/apps/yunx-desktop.png"
fi
cp -a LICENSE "$DEB_ROOT/usr/share/doc/yunx-allsystem/copyright"

cat >"$DEB_ROOT/usr/share/applications/yunx-desktop.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=云析 YunX
GenericName=网盘下载
Comment=Unofficial multi-system port of tidain/YunX-Desktop
Exec=/opt/yunx-allsystem/yunx-desktop
Icon=yunx-desktop
Terminal=false
Categories=Network;FileTransfer;
StartupWMClass=云析 YunX-Desktop
EOF

# Debian 13 (trixie) 的 t64 库名。应用自带 jlink 运行时，不再依赖系统 JRE。
cat >"$DEB_ROOT/DEBIAN/control" <<EOF
Package: yunx-allsystem
Version: ${VERSION}
Section: net
Priority: optional
Architecture: amd64
Maintainer: YunX-Allsystem <https://github.com/gopk-core/YunX-Allsystem>
Depends: libnss3, libnspr4, libatk1.0-0t64, libatk-bridge2.0-0t64, libcups2t64, libdrm2, libgbm1, libgtk-3-0t64, libglib2.0-0t64, libpango-1.0-0, libcairo2, libasound2t64, libx11-6, libx11-xcb1, libxcb1, libxext6, libxfixes3, libxdamage1, libxcomposite1, libxrandr2, libxkbcommon0, libxshmfence1, libxss1, libxcursor1, libxi6, libxtst6, fonts-liberation, xdg-utils
Recommends: libnotify-bin, libsecret-tools, ffmpeg
Homepage: https://github.com/gopk-core/YunX-Allsystem
Description: Unofficial Debian port of YunX-Desktop
 YunX-Allsystem is an unofficial multi-system port of the official Windows
 client tidain/YunX-Desktop. It is not the official Windows release.
 License: GNU AGPL-3.0.
EOF

DEB="$ROOT/release/${NAME,,}_${VERSION}_amd64.deb"
dpkg-deb --root-owner-group --build "$DEB_ROOT" "$DEB"
echo ">>> Debian package: $DEB"
