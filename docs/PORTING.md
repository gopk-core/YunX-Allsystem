# 移植说明与暂缓项

上游：[tidain/YunX-Desktop](https://github.com/tidain/YunX-Desktop) `main`，
当前对齐的提交是 `00a030f778a6`（2026-11-13，版本 1.3.4）。
安卓版是 [CYQawa/YunX](https://github.com/CYQawa/YunX)。

本仓库保留上游历史（`upstream/main` 以无共同祖先的方式合并进来），并在其上改 Linux 路径。
Windows 脚本（`run.ps1`、`portable-package.ps1`、`installer/`、C# 启动器）还在，
在 Windows 上仍按原来的方式构建。Linux 不编译 `darkmode.dll`，也不跑 Inno Setup。

## 这一轮已经接到 Linux 上的行为

- 分享解析、视频解析、Range 下载、多账号与加密备份：这些是 Kotlin/JVM 代码，随 `./run.sh build` 一起编译。
- 数据目录改到 XDG（`~/.local/share`、`~/.cache`、`~/.config`）。
- 目录选择继续用 AWT/Swing；打开文件和目录走 `xdg-open`；在文件管理器里显示文件优先走 `org.freedesktop.FileManager1`。
- 托盘用 AWT `TrayIcon`。在 GNOME / KDE 上由桌面环境显示，右键是 GTK 菜单。
- 下载通知用 `notify-send`，大约 5 秒一次，外加完成时的一条。没有 Windows 那种原地更新的进度条。
- 下载时用 `systemd-inhibit` 阻止休眠。没有 systemd 时只记一条日志，下载继续。
- 系统代理读 `http_proxy` / `https_proxy` / `all_proxy`，否则读 GNOME `gsettings`。PAC 仍然不支持。
- 「普通」密钥档绑定 `/etc/machine-id`。「较为严格」在 Linux 上写入 libsecret，而不是 DPAPI。
- Gopeed 按本机系统拉取官方 `gopeed-web-<版本>-linux-<架构>.zip`。
- ffplay：先用系统 PATH 里的 `ffplay` / `ffmpeg`，没有再下载 BtbN 的 Linux 构建。预览窗口是独立的 ffplay 窗口。
- Firefox Cookie 可以导入。Chromium Cookie 在密钥环解锁且 `secret-tool` 可用时尝试解密 v10。
- 内嵌浏览器使用 JCEF 的 Linux amd64 原生库。默认加 `--no-sandbox`，因为解包出来的 `chrome-sandbox` 没有 setuid。需要沙箱时启动前设置 `YUNX_CEF_SANDBOX=1`。
- 关机：`systemctl poweroff`，失败再试 `shutdown -h`。是否真的关得掉取决于 polkit。

## 有意去掉：应用内更新

这个移植**不检查、不下载、也不安装自己的新版本**。

已经删掉的部分：

- 启动时对 `tidain/YunX-Desktop` GitHub Release 的自动检查（`MainScreen` 里的 `LaunchedEffect`）
- 设置页「检查更新」和「接受预发布版更新」
- 更新弹窗（`UpdateDialog`），包括「下载并安装」「镜像下载」「不再提示该版本」「强制更新」
- 开发调试菜单里的「显示检查更新弹窗」
- `UpdateChecker.fetchLatestRelease` 以及版本比较、强制更新解析
- 设置项 `accept_prerelease_update`

关于页仍显示 `version.txt` 生成的版本号。GitHub 镜像测速还在，它只给仓库文件、Gopeed 内核和 FFmpeg 包加速，不会拉取本应用的安装包。

新版本请自行从 [gopk-core/YunX-Allsystem](https://github.com/gopk-core/YunX-Allsystem) 获取源码后重新构建。

安卓、macOS、iOS 见 [ANDROID.md](ANDROID.md)、[MACOS.md](MACOS.md)、[IOS.md](IOS.md)。

## 暂缓（没有静默删掉，只是这一轮不做）

- **Win32 暗色标题栏 / `darkmode.dll`**：只服务 Windows 菜单和 DWM。Linux 跟随桌面主题，不再移植这份 C++。
- **通知中心里的实时进度条**：Linux 通知没有同等的 progress toast API。现在是节流后的 `notify-send`。
- **ffplay 嵌进主窗口**：依赖 `SetParent`。Linux 上预览是单独的 ffplay 窗口。
- **全屏透明遮罩选窗口位置**：上游用 C# `UpdateLayeredWindow`。Linux 改为一个可拖动的 Swing 窗口，保存它的坐标。
- **把 Windows DPAPI 包起来的密钥拿到 Linux 上解开**：格式不同。换机请用应用里的加密认证备份，或在 Windows 上先把密钥档改成「宽松」再拷数据。
- **Chrome「应用绑定加密」（v20）**：上游在 Windows 上也不能解，Linux 同样跳过。
- **360 / QQ / 搜狗浏览器**：只扫描 Windows 安装路径。Linux 扫描 Chrome、Chromium、Edge、Brave、Opera、Vivaldi 和 Firefox。
- **SQLCipher「严格」档**：上游就没做，这里也没做。
- **在 Linux 上交叉编译 Windows 安装包**：不在这一轮。Windows 安装包仍用仓库里的 PowerShell 脚本。
- **浏览器扩展**：扩展本身是 Manifest V3，不绑定 Windows。它通过 `127.0.0.1` 和桌面端通信，桌面端起来之后可以配。没有单独做 Linux 安装器。

网盘和视频接口的行为与上游桌面版一致，包括请求里仍然使用桌面版 Chrome 的 Windows 风格 UA。
改成 Linux UA 会让部分网盘接口拒绝登录，所以没有改。
