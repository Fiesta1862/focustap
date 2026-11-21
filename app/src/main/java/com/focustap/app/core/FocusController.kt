package com.focustap.app.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.content.getSystemService
import com.focustap.app.blocking.FocusDeviceAdminReceiver
import com.focustap.app.blocking.FocusGuardService
import com.focustap.app.blocking.FocusLockService
import com.focustap.app.blocking.ShieldActivity
import com.focustap.app.data.AppState
import com.focustap.app.data.ESSENTIAL_PACKAGES
import com.focustap.app.data.EmergencyResult
import com.focustap.app.data.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth shared by Compose, the accessibility service, the
 * foreground service, the NFC handler and the shield screen.
 */
object FocusController {

    private lateinit var app: Context
    private var repository: FocusRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    var cached: AppState = AppState()
        private set

    @Volatile
    var awaitingTagPairing = false

    @Volatile
    var awaitingQrPairing = false

    @Volatile
    private var lastShieldLaunch = 0L

    val events = MutableSharedFlow<String>(extraBufferCapacity = 16)

    val state: Flow<AppState>
        get() = readyRepository().state

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        repository = FocusRepository(app)
        scope.launch {
            readyRepository().state.collect { cached = it }
        }
    }

    private fun readyRepository(): FocusRepository =
        repository ?: error("FocusController.init() must be called from Application.onCreate()")

    // ---------------------------------------------------------------------
    // Anchor handling
    // ---------------------------------------------------------------------

    fun onAnchorScanned(tagId: String) {
        if (!::app.isInitialized) return
        scope.launch {
            haptic()
            val current = readyRepository().state.first()
            when {
                awaitingTagPairing -> {
                    readyRepository().setPairedTag(tagId)
                    awaitingTagPairing = false
                    events.tryEmit("Anchor tag registered")
                }

                current.pairedTagId == null -> {
                    events.tryEmit("No anchor registered yet — use Pair to register this tag")
                }

                current.pairedTagId != tagId -> {
                    events.tryEmit("This tag is not your Anchor Key")
                }

                current.isLocked -> {
                    endSession("nfc")
                    events.tryEmit("Session ended — welcome back")
                }

                else -> {
                    startSession()
                    events.tryEmit("Focus started")
                }
            }
        }
    }

    fun onQrScanned(payload: String) {
        if (!::app.isInitialized) return
        scope.launch {
            haptic()
            val current = readyRepository().state.first()
            when {
                awaitingQrPairing -> {
                    readyRepository().setPairedQr(payload)
                    awaitingQrPairing = false
                    events.tryEmit("QR anchor registered")
                }

                current.pairedQrSecret == null -> {
                    events.tryEmit("No QR anchor registered yet — use Pair to create one")
                }

                current.pairedQrSecret != payload -> {
                    events.tryEmit("This QR code is not your Anchor Key")
                }

                current.isLocked -> {
                    endSession("qr")
                    events.tryEmit("Session ended — welcome back")
                }

                else -> {
                    startSession()
                    events.tryEmit("Focus started")
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Session controls
    // ---------------------------------------------------------------------

    fun startFocus(modeId: String? = null) {
        scope.launch {
            if (modeId != null) readyRepository().setActiveMode(modeId)
            startSession()
            haptic()
            events.tryEmit("Focus started")
        }
    }

    private suspend fun startSession() {
        val current = readyRepository().state.first()
        readyRepository().startSession(current.activeModeId)
        startLockService()
    }

    fun endSession(reason: String = "manual") {
        scope.launch { endSessionInternal(reason) }
    }

    private suspend fun endSessionInternal(reason: String) {
        readyRepository().endSession(reason)
        stopLockService()
    }

    fun emergencyUnlock() {
        scope.launch {
            when (readyRepository().emergencyUnlock()) {
                EmergencyResult.OK -> {
                    stopLockService()
                    haptic()
                    events.tryEmit("Emergency unlock used — 60s cooldown started")
                }

                EmergencyResult.COOLDOWN -> events.tryEmit("Emergency unlock cooling down — try again in a minute")
                EmergencyResult.EXHAUSTED -> events.tryEmit("No emergency unlocks left this month")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------

    fun setActiveMode(id: String) = scope.launch { readyRepository().setActiveMode(id) }

    fun setStrict(enabled: Boolean) = scope.launch {
        readyRepository().setStrict(enabled)
        events.tryEmit(if (enabled) "Strict mode on" else "Strict mode off")
    }

    fun saveMode(mode: com.focustap.app.data.FocusMode) = scope.launch { readyRepository().saveMode(mode) }

    fun deleteMode(id: String) = scope.launch { readyRepository().deleteMode(id) }

    fun pairTag(id: String) = scope.launch { readyRepository().setPairedTag(id) }

    fun pairQr(secret: String) = scope.launch { readyRepository().setPairedQr(secret) }

    fun clearPairing() = scope.launch {
        readyRepository().setPairedTag(null)
        readyRepository().setPairedQr(null)
        events.tryEmit("Anchor keys removed")
    }

    fun finishOnboarding() = scope.launch { readyRepository().setOnboardingComplete() }

    fun resetEmergencyAllowance() = scope.launch {
        readyRepository().resetEmergencyAllowance()
        events.tryEmit("Emergency unlocks reset")
    }

    fun resetEverything() = scope.launch {
        stopLockService()
        readyRepository().clearAll()
        events.tryEmit("All FocusTap data cleared")
    }

    // ---------------------------------------------------------------------
    // Blocking helpers used by services
    // ---------------------------------------------------------------------

    fun isBlocked(packageName: String): Boolean {
        val snapshot = cached
        if (!snapshot.isLocked || packageName.isBlank()) return false
        if (ESSENTIAL_PACKAGES.any { packageName == it || packageName.startsWith(it) }) return false
        return snapshot.activeMode.blockedPackages.any { packageName == it || packageName.startsWith(it) }
    }

    /** Domain enforcement is recorded in profiles; a VPN-backed blocker is a follow-up feature. */
    fun isBlockedDomain(host: String): Boolean {
        val snapshot = cached
        if (!snapshot.isLocked) return false
        return snapshot.activeMode.blockedDomains.any { domain ->
            host.equals(domain, ignoreCase = true) || host.endsWith(".$domain", ignoreCase = true)
        }
    }

    fun launchShield() {
        if (!::app.isInitialized) return
        val now = System.currentTimeMillis()
        if (now - lastShieldLaunch < 1_500L) return
        lastShieldLaunch = now
        val intent = Intent(app, ShieldActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
        )
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            // Some OEM skins block background activity starts; the persistent
            // notification remains as a fallback entry point to the shield.
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        if (!::app.isInitialized) return false
        val expected = ComponentName(app, FocusGuardService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun isDeviceAdminActive(): Boolean {
        if (!::app.isInitialized) return false
        return devicePolicyManager().isAdminActive(ComponentName(app, FocusDeviceAdminReceiver::class.java))
    }

    fun hasNfcHardware(): Boolean =
        ::app.isInitialized && NfcAdapter.getDefaultAdapter(app) != null

    fun lockDeviceNow() {
        if (!::app.isInitialized) return
        if (isDeviceAdminActive()) {
            try {
                devicePolicyManager().lockNow()
            } catch (_: SecurityException) {
            }
        }
    }

    private fun devicePolicyManager(): android.app.DevicePolicyManager =
        app.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.DevicePolicyManager

    private fun startLockService() {
        if (!::app.isInitialized) return
        val intent = Intent(app, FocusLockService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        } catch (_: Exception) {
            // Lock still holds via DataStore + accessibility; the notification is best-effort.
        }
    }

    private fun stopLockService() {
        if (!::app.isInitialized) return
        runCatching { app.stopService(Intent(app, FocusLockService::class.java)) }
    }

    fun haptic() {
        if (!::app.isInitialized) return
        try {
            val effect = VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = app.getSystemService<VibratorManager>(Context.VIBRATOR_MANAGER_SERVICE)
                manager?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService<Vibrator>(Context.VIBRATOR_SERVICE)
                vibrator?.vibrate(effect)
            }
        } catch (_: Exception) {
        }
    }
}
