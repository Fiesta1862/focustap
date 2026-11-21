package com.focustap.app

import android.app.Application
import com.focustap.app.core.FocusController
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class FocusTapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        FocusController.init(this)
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val stack = StringWriter()
                throwable.printStackTrace(PrintWriter(stack))
                File(filesDir, "crash-log.txt").writeText(
                    buildString {
                        append("Time: ").append(System.currentTimeMillis()).append('\n')
                        append("Thread: ").append(thread.name).append('\n')
                        append("Device: ").append(android.os.Build.MODEL).append('\n')
                        append("Android: ").append(android.os.Build.VERSION.RELEASE)
                        append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n\n")
                        append(stack.toString())
                    },
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
