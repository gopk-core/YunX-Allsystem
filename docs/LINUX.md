# Debian GNU/Linux 13（trixie）amd64

本文件说明如何在 **Debian 13（trixie）amd64** 上构建、安装和运行
[YunX-Allsystem](https://github.com/gopk-core/YunX-Allsystem)。

这是 [tidain/YunX-Desktop](https://github.com/tidain/YunX-Desktop) 的非官方多系统移植，
**不是**官方 Windows 客户端。协议仍是 [GNU AGPL-3.0](../LICENSE)。
与安卓版 [CYQawa/YunX](https://github.com/CYQawa/YunX) 同属云析项目，使用代码时请保留署名并同样以 AGPL-3.0 公开。

官方 Windows 客户端的构建说明仍在 [BUILD.md](BUILD.md)，只适用于 Windows 10/11 x64。

## 运行时依赖

内嵌浏览器是 JCEF 132（Chromium）。第一次打开登录页时会把原生库解到
`~/.cache/yunx-desktop/jcef-bundle`。下面这些包要先装上，否则 Chromium 起不来：

```bash
sudo apt update
sudo apt install -y \
  libnss3 libnspr4 \
  libatk1.0-0t64 libatk-bridge2.0-0t64 libcups2t64 \
  libdrm2 libgbm1 libgtk-3-0t64 libglib2.0-0t64 \
  libpango-1.0-0 libcairo2 libasound2t64 \
  libx11-6 libx11-xcb1 libxcb1 libxext6 libxfixes3 libxdamage1 \
  libxcomposite1 libxrandr2 libxkbcommon0 libxshmfence1 \
  libxss1 libxcursor1 libxi6 libxtst6 \
  fonts-liberation xdg-utils
```

建议再装（没有也能启动，对应功能会降级）：

| 包 | 作用 |
| --- | --- |
| `libnotify-bin` | 下载进度的桌面通知（`notify-send`） |
| `libsecret-tools` | 「较为严格」密钥档，以及从 Chrome 导入 Cookie |
| `ffmpeg` | 系统里的 `ffplay` / `ffmpeg`。不装也可以在应用里下载一份私有构建 |
| `openjdk-17-jre` | 只有**没带** `runtime/` 的目录才需要。本仓库打出来的 tar.gz 和 .deb 已自带 jlink 运行时 |

从源码编译还需要 JDK 17（含 `jlink`）和常见构建工具：

```bash
sudo apt install -y openjdk-17-jdk
```

## 从源码编译

```bash
git clone https://github.com/gopk-core/YunX-Allsystem.git
cd YunX-Allsystem
./run.sh build
```

`./run.sh build` 只编译 Kotlin，生成 `desktop/build/libs/*.jar`，不启动界面，也不需要 C++ 编译器。
Windows 上的 `darkmode.dll` 只在 Windows 构建里才会编译。

在图形会话里直接跑：

```bash
./run.sh run
```

## 打包装

```bash
./run.sh package    # release/YunX-Allsystem-<版本>-linux-amd64.tar.gz
./run.sh deb        # 上面的 tar.gz，再加上 release/yunx-allsystem_<版本>_amd64.deb
```

便携包解压后：

```bash
tar -C "$HOME" -xzf release/YunX-Allsystem-*-linux-amd64.tar.gz
"$HOME/YunX-Allsystem/yunx-desktop"
```

安装 deb（文件名里的版本号以 `version.txt` 为准，当前是 1.3.3）：

```bash
sudo apt install -y ./release/yunx-allsystem_*_amd64.deb
yunx-desktop
```

deb 把程序放在 `/opt/yunx-allsystem`，命令在 `/usr/bin/yunx-desktop`，并注册桌面菜单「云析 YunX」。
卸载：`sudo apt remove yunx-allsystem`。用户数据不会被删。

## 数据目录（XDG）

| 用途 | 路径 |
| --- | --- |
| 数据库、账号、日志 | `$XDG_DATA_HOME/yunx-desktop`，默认 `~/.local/share/yunx-desktop` |
| 下载分片、JCEF | `$XDG_CACHE_HOME/yunx-desktop`，默认 `~/.cache/yunx-desktop` |
| 偏好（java.util.prefs） | `$XDG_CONFIG_HOME/yunx-desktop/.java/.userPrefs`，默认 `~/.config/yunx-desktop/.java/.userPrefs` |

JDK 会在配置根后面固定再加一层 `.java/.userPrefs`，所以偏好不在 `~/.config/yunx-desktop` 的根上。
测试或沙箱可以用 `YUNX_DESKTOP_DATA_DIR` 改数据根。

## 和 Windows 版的对应关系

| Windows | Debian / Linux |
| --- | --- |
| `run.ps1` / `run.bat` | `run.sh` |
| C# 启动器 `YunX-Desktop.exe` | `yunx-desktop`（bash + 自带 JRE） |
| Inno Setup | `.deb` 与便携 tar.gz |
| `%LOCALAPPDATA%` 安装目录 | `/opt/yunx-allsystem` 或任意解压目录 |
| Win32 托盘菜单、`darkmode.dll` | AWT / 桌面环境托盘菜单 |
| 通知中心进度条 | `notify-send`（没有系统进度条） |
| `explorer.exe` | `xdg-open`，文件选中优先走 FileManager1 |
| DPAPI | libsecret（`secret-tool`）；「普通」档绑定 `/etc/machine-id` |
| JCEF Windows 原生库 | `jcef-natives-linux-amd64` |
| `gopeed.exe` | 官方 `gopeed-web-*-linux-amd64.zip` 里的 `gopeed` |

还没在这一轮做成和 Windows 一样的部分见 [PORTING.md](PORTING.md)。
