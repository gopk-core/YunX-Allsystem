package com.yunx.app.util

import com.sun.jna.platform.win32.Crypt32Util
import com.yunx.app.AppContext
import java.io.File
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 一键从本机浏览器导入网盘登录 Cookie（方案 A）。
 *
 * - Windows Chromium 系（Chrome / Edge / 360 / QQ / 搜狗 / Opera / Brave 等）：
 *   Network\Cookies（SQLite）中的 encrypted_value 为 "v10" 前缀 + Windows DPAPI 加密载荷，
 *   用 CryptUnprotectData 解密（同一 Windows 用户下可解）。
 *   "v20" 前缀为「应用绑定加密」（Chrome 127+ / 较新 Edge），本应用无法解密 → 跳过该 Cookie。
 * - Linux Chromium 系：Cookie 在 `~/.config/<browser>`，v10 用密钥环里的
 *   "Safe Storage" 口令（`secret-tool lookup application <id>`）加 AES-128-CBC 解密。
 *   密钥环锁着或没有 secret-tool 时跳过该浏览器。
 * - Firefox：cookies.sqlite 中 value 为明文，直接读取（Windows 与 Linux 都支持）。
 */
object BrowserCookieImporter {

    private const val TAG = "YunX-Cookie"

    enum class Kind { CHROMIUM, FIREFOX }

    data class BrowserInfo(
        val id: String,
        val displayName: String,
        val kind: Kind,
        val profileDirs: List<File>
    )

    data class FoundCookies(
        val browserName: String,
        val cookie: String,
        val count: Int
    )

    /** 平台 → 需要匹配的 Cookie 域名后缀 */
    fun domainSuffixes(platform: String): List<String> = when (platform) {
        "QUARK" -> listOf(".quark.cn")
        "UC" -> listOf(".uc.cn")
        "BAIDU" -> listOf(".baidu.com")
        "C139" -> listOf("mail.10086.cn", "yun.139.com", ".10086.cn")
        "PAN115" -> listOf(".115.com", ".115cdn.com")
        else -> emptyList()
    }

    /** 发现本机已安装浏览器的 Cookie 库目录（存在 cookies 文件才算） */
    fun discoverBrowsers(): List<BrowserInfo> {
        val os = System.getProperty("os.name", "").lowercase()
        if (os.contains("linux") || os.contains("nix")) return discoverLinux()
        if (!os.contains("win")) return emptyList()
        return discoverWindows()
    }

    private fun discoverWindows(): List<BrowserInfo> {
        val local = System.getenv("LOCALAPPDATA")?.let { File(it) } ?: return emptyList()
        val roaming = System.getenv("APPDATA")?.let { File(it) } ?: return emptyList()
        val out = mutableListOf<BrowserInfo>()

        // Chromium 系：<root>/Network/Cookies
        val chromiumCandidates = listOf(
            "Chrome" to listOf(File(local, "Google\\Chrome\\User Data")),
            "Edge" to listOf(File(local, "Microsoft\\Edge\\User Data")),
            "360极速浏览器" to listOf(File(local, "360Chrome\\Chrome\\User Data")),
            "360安全浏览器" to listOf(
                File(roaming, "360se6\\User Data"),
                File(roaming, "360se\\User Data"),
                File(local, "360se6\\User Data")
            ),
            "QQ浏览器" to listOf(File(local, "Tencent\\QQBrowser\\User Data")),
            "搜狗浏览器" to listOf(
                File(roaming, "SogouExplorer\\User Data"),
                File(local, "SogouExplorer\\User Data")
            ),
            "Opera" to listOf(File(roaming, "Opera Software\\Opera Stable")),
            "Brave" to listOf(File(local, "BraveSoftware\\Brave-Browser\\User Data")),
            "Vivaldi" to listOf(File(local, "Vivaldi\\User Data"))
        )
        for ((name, roots) in chromiumCandidates) {
            val existing = roots.filter { root ->
                root.isDirectory && File(root, "Network\\Cookies").exists()
            }
            if (existing.isNotEmpty()) {
                out += BrowserInfo("chromium-$name", name, Kind.CHROMIUM, existing)
            }
        }

        // Firefox：<Profiles>/xxx/cookies.sqlite
        val ffRoot = File(roaming, "Mozilla\\Firefox\\Profiles")
        if (ffRoot.isDirectory) {
            val profiles = ffRoot.listFiles { f -> f.isDirectory && File(f, "cookies.sqlite").exists() }
                ?.toList() ?: emptyList()
            if (profiles.isNotEmpty()) {
                out += BrowserInfo("firefox", "Firefox", Kind.FIREFOX, profiles)
            }
        }
        return out
    }

    private fun discoverLinux(): List<BrowserInfo> {
        val home = File(System.getProperty("user.home"))
        val config = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }?.let { File(it) }
            ?: File(home, ".config")
        val out = mutableListOf<BrowserInfo>()
        val chromium = listOf(
            "Chrome" to listOf(File(config, "google-chrome"), File(home, ".var/app/com.google.Chrome/config/google-chrome")),
            "Chromium" to listOf(File(config, "chromium")),
            "Edge" to listOf(File(config, "microsoft-edge")),
            "Brave" to listOf(File(config, "BraveSoftware/Brave-Browser")),
            "Opera" to listOf(File(config, "opera")),
            "Vivaldi" to listOf(File(config, "vivaldi"))
        )
        for ((name, roots) in chromium) {
            val profiles = roots.flatMap { chromiumProfiles(it) }
            if (profiles.isNotEmpty()) out += BrowserInfo("chromium-$name", name, Kind.CHROMIUM, profiles)
        }
        val firefoxRoots = listOf(
            File(home, ".mozilla/firefox"),
            File(home, ".var/app/org.mozilla.firefox/.mozilla/firefox")
        )
        val profiles = firefoxRoots.flatMap { root ->
            root.listFiles()?.filter { it.isDirectory && File(it, "cookies.sqlite").isFile }.orEmpty()
        }
        if (profiles.isNotEmpty()) out += BrowserInfo("firefox", "Firefox", Kind.FIREFOX, profiles)
        return out
    }

    /** Chromium 用户目录下 Default / Profile *，Cookie 库在 Network/Cookies 或旧版 Cookies。 */
    private fun chromiumProfiles(root: File): List<File> {
        if (!root.isDirectory) return emptyList()
        val dirs = mutableListOf<File>()
        val names = root.list()?.filter { it == "Default" || it.startsWith("Profile ") }.orEmpty()
        for (name in names) {
            val profile = File(root, name)
            if (File(profile, "Network/Cookies").isFile || File(profile, "Cookies").isFile) dirs += profile
        }
        return dirs
    }

    /** 从指定浏览器导入指定平台的 Cookie；未找到返回 null */
    fun importFrom(browser: BrowserInfo, domains: List<String>): FoundCookies? =
        when (browser.kind) {
            Kind.CHROMIUM -> importChromium(browser, domains)
            Kind.FIREFOX -> importFirefox(browser, domains)
        }

    // ---------- Chromium ----------

    private fun importChromium(browser: BrowserInfo, domains: List<String>): FoundCookies? {
        for (root in browser.profileDirs) {
            val cookiesFile = listOf(File(root, "Network/Cookies"), File(root, "Cookies"))
                .firstOrNull { it.isFile } ?: continue
            val copy = File(AppContext.cacheDir, "browser-cookies-${System.nanoTime()}.db")
            try {
                cookiesFile.copyTo(copy, overwrite = true)
                val cookie = extractChromium(copy, domains, browser.id) ?: continue
                return FoundCookies(browser.displayName, cookie, cookie.split(';').size)
            } catch (e: Exception) {
                Log.w(TAG, "读取 ${browser.displayName} Cookie 失败: ${e.message}")
            } finally {
                runCatching { copy.delete() }
            }
        }
        return null
    }

    private fun extractChromium(dbFile: File, domains: List<String>, browserId: String): String? {
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            val likeClause = domains.joinToString(" OR ") { "host_key LIKE ?" }
            conn.prepareStatement(
                "SELECT host_key, name, encrypted_value FROM cookies WHERE $likeClause"
            ).use { st ->
                domains.forEachIndexed { i, d -> st.setString(i + 1, "%$d%") }
                st.executeQuery().use { rs ->
                    val pairs = mutableListOf<String>()
                    while (rs.next()) {
                        val name = rs.getString(2)
                        val value = decryptChromiumValue(rs.getBytes(3), browserId) ?: continue
                        if (name.isNotBlank()) pairs += "$name=$value"
                    }
                    if (pairs.isEmpty()) return null
                    return pairs.joinToString("; ")
                }
            }
        }
    }

    /**
     * Chromium encrypted_value：
     * - Windows "v10" 前缀 + DPAPI 载荷 → CryptUnprotectData；
     * - Linux "v10" 前缀 → 密钥环口令派生 AES-128-CBC；
     * - "v20" 前缀（应用绑定加密，Chrome 127+）→ 返回 null（无法解密，跳过后整体提示）；
     * - 无前缀（个别旧版）→ Windows 按 DPAPI 尝试，Linux 无法解。
     */
    private fun decryptChromiumValue(bytes: ByteArray, browserId: String): String? {
        if (bytes.size < 3) return null
        val windows = System.getProperty("os.name", "").lowercase().contains("win")
        var offset = 0
        if (bytes[0] == 'v'.code.toByte() &&
            bytes[1] in '0'.code.toByte()..'9'.code.toByte() &&
            bytes[2] in '0'.code.toByte()..'9'.code.toByte()
        ) {
            val version = (bytes[1] - '0'.code.toByte()) * 10 + (bytes[2] - '0'.code.toByte())
            if (version >= 20) return null
            offset = 3
        }
        val payload = if (offset > 0) bytes.copyOfRange(offset, bytes.size) else bytes
        if (!windows) {
            if (offset == 0) return null
            return decryptChromiumLinux(payload, browserId)
        }
        return runCatching {
            val plain = Crypt32Util.cryptUnprotectData(payload)
            String(plain, Charsets.UTF_8)
        }.getOrNull()
    }

    private val linuxKeyCache = HashMap<String, SecretKeySpec?>()

    /** Linux Chromium：PBKDF2-HMAC-SHA1(口令, "saltysalt", 1 次) → AES-128-CBC，IV 为 16 个空格。 */
    private fun decryptChromiumLinux(payload: ByteArray, browserId: String): String? {
        val key = linuxAesKey(browserId) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ByteArray(16) { ' '.code.toByte() }))
            val plain = cipher.doFinal(payload)
            val text = String(plain, Charsets.UTF_8)
            if (text.any { it.code < 9 || (it.code in 14..31) }) {
                if (plain.size > 32) String(plain, 32, plain.size - 32, Charsets.UTF_8) else null
            } else {
                text
            }
        }.getOrNull()
    }

    private fun linuxAesKey(browserId: String): SecretKeySpec? = synchronized(linuxKeyCache) {
        if (linuxKeyCache.containsKey(browserId)) return@synchronized linuxKeyCache[browserId]
        val app = when {
            browserId.contains("chromium", ignoreCase = true) -> "chromium"
            browserId.contains("brave", ignoreCase = true) -> "brave"
            browserId.contains("edge", ignoreCase = true) -> "edge"
            browserId.contains("opera", ignoreCase = true) -> "opera"
            browserId.contains("vivaldi", ignoreCase = true) -> "vivaldi"
            else -> "chrome"
        }
        val password = lookupSecret(app)
        val key = password?.let {
            val spec = PBEKeySpec(it.toCharArray(), "saltysalt".toByteArray(Charsets.UTF_8), 1, 128)
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        }
        if (key == null) {
            Log.w(TAG, "读不到 $app 的 Safe Storage 口令（需要 secret-tool 且密钥环已解锁），跳过该浏览器")
        }
        linuxKeyCache[browserId] = key
        key
    }

    private fun lookupSecret(application: String): String? {
        val process = runCatching {
            ProcessBuilder("secret-tool", "lookup", "application", application)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }.getOrNull() ?: return null
        if (!process.waitFor(4, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        if (process.exitValue() != 0) return null
        return process.inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() }
    }

    // ---------- Firefox ----------

    private fun importFirefox(browser: BrowserInfo, domains: List<String>): FoundCookies? {
        for (profile in browser.profileDirs) {
            val dbFile = File(profile, "cookies.sqlite")
            if (!dbFile.exists()) continue
            val copy = File(AppContext.cacheDir, "ff-cookies-${System.nanoTime()}.db")
            try {
                dbFile.copyTo(copy, overwrite = true)
                val cookie = extractFirefox(copy, domains) ?: continue
                return FoundCookies(browser.displayName, cookie, cookie.split(';').size)
            } catch (e: Exception) {
                Log.w(TAG, "读取 Firefox Cookie 失败: ${e.message}")
            } finally {
                runCatching { copy.delete() }
            }
        }
        return null
    }

    private fun extractFirefox(dbFile: File, domains: List<String>): String? {
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            val likeClause = domains.joinToString(" OR ") { "host LIKE ?" }
            conn.prepareStatement(
                "SELECT host, name, value FROM moz_cookies WHERE $likeClause"
            ).use { st ->
                domains.forEachIndexed { i, d -> st.setString(i + 1, "%$d%") }
                st.executeQuery().use { rs ->
                    val pairs = mutableListOf<String>()
                    while (rs.next()) {
                        val name = rs.getString(2)
                        val value = rs.getString(3)
                        if (name.isNotBlank()) pairs += "$name=$value"
                    }
                    if (pairs.isEmpty()) return null
                    return pairs.joinToString("; ")
                }
            }
        }
    }
}
