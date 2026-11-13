# macOS

macOS 包走现有的 Compose Desktop JVM 应用，不另写一套原生客户端。在 Mac 上 `jpackage` 打出 `.dmg`。

`desktop/build.gradle.kts` 在 macOS 上把 `nativeDistributions` 的格式设为 `TargetFormat.Dmg`，并设置：

- `packageName`：`YunX-Allsystem`
- `bundleID`：`com.yunx.allsystem`
- `dockName`：`YunX`

Linux 构建仍打 `.deb`，Windows 仍打 `.exe`。

```bash
# 必须在 macOS 上执行。需要 JDK 17（含 jpackage）。
./packaging/package-macos.sh
```

产物在 `desktop/build/compose/binaries/main/dmg/`。

这次没有 macOS 构建机，所以没有 `.dmg` 文件。JCEF 还要换成 `jcef-natives-macos-amd64` 或 `macos-arm64`（`desktop/build.gradle.kts` 已按构建机选择）。在 Apple Silicon 上请用 arm64 的 JDK 构建。
