package com.focustap.app.core

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Records unexpected errors locally so they can be shared in bug reports. */
object CrashLogger {

    private const val FILE_NAME = "crash-log.txt"
    private const val MAX_CHARS = 12_000

    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun record(throwable: Throwable, source: String) {
        if (!::app.isInitialized) return
        runCatching {
            val stack = StringWriter()
            throwable.printStackTrace(PrintWriter(stack))
            val body = buildString {
                append("Time: ").append(System.currentTimeMillis()).append('\n')
                append("Source: ").append(source).append('\n')
                append("Device: ").append(android.os.Build.MODEL).append('\n')
                append("Android: ").append(android.os.Build.VERSION.RELEASE)
                append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n\n")
                append(stack.toString())
            }.take(MAX_CHARS)
            File(app.filesDir, FILE_NAME).writeText(body)
        }
    }
}
