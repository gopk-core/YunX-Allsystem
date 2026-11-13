# Upstream

This repository imports the official Windows client and ports it.

| | |
| --- | --- |
| Upstream | https://github.com/tidain/YunX-Desktop |
| Branch | `main` |
| Commit | `3700c1c607eea9517f022318d85cb543f05a85dc` |
| Date | 2026-11-04 20:37:46 +0800 |
| Release line | v1.3.3 (`version.txt`) |
| Related Android project | https://github.com/CYQawa/YunX |
| License | GNU AGPL-3.0 (same as upstream; this port is not relicensed) |

The import is a merge of `upstream/main` with `--allow-unrelated-histories`.
The fetched upstream history is the tip commit (a full clone of older history was not required to build).
Windows-only packaging (`run.ps1`, Inno Setup, the C# launcher, `darkmode.dll`) stays in the tree for Windows builds.
Linux entry points are `run.sh` and `packaging/package-linux.sh`. See [docs/LINUX.md](docs/LINUX.md) and [docs/PORTING.md](docs/PORTING.md).
