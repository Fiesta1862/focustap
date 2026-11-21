package com.focustap.app.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focustap.app.core.FocusController

/** Restores the shield after a reboot if a session was still active. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return
        FocusController.init(context)
        FocusController.restoreAfterBoot()
    }
}
