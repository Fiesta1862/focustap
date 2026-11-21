package com.focustap.app

import android.app.Application
import com.focustap.app.core.CrashLogger
import com.focustap.app.core.FocusController

class FocusTapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
        installCrashLogger()
        FocusController.init(this)
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashLogger.record(throwable, "uncaught:${thread.name}")
            previous?.uncaughtException(thread, throwable)
        }
    }
}
