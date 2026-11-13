# Upstream

This repository imports the official Windows client and ports it.

| | |
| --- | --- |
| Upstream | https://github.com/tidain/YunX-Desktop |
| Branch | `main` |
| Commit | `00a030f778a65840e9be65baecb15497a94b04ec` |
| Date | 2026-11-13 |
| Release line | v1.3.4 (`version.txt`) |
| Related Android project | https://github.com/CYQawa/YunX |
| License | GNU AGPL-3.0 (same as upstream; this port is not relicensed) |

The import is a merge of `upstream/main` with `--allow-unrelated-histories`, so the upstream commits stay in this repository's history.
Windows-only packaging (`run.ps1`, Inno Setup, the C# launcher, `darkmode.dll`) stays in the tree for Windows builds.
Linux entry points are `run.sh` and `packaging/package-linux.sh`. See [docs/LINUX.md](docs/LINUX.md) and [docs/PORTING.md](docs/PORTING.md).
