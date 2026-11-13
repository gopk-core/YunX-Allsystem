# YunX-Allsystem

**This is an unofficial multi-system port of the official Windows client [tidain/YunX-Desktop](https://github.com/tidain/YunX-Desktop). It is not the official Windows release.**

The first target is **Debian GNU/Linux 13 (trixie) amd64**. The license stays **[GNU AGPL-3.0](LICENSE)** — it is not MIT. If you use this code, you must publish your changes under AGPL-3.0 as well.

Attribution: the Windows client is [tidain/YunX-Desktop](https://github.com/tidain/YunX-Desktop) (imported from `main` at `00a030f`, release line v1.3.4). The Android app in the same project is [CYQawa/YunX](https://github.com/CYQawa/YunX). See [NOTICE](NOTICE) and [UPSTREAM.md](UPSTREAM.md).

This port does **not** include the upstream in-app updater. It does not check for, download, or install a newer version of itself. Get updates from this repository and rebuild.

这是 [tidain/YunX-Desktop](https://github.com/tidain/YunX-Desktop) 的**非官方多系统移植**（仓库名保持 YunX-Allsystem），从 Debian/Linux 开始。它不是官方 Windows 发行版。协议仍然是 GNU AGPL-3.0。

> 云析永远免费开源。如果你是在任何地方「花钱买到」的，说明你被骗了，请立即退款。任何收费版本都与本仓库和上游无关。

## Debian 13 上怎么用

安装依赖、编译、打 `.deb` / 便携包的步骤写在 **[docs/LINUX.md](docs/LINUX.md)**。最短路径：

```bash
git clone https://github.com/gopk-core/YunX-Allsystem.git
cd YunX-Allsystem
sudo apt install -y openjdk-17-jdk
./run.sh build          # 只编译
./run.sh run            # 编译并启动（需要图形界面）
./run.sh package        # release/YunX-Allsystem-<版本>-linux-amd64.tar.gz
./run.sh deb            # 再打一个 amd64 .deb
```

Windows 10/11 上的官方构建方式没有删掉，仍是 `.\run.ps1`，说明见 [docs/BUILD.md](docs/BUILD.md)。

## 它能做什么

和上游桌面版同一套 Kotlin 代码：粘贴网盘分享或视频链接，浏览、预览、分片下载。

- 网盘：夸克 / UC / 迅雷 / 百度 / 139 / 123 / 115 / 光鸭 / 蓝奏云 / 蓝奏云优享版，以及 GitHub
- 视频：抖音 / 快手 / 微博 / 小红书 / 哔哩哔哩 / AcFun / Pixiv
- P2P：磁力 / BT / ed2k，交给 Gopeed（Linux 上下载官方 linux 内核）
- 多账号，以及加密后的认证备份

数据在 Linux 上按 XDG 放：`~/.local/share/yunx-desktop`、`~/.cache/yunx-desktop`、`~/.config/yunx-desktop`。

还没做到和 Windows 完全一样的地方（托盘进度条、ffplay 嵌进窗口、DPAPI 互操作等）列在 **[docs/PORTING.md](docs/PORTING.md)**。应用内更新系统是有意去掉的，不是漏移植：启动时不会检查新版本，设置里也没有「检查更新」。

## 文档

| 文档 | 内容 |
| --- | --- |
| [Debian / Linux 构建与安装](docs/LINUX.md) | Debian 13 amd64 的依赖、编译、tar.gz 和 .deb |
| [移植差异与暂缓项](docs/PORTING.md) | 哪些已经换成本机实现，哪些仍是 Windows 专用 |
| [上游来源](UPSTREAM.md) | 导入的提交与协议 |
| [功能清单](docs/FEATURES.md) | 上游功能说明（原文按 Windows 来写） |
| [使用说明](docs/USAGE.md) | 上游使用说明 |
| [Windows 构建](docs/BUILD.md) | 仅 Windows |
| [免责声明](docs/DISCLAIMER.md) | 上游免责声明 |

## 开源协议

GNU AGPL-3.0。全文见 [LICENSE](LICENSE)。请保留对 [tidain/YunX-Desktop](https://github.com/tidain/YunX-Desktop) 和 [CYQawa/YunX](https://github.com/CYQawa/YunX) 的署名。
