package com.focustap.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import com.focustap.app.core.FocusController

/** Foreground-dispatch NFC handler shared by the main and shield activities. */
class NfcAnchorManager(private val activity: Activity) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isAvailable: Boolean get() = adapter != null

    val isEnabled: Boolean get() = adapter?.isEnabled == true

    fun enable() {
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(IsoDep::class.java.name),
        )
        runCatching { adapter?.enableForegroundDispatch(activity, pendingIntent, null, techLists) }
    }

    fun disable() {
        runCatching { adapter?.disableForegroundDispatch(activity) }
    }

    fun handleIntent(intent: Intent?) {
        @Suppress("DEPRECATION")
        val tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag ?: return
        val id = tag.id?.joinToString("") { "%02X".format(it) } ?: return
        if (id.isBlank()) return
        FocusController.onAnchorScanned(id)
    }

    /**
     * Optionally writes a FocusTap NDEF record to an NTAG215. Pairing itself uses
     * the immutable tag UID, so writing is not required — it just makes the tag
     * self-describing if it is ever scanned by another device.
     */
    fun writeAnchorLabel(tag: Tag, label: String): Boolean {
        val record = NdefRecord.createMime(
            "application/vnd.focustap.anchor",
            label.toByteArray(Charsets.UTF_8),
        )
        val message = NdefMessage(arrayOf(record))
        val payloadSize = message.toByteArray().size
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    if (!ndef.isWritable || ndef.maxSize < payloadSize) return false
                    ndef.writeNdefMessage(message)
                    true
                } finally {
                    ndef.close()
                }
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    try {
                        formatable.format(message)
                        true
                    } finally {
                        formatable.close()
                    }
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
