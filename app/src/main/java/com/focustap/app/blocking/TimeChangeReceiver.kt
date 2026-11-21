package com.focustap.app.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focustap.app.core.FocusController

/**
 * Detects clock tampering during a strict session and immediately re-locks the
 * device / raises the shield. It does not try to change the clock back itself.
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FocusController.init(context)
        val snapshot = FocusController.cached
        if (snapshot.isLocked && snapshot.strictMode) {
            FocusController.lockDeviceNow()
            FocusController.launchShield()
        }
    }
}
