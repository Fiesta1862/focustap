package com.focustap.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.focustap.app.data.AppState
import com.focustap.app.data.FocusSession
import com.focustap.app.ui.FocusViewModel
import com.focustap.app.ui.components.SectionCard
import com.focustap.app.ui.components.formatDuration
import com.focustap.app.ui.components.formatElapsed
import com.focustap.app.ui.theme.Accent
import com.focustap.app.ui.theme.Canvas
import com.focustap.app.ui.theme.Charcoal
import com.focustap.app.ui.theme.Line
import com.focustap.app.ui.theme.Surface
import com.focustap.app.ui.theme.Success
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: FocusViewModel,
    onScanQr: () -> Unit,
    onOpenPairing: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val elapsed = if (state.isLocked && state.lockStartedAt > 0L) now - state.lockStartedAt else 0L
    val stats = remember(state.sessions, now) { computeStats(state.sessions) }
    val scale by animateFloatAsState(if (state.isLocked) 1f else 0.94f, label = "status-scale")
    val ringColor by animateColorAsState(
        if (state.isLocked) Accent else Success,
        label = "status-ring",
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("FocusTap", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (state.isLocked) "Stay with it." else "What deserves your attention?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(28.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(236.dp)
                    .background(Surface, CircleShape)
                    .border(5.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (state.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = ringColor,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (state.isLocked) "LOCKED" else "UNLOCKED",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (state.isLocked) formatElapsed(elapsed) else state.activeMode.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.typography.bodyLarge.color.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text("Focus mode", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.modes.forEach { mode ->
                FilterChip(
                    selected = mode.id == state.activeModeId,
                    onClick = { if (!state.isLocked) viewModel.setActiveMode(mode.id) },
                    enabled = !state.isLocked,
                    label = { Text("${mode.symbol}  ${mode.name}") },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (state.isLocked) {
            SectionCard {
                Text("End this session", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap your paired NFC tag, or scan the QR anchor. You can also use an emergency unlock if you really need out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onScanQr, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan QR")
                    }
                    OutlinedButton(onClick = onOpenPairing, modifier = Modifier.weight(1f)) {
                        Text("Tag help")
                    }
                }
            }
        } else {
            Button(
                onClick = { viewModel.startFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.width(10.dp))
                Text("Start focus · ${state.activeMode.name}")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start with QR anchor")
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Today", formatDuration(stats.today), Modifier.weight(1f))
            StatCard("7 days", formatDuration(stats.week), Modifier.weight(1f))
            StatCard("Streak", "${stats.streak}d", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent sessions", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        if (state.sessions.isEmpty()) {
            SectionCard {
                Text(
                    "No sessions yet. Your completed focus blocks will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText(),
                )
            }
        } else {
            state.sessions.take(5).forEach { session -> SessionRow(session) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MutedText() = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.6f)

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Charcoal)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedText())
    }
}

@Composable
private fun SessionRow(session: FocusSession) {
    SectionCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (session.unlockReason == "emergency") Accent else Success, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.modeName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${formatTime(session.startedAt)} · ended by ${session.unlockReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText(),
                )
            }
            Text(formatDuration(session.endedAt - session.startedAt), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private data class Stats(val today: Long, val week: Long, val streak: Int)

private fun computeStats(sessions: List<FocusSession>): Stats {
    val todayStart = startOfDay(0)
    val weekStart = startOfDay(-6)
    val today = sessions.filter { it.startedAt >= todayStart }.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0L) }
    val week = sessions.filter { it.startedAt >= weekStart }.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0L) }

    val activeDays = sessions.map { dayKey(it.startedAt) }.toSet()
    var streak = 0
    var offset = 0
    while (activeDays.contains(dayKey(startOfDay(-offset)))) {
        streak++
        offset++
    }
    return Stats(today, week, streak)
}

private fun startOfDay(offsetDays: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
    return calendar.timeInMillis
}

private fun dayKey(time: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
}

private fun formatTime(time: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
}
