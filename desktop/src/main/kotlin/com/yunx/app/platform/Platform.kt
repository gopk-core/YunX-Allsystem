package com.yunx.app.platform

import java.io.File

/**
 * 当前进程跑在哪种操作系统上。Windows 专用实现（DPAPI、Win32 托盘菜单、darkmode.dll）
 * 都先问这里，再决定要不要碰原生库。
 */
object Platform {
    private val osName: String = System.getProperty("os.name", "").lowercase()

    val isWindows: Boolean = osName.contains("win")
    val isMac: Boolean = osName.contains("mac") || osName.contains("darwin")
    val isLinux: Boolean = !isWindows && !isMac

    /** XDG 数据目录：`$XDG_DATA_HOME/yunx-desktop`，缺省 `~/.local/share/yunx-desktop`。 */
    fun xdgDataDir(): File {
        val base = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?: File(System.getProperty("user.home"), ".local/share")
        return File(base, "yunx-desktop")
    }

    /** XDG 缓存目录：`$XDG_CACHE_HOME/yunx-desktop`，缺省 `~/.cache/yunx-desktop`。 */
    fun xdgCacheDir(): File {
        val base = System.getenv("XDG_CACHE_HOME")?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?: File(System.getProperty("user.home"), ".cache")
        return File(base, "yunx-desktop")
    }

    /**
     * XDG 配置目录：`$XDG_CONFIG_HOME/yunx-desktop`，缺省 `~/.config/yunx-desktop`。
     *
     * OpenJDK 的文件偏好实现会在 [java.util.prefs.userRoot] 后面再拼 `.java/.userPrefs`，
     * 所以真正的偏好文件在 `~/.config/yunx-desktop/.java/.userPrefs`。
     */
    fun xdgConfigDir(): File {
        val base = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?: File(System.getProperty("user.home"), ".config")
        return File(base, "yunx-desktop")
    }

    /**
     * 在第一次触碰 [java.util.prefs.Preferences] 之前调用。
     * Linux 上把偏好根指到 XDG 配置目录；Windows 继续用注册表，这里什么都不做。
     */
    fun installUserPrefsRoot() {
        if (!isLinux) return
        if (!System.getProperty("java.util.prefs.userRoot").isNullOrBlank()) return
        val dir = xdgConfigDir()
        dir.mkdirs()
        System.setProperty("java.util.prefs.userRoot", dir.absolutePath)
    }
}
