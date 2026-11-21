package com.focustap.app.qr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.IntentIntegrator
import java.util.EnumMap
import java.util.UUID

object QrManager {

    const val SCAN_REQUEST_CODE = 0x0C0DE

    fun newSecret(): String = "FOCUSTAP:ANCHOR:" + UUID.randomUUID().toString()

    fun generate(text: String, size: Int = 512): Bitmap? = try {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 2)
        }
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (bits[x, y]) Color.rgb(28, 28, 30) else Color.rgb(250, 248, 243))
                }
            }
        }
    } catch (_: Exception) {
        null
    }

    fun createScanIntent(activity: Activity): Intent = IntentIntegrator(activity)
        .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        .setPrompt("Scan your FocusTap Anchor Key")
        .setBeepEnabled(true)
        .createScanIntent()

    fun parseResult(resultCode: Int, data: Intent?): String? =
        IntentIntegrator.parseActivityResult(SCAN_REQUEST_CODE, resultCode, data)?.contents
}
