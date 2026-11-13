# Android（非官方移植）

`android/` 是官方安卓应用 [CYQawa/YunX](https://github.com/CYQawa/YunX) 的一份拷贝，导入提交 `cc9afe24c9409b239c361263b976f7aad77d1d9e`（`master`）。协议仍是 GNU AGPL-3.0。它不是官方安卓发行版。

桌面端用的是 JCEF + Compose Desktop，不能装到 Android 上。安卓端保持官方的 Jetpack Compose 工程，只改了项目地址：引导页、关于页、设置页显示「您正在使用移植版本，项目地址：https://github.com/gopk-core/YunX-Allsystem」，点击打开本仓库。官方仓库仍写在说明里作署名。

## 构建

需要 JDK 17 和 Android SDK（`compileSdk` 以 `android/app/build.gradle.kts` 为准，上游 targetSdk 34）。

```bash
cd android
export ANDROID_HOME="$HOME/Android/Sdk"   # 按本机 SDK 位置改
./gradlew :app:assembleRelease
```

没有发布证书时产物是未签名包：

`android/app/build/outputs/apk/release/app-release-unsigned.apk`

调试包：

```bash
./gradlew :app:assembleDebug
```

本仓库的云端 Linux 环境没有 Android SDK，这次没有打出 APK。
