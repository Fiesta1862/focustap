package com.focustap.app

import android.app.Application
import com.focustap.app.core.FocusController

class FocusTapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FocusController.init(this)
    }
}
