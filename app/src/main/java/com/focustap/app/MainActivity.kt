package com.focustap.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.focustap.app.nfc.NfcAnchorManager
import com.focustap.app.ui.FocusTapNavigation
import com.focustap.app.ui.theme.FocusTapTheme

class MainActivity : ComponentActivity() {

    private val nfc by lazy { NfcAnchorManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfc.handleIntent(intent)
        setContent {
            FocusTapTheme {
                FocusTapNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfc.enable()
    }

    override fun onPause() {
        super.onPause()
        nfc.disable()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfc.handleIntent(intent)
    }
}
