package com.focustap.app.ui.screens

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.focustap.app.blocking.FocusDeviceAdminReceiver
import com.focustap.app.core.FocusController
import com.focustap.app.ui.FocusViewModel
import com.focustap.app.ui.components.SectionCard
import com.focustap.app.ui.theme.Accent
import com.focustap.app.ui.theme.Canvas
import com.focustap.app.ui.theme.Charcoal
import com.focustap.app.ui.theme.Success

@Composable
fun OnboardingScreen(viewModel: FocusViewModel) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val accessibility by remember(refresh) { mutableStateOf(FocusController.isAccessibilityEnabled()) }
    val deviceAdmin by remember(refresh) { mutableStateOf(FocusController.isDeviceAdminActive()) }
    val nfc by remember(refresh) { mutableStateOf(FocusController.hasNfcHardware()) }
    val notifications by remember(refresh) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = step, label = "onboarding-step") { current ->
            when (current) {
                0 -> OnboardingWelcome(onNext = { step = 1 })
                1 -> OnboardingPermissions(
                    accessibility = accessibility,
                    deviceAdmin = deviceAdmin,
                    nfc = nfc,
                    notifications = notifications,
                    onOpen = { intent ->
                        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onNext = { step = 2 },
                )

                2 -> OnboardingAnchor(
                    onNext = { step = 3 },
                    onPair = {
                        viewModel.setAwaitingTagPairing(true)
                        step = 3
                    },
                )

                else -> OnboardingReady(onDone = { viewModel.finishOnboarding() })
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == step) 22.dp else 8.dp, 8.dp)
                        .padding(0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = if (index == step) 22.dp else 8.dp, height = 8.dp)
                            .androidBackground(if (index == step) Accent else Color(0xFFD8D4CC), CircleShape),
                    )
                }
            }
        }
    }
}

private fun Modifier.androidBackground(color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.then(androidx.compose.foundation.background(color, shape))

@Composable
private fun OnboardingWelcome(onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(104.dp)
                .androidBackground(Charcoal, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, null, tint = Canvas, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("A physical key for your attention", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "FocusTap locks distracting apps until you tap an NFC tag or scan your QR anchor. Put the key in another room, then get to work.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.typography.bodyLarge.color.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
private fun OnboardingPermissions(
    accessibility: Boolean,
    deviceAdmin: Boolean,
    nfc: Boolean,
    notifications: Boolean,
    onOpen: (Intent) -> Unit,
    onRequestNotifications: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("Permissions", style = MaterialTheme.typography.headlineMedium)
        Text("FocusTap never reads your screen. These permissions only let it notice and block apps.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        SectionCard {
            PermissionRow(
                title = "Accessibility",
                subtitle = "Detects when a blocked app opens",
                granted = accessibility,
                action = {
                    onOpen(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )
            Spacer(Modifier.height(14.dp))
            PermissionRow(
                title = "Device admin",
                subtitle = "Required for strict mode safeguards",
                granted = deviceAdmin,
                action = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            FocusDeviceAdminReceiver.componentName(context),
                        )
                        .putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Allows FocusTap's strict mode to guard active sessions.",
                        )
                    onOpen(intent)
                },
            )
            Spacer(Modifier.height(14.dp))
            PermissionRow(
                title = "NFC",
                subtitle = if (nfc) "NFC hardware available" else "Optional — QR anchor works too",
                granted = nfc,
                action = { onOpen(Intent(Settings.ACTION_NFC_SETTINGS)) },
            )
            Spacer(Modifier.height(14.dp))
            PermissionRow(
                title = "Notifications",
                subtitle = "Shows the active session timer and shield shortcut",
                granted = notifications,
                action = onRequestNotifications,
            )
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Set up later") }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    action: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.6f))
        }
        if (granted) {
            Icon(Icons.Default.Check, null, tint = Success)
        } else {
            OutlinedButton(onClick = action) { Text("Open") }
        }
    }
}

@Composable
private fun OnboardingAnchor(onNext: () -> Unit, onPair: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Choose your anchor", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Write a token to an NTAG215 tag or print the QR code from the Pair tab. You'll need it to end every session.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onPair, modifier = Modifier.fillMaxWidth()) { Text("Pair a tag now") }
        TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("I'll pair later") }
    }
}

@Composable
private fun OnboardingReady(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Ready when you are", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Start a mode from the dashboard. While locked, blocked apps show a shield. Only your anchor ends the session — with three emergency unlocks per month as a safety valve.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Open FocusTap") }
    }
}
