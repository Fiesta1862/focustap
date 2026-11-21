package com.focustap.app.blocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.focustap.app.R
import com.focustap.app.core.CrashLogger
import com.focustap.app.core.FocusController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Keeps a visible, persistent reminder of the active session. */
class FocusLockService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification(0L)
        val started = runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        }
        if (started.isFailure) {
            CrashLogger.record(
                started.exceptionOrNull() ?: RuntimeException("Foreground service start failed"),
                "foreground-service",
            )
            stopSelf()
            return
        }
        scope.launch {
            while (true) {
                val snapshot = FocusController.cached
                if (FocusController.stateLoaded.value && !snapshot.isLocked) {
                    stopSelf()
                    break
                }
                val elapsed = if (snapshot.lockStartedAt > 0L) {
                    (System.currentTimeMillis() - snapshot.lockStartedAt).coerceAtLeast(0L)
                } else {
                    0L
                }
                runCatching {
                    NotificationManagerCompat.from(this@FocusLockService)
                        .notify(NOTIFICATION_ID, buildNotification(elapsed))
                }
                delay(1_000L)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.focus_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows the elapsed time of your active focus session"
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    private fun buildNotification(elapsedMs: Long): android.app.Notification {
        val intent = Intent(this, ShieldActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val minutes = elapsedMs / 60_000L
        val seconds = (elapsedMs / 1_000L) % 60L
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(getString(R.string.focus_notification_title))
            .setContentText(
                String.format("%02d:%02d elapsed · tap your anchor to finish", minutes, seconds),
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "focustap_focus"
        const val NOTIFICATION_ID = 4101
    }
}
