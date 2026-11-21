package com.focustap.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focustap.app.ui.components.EventToasts
import com.focustap.app.ui.components.rememberQrScanner
import com.focustap.app.ui.theme.Surface
import com.focustap.app.ui.screens.DashboardScreen
import com.focustap.app.ui.screens.ModesScreen
import com.focustap.app.ui.screens.OnboardingScreen
import com.focustap.app.ui.screens.PairingScreen
import com.focustap.app.ui.screens.SettingsScreen
import java.io.File

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("dashboard", "Today", Icons.Default.Dashboard),
    Tab("modes", "Modes", Icons.Default.Layers),
    Tab("pair", "Anchor", Icons.Default.Nfc),
    Tab("settings", "Settings", Icons.Default.Settings),
)

@Composable
fun FocusTapNavigation(viewModel: FocusViewModel = viewModel()) {
    EventToasts()
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var crashLog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val file = File(context.filesDir, "crash-log.txt")
        crashLog = runCatching {
            if (file.exists()) file.readText().take(12_000) else null
        }.getOrNull()
    }

    if (crashLog != null) {
        AlertDialog(
            onDismissRequest = { crashLog = null },
            title = { Text("FocusTap crashed") },
            text = { Text(crashLog?.take(800).orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { File(context.filesDir, "crash-log.txt").delete() }
                    crashLog = null
                }) { Text("Got it") }
            },
        )
    }

    if (!state.onboardingComplete) {
        OnboardingScreen(viewModel)
        return
    }

    val navController = rememberNavController()
    val scanQr = rememberQrScanner()

    Scaffold(
        bottomBar = { BottomBar(navController) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onScanQr = scanQr,
                    onOpenPairing = { navController.navigate("pair") },
                )
            }
            composable("modes") { ModesScreen(viewModel) }
            composable("pair") { PairingScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar(containerColor = Surface) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
