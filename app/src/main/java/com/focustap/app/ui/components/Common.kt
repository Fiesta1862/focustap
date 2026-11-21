package com.focustap.app.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.focustap.app.core.CrashLogger
import com.focustap.app.core.FocusController
import com.focustap.app.qr.QrManager
import com.focustap.app.ui.theme.Line
import com.focustap.app.ui.theme.Surface

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberQrScanner(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val payload = QrManager.parseResult(result.resultCode, result.data)
        if (!payload.isNullOrBlank()) FocusController.onQrScanned(payload)
    }
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchQrScanner(context, launcher)
    }
    return remember(launcher, requestCamera, context) {
        {
            val hasCamera = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCamera) {
                launchQrScanner(context, launcher)
            } else {
                runCatching { requestCamera.launch(Manifest.permission.CAMERA) }
                    .onFailure { CrashLogger.record(it, "camera-permission") }
            }
        }
    }
}

private fun launchQrScanner(context: Context, launcher: ActivityResultLauncher<Intent>) {
    val activity = context.findActivity()
    if (activity == null) {
        CrashLogger.record(RuntimeException("No activity available to open the QR scanner"), "qr-scan")
        Toast.makeText(context, "Could not open the QR scanner", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val intent = QrManager.createScanIntent(activity)
        if (activity.packageManager.resolveActivity(intent, 0) == null) {
            throw ActivityNotFoundException("No app is available to scan QR codes")
        }
        launcher.launch(intent)
    }.onFailure { throwable ->
        CrashLogger.record(throwable, "qr-scan")
        Toast.makeText(context, "Could not open the QR scanner", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun EventToasts() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FocusController.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000L
    val hours = minutes / 60L
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
