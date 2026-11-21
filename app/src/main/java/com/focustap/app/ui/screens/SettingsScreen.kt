package com.focustap.app.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.focustap.app.blocking.FocusDeviceAdminReceiver
import com.focustap.app.core.FocusController
import com.focustap.app.ui.FocusViewModel
import com.focustap.app.ui.components.SectionCard
import com.focustap.app.ui.theme.Accent
import com.focustap.app.ui.theme.Success

@Composable
fun SettingsScreen(viewModel: FocusViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmReset by remember { mutableStateOf(false) }
    var showOwnerInfo by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }

    val accessibility = remember(refresh) { FocusController.isAccessibilityEnabled() }
    val deviceAdmin = remember(refresh) { FocusController.isDeviceAdminActive() }
    val cooldown = (state.emergencyCooldownUntil - now).coerceAtLeast(0L)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Strict mode", style = MaterialTheme.typography.titleLarge)
                    Text("Locks the device if someone tries to disable FocusTap, and watches for clock changes.", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = state.strictMode, onCheckedChange = { viewModel.setStrict(it) })
            }
            if (state.strictMode && !deviceAdmin) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Device admin is off — strict safeguards are limited.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text("Emergency unlock", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.emergencyUnlocksRemaining} of ${com.focustap.app.data.AppState.EMERGENCY_LIMIT} left this month",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (cooldown > 0L) {
                Text("Cooldown: ${cooldown / 1000}s", style = MaterialTheme.typography.bodyMedium, color = Accent)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.emergencyUnlock() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isLocked && state.emergencyUnlocksRemaining > 0 && cooldown == 0L,
            ) { Text("Use emergency unlock") }
            TextButton(onClick = { viewModel.resetEmergencyAllowance() }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset allowance")
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text("Permissions", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            PermissionLine("Accessibility service", accessibility) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            Spacer(Modifier.height(6.dp))
            PermissionLine("Device admin", deviceAdmin) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        FocusDeviceAdminReceiver.componentName(context),
                    )
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Allows FocusTap's strict mode to guard active sessions.",
                    )
                context.startActivity(intent)
            }
            Spacer(Modifier.height(6.dp))
            PermissionLine("NFC", FocusController.hasNfcHardware()) {
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text("Prevent uninstall (advanced)", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Android only lets a Device Owner block uninstalls. FocusTap can be provisioned as device owner on a dedicated device using adb:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "adb shell dpm set-device-owner com.focustap.app/.blocking.FocusDeviceAdminReceiver",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = Accent,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showOwnerInfo = !showOwnerInfo }) {
                Text(if (showOwnerInfo) "Hide details" else "What this means")
            }
            if (showOwnerInfo) {
                Text(
                    "Device owner can only be set before adding an account, and usually requires a factory reset. " +
                        "It is optional: the accessibility shield and emergency unlock work without it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text("Data", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset all FocusTap data", color = Accent)
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset everything?") },
            text = { Text("Removes modes, stats, paired anchors and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetEverything()
                    confirmReset = false
                }) { Text("Reset", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PermissionLine(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (granted) {
            Icon(Icons.Default.Check, null, tint = Success, modifier = Modifier.size(20.dp))
        } else {
            TextButton(onClick = onClick) { Text("Open") }
        }
    }
}
