package com.yunx.app.util

import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 下载期间阻止系统睡眠。
 * Windows：kernel32.SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED)，
 * 该状态是线程本地属性，所有调用固定投递到同一线程执行。
 * Linux：`systemd-inhibit --what=sleep:idle` 占住一个抑制锁，引用计数归零时结束该进程。
 */
object WindowsKeepAwake {
    private interface Kernel32 : StdCallLibrary {
        fun SetThreadExecutionState(esFlags: Int): Int

        companion object {
            val INSTANCE: Kernel32 = Native.load("kernel32", Kernel32::class.java)
        }
    }

    private const val ES_CONTINUOUS = 0x80000000.toInt()
    private const val ES_SYSTEM_REQUIRED = 0x00000001

    /** 所有 SetThreadExecutionState 调用固定在此线程（状态线程本地，跨线程会失调） */
    private val executor by lazy {
        Executors.newSingleThreadExecutor { r -> Thread(r, "yunx-keep-awake") }
    }
    private val refs = AtomicInteger(0)

    @Volatile
    private var inhibit: Process? = null

    private fun isWindows(): Boolean =
        System.getProperty("os.name", "").lowercase().contains("win")

    fun acquire() {
        if (!isWindows()) {
            if (refs.getAndIncrement() == 0) startLinuxInhibit()
            return
        }
        if (refs.getAndIncrement() == 0) {
            executor.execute {
                runCatching { Kernel32.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS or ES_SYSTEM_REQUIRED) }
            }
        }
    }

    fun release() {
        if (!isWindows()) {
            if (refs.decrementAndGet() <= 0) {
                refs.set(0)
                stopLinuxInhibit()
            }
            return
        }
        if (refs.decrementAndGet() <= 0) {
            refs.set(0)
            executor.execute {
                runCatching { Kernel32.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS) }
            }
        }
    }

    private fun startLinuxInhibit() {
        runCatching {
            inhibit = ProcessBuilder(
                "systemd-inhibit",
                "--what=sleep:idle",
                "--who=YunX-Desktop",
                "--why=Downloading",
                "--mode=block",
                "sleep", "infinity"
            ).redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        }.onFailure {
            Log.w("YunX-KeepAwake", "systemd-inhibit 不可用，下载期间不阻止休眠: ${it.message}")
        }
    }

    private fun stopLinuxInhibit() {
        val process = inhibit
        inhibit = null
        runCatching { process?.destroy() }
    }
}
