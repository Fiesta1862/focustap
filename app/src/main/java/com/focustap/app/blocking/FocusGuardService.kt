package com.focustap.app.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.focustap.app.core.FocusController

/**
 * Watches window transitions only. It never reads screen text or interacts with
 * other apps — it simply raises FocusTap's shield when a blocked app opens.
 */
class FocusGuardService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusController.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName.isEmpty()) return
        if (FocusController.isBlocked(packageName)) {
            FocusController.launchShield()
        }
    }

    override fun onInterrupt() = Unit
}
