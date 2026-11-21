package com.focustap.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.focustap.app.core.FocusController
import com.focustap.app.qr.QrManager
import com.focustap.app.ui.FocusViewModel
import com.focustap.app.ui.components.SectionCard
import com.focustap.app.ui.components.rememberQrScanner
import com.focustap.app.ui.theme.Accent

@Composable
fun PairingScreen(viewModel: FocusViewModel) {
    val state by viewModel.state.collectAsState()
    val scanQr = rememberQrScanner()
    var waitingForTag by remember { mutableStateOf(false) }

    LaunchedEffect(state.pairedTagId) {
        if (state.pairedTagId != null) waitingForTag = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("Anchor keys", style = MaterialTheme.typography.headlineMedium)
        Text("Tap a tag or scan a code to start and end every session.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nfc, null, tint = Accent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("NFC tag", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    !FocusController.hasNfcHardware() -> "This device has no NFC hardware. Use the QR anchor below."
                    state.pairedTagId == null -> "No tag registered yet. An NTAG215 works well."
                    else -> "Registered tag: ${state.pairedTagId}"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.pairedTagId != null) {
                Spacer(Modifier.height(4.dp))
                Text(state.pairedTagId.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    waitingForTag = true
                    viewModel.setAwaitingTagPairing(true)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = FocusController.hasNfcHardware(),
            ) {
                Text(if (waitingForTag) "Now tap your tag…" else "Pair this tag")
            }
            if (state.pairedTagId != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.clearPairing() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Forget all anchors")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, null, tint = Accent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("QR anchor", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Screenshot or print this code and keep it away from your desk. Scanning it toggles your focus session.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            val secret = state.pairedQrSecret
            if (secret == null) {
                Button(
                    onClick = { viewModel.pairQr(QrManager.newSecret()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Generate QR anchor")
                }
            } else {
                val bitmap = remember(secret) { QrManager.generate(secret) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Your QR anchor",
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    secret,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = scanQr, modifier = Modifier.weight(1f)) { Text("Scan") }
                    OutlinedButton(
                        onClick = { viewModel.pairQr(QrManager.newSecret()) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Regenerate") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.setAwaitingQrPairing(true)
                        scanQr()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Use a different printed QR") }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("How pairing works", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "FocusTap stores the immutable UID of your NFC tag — the tag does not need to hold a secret. " +
                    "For QR anchors, the full token is stored locally and compared on every scan. " +
                    "Nothing is uploaded anywhere.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
