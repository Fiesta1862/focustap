package com.focustap.app.blocking

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.focustap.app.core.FocusController

/**
 * Device-admin side of Strict Mode. On a standard user profile Android only
 * grants limited policies; full "cannot uninstall" behaviour requires device-owner
 * provisioning, which is documented in the README.
 */
class FocusDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        FocusController.init(context)
        Toast.makeText(context, "Strict mode device admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        FocusController.init(context)
        val snapshot = FocusController.cached
        return if (snapshot.isLocked && snapshot.strictMode) {
            FocusController.lockDeviceNow()
            "A FocusTap session is active. Finish it with your NFC tag or QR anchor before turning strict mode off."
        } else {
            "Turn off FocusTap strict mode?"
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Strict mode device admin disabled", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, FocusDeviceAdminReceiver::class.java)
    }
}
