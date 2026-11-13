# iOS

这一轮没有做 iOS 应用，也没有放一个不能运行的占位工程。

官方云析没有 iOS 客户端。本仓库的桌面端是 Compose Desktop + JCEF（内嵌 Chromium）+ JavaFX，只能跑在桌面 JVM 上。这些都进不了 iOS：

- App Store 不允许把一套 JVM 和 Chromium（JCEF）嵌进 iOS 应用。
- JCEF / JavaFX 没有 iOS 后端。
- 官方安卓工程是 Jetpack Compose + Android SDK（WebView、Room、Media3），不能直接编成 iOS。
- Compose Multiplatform 的 iOS 目标要重写界面和系统接口（登录 WebView、下载、通知、文件访问），工作量和新做一款应用相当，不是把现有模块打开就能编。

如果以后要做 iOS，需要单独的原生或 Compose Multiplatform iOS 客户端，并重新接网盘 / 视频接口。在那之前不要把桌面包或安卓包装到 iPhone 上。
