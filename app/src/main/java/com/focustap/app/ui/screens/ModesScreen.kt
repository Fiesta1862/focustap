package com.focustap.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.focustap.app.data.FocusMode
import com.focustap.app.ui.FocusViewModel
import com.focustap.app.ui.components.SectionCard
import com.focustap.app.ui.theme.Accent
import com.focustap.app.ui.theme.Charcoal
import java.util.UUID

@Composable
fun ModesScreen(viewModel: FocusViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val apps = remember { loadLaunchableApps(context) }
    var editing by remember { mutableStateOf<FocusMode?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<FocusMode?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = FocusMode(
                        id = UUID.randomUUID().toString(),
                        name = "New mode",
                        symbol = "★",
                    )
                    creating = true
                },
                containerColor = Charcoal,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text("Focus modes", style = MaterialTheme.typography.headlineMedium)
            Text("Each mode chooses what gets locked. Phone, messages and maps stay available.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))

            state.modes.forEach { mode ->
                SectionCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .background(Charcoal, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(mode.symbol, color = androidx.compose.ui.graphics.Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mode.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${mode.blockedPackages.size} apps · ${mode.blockedDomains.size} sites",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.6f),
                            )
                        }
                        TextButton(onClick = {
                            editing = mode
                            creating = false
                        }) { Text("Edit") }
                        IconButton(onClick = { deleting = mode }) {
                            Icon(Icons.Default.Delete, null, tint = Accent)
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    editing?.let { mode ->
        ModeEditor(
            initial = mode,
            isNew = creating,
            apps = apps,
            onSave = { saved ->
                viewModel.saveMode(saved)
                editing = null
            },
            onCancel = { editing = null },
        )
    }

    deleting?.let { mode ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${mode.name}?") },
            text = { Text("This cannot be undone. Active sessions are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMode(mode.id)
                    deleting = null
                }) { Text("Delete", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ModeEditor(
    initial: FocusMode,
    isNew: Boolean,
    apps: List<AppItem>,
    onSave: (FocusMode) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var symbol by remember { mutableStateOf(initial.symbol) }
    val selectedPackages = remember { mutableStateListOf(*initial.blockedPackages.toTypedArray()) }
    val domains = remember { mutableStateListOf(*initial.blockedDomains.toTypedArray()) }
    var domainInput by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf("") }

    val filteredApps = remember(appFilter, apps) {
        if (appFilter.isBlank()) apps else apps.filter { it.label.contains(appFilter, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (isNew) "New focus mode" else "Edit focus mode") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Icon", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("◆", "▲", "☾", "●", "★", "◎").forEach { candidate ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(
                                    if (symbol == candidate) Charcoal else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(onClick = { symbol = candidate }, modifier = Modifier.size(38.dp)) {
                                Text(candidate, color = if (symbol == candidate) androidx.compose.ui.graphics.Color.White else Charcoal)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = appFilter,
                    onValueChange = { appFilter = it },
                    label = { Text("Find apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.height(220.dp).fillMaxWidth()) {
                    LazyColumn {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedPackages.contains(app.packageName),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPackages.add(app.packageName)
                                        else selectedPackages.remove(app.packageName)
                                    },
                                )
                                Text(app.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { Text("Add website domain") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val value = domainInput.trim().lowercase().removePrefix("https://").removePrefix("www.")
                        if (value.contains('.')) domains.add(value)
                        domainInput = ""
                    }),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    domains.take(6).forEach { domain ->
                        TextButton(onClick = { domains.remove(domain) }) { Text(domain) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = domainInput.trim().lowercase().removePrefix("https://").removePrefix("www.")
                if (trimmed.contains('.')) domains.add(trimmed)
                onSave(
                    FocusMode(
                        id = initial.id,
                        name = name.ifBlank { "Focus" },
                        symbol = symbol.ifBlank { "◆" },
                        blockedPackages = selectedPackages.distinct(),
                        blockedDomains = domains.distinct(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

private data class AppItem(val label: String, val packageName: String)

private fun loadLaunchableApps(context: Context): List<AppItem> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(launcherIntent, 0)
        .map { info -> AppItem(info.loadLabel(packageManager).toString(), info.activityInfo.packageName) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
