package com.focustap.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focustap.app.ui.FocusViewModel
import com.focustap.app.ui.components.formatElapsed
import com.focustap.app.ui.components.rememberQrScanner
import com.focustap.app.ui.theme.Accent
import com.focustap.app.ui.theme.Canvas
import com.focustap.app.ui.theme.Charcoal
import kotlinx.coroutines.delay

@Composable
fun ShieldScreen(onFinished: () -> Unit) {
    val viewModel: FocusViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val scanQr = rememberQrScanner()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    BackHandler { /* The anchor is the only normal way out. */ }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    LaunchedEffect(state.isLocked) {
        if (!state.isLocked) onFinished()
    }

    val elapsed = if (state.lockStartedAt > 0L) (now - state.lockStartedAt).coerceAtLeast(0L) else 0L
    val cooldown = (state.emergencyCooldownUntil - now).coerceAtLeast(0L)
    val pulse = rememberInfiniteTransition(label = "shield-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shield-scale",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(112.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(Charcoal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, null, tint = Canvas, modifier = Modifier.size(46.dp))
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "FOCUS ACTIVE",
                style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 3.sp),
                color = Accent,
            )
            Spacer(Modifier.height(8.dp))
            Text(state.activeMode.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                formatElapsed(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = Charcoal,
            )
            Spacer(Modifier.height(30.dp))

            Icon(Icons.Default.Nfc, null, tint = Charcoal, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                "Tap your NFC Anchor Key to finish this session",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                "Or scan the printed QR anchor. Blocked apps stay shielded until then.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(34.dp))
            OutlinedButton(onClick = scanQr, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan QR anchor")
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { viewModel.emergencyUnlock() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.emergencyUnlocksRemaining > 0 && cooldown == 0L,
            ) {
                Text(
                    when {
                        cooldown > 0L -> "Emergency unlock · ${cooldown / 1000}s"
                        state.emergencyUnlocksRemaining == 0 -> "No emergency unlocks left"
                        else -> "Emergency unlock (${state.emergencyUnlocksRemaining} left)"
                    },
                )
            }
        }
    }
}
