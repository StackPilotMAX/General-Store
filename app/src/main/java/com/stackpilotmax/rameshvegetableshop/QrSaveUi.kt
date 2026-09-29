package com.stackpilotmax.rameshvegetableshop

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SaveQrToPhoneButton(
    bitmap: Bitmap?,
    vendorName: String
) {
    val context = LocalContext.current
    val latestBitmap by rememberUpdatedState(bitmap)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val qr = latestBitmap ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                check(qr.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "QR PNG save nahi hua"
                }
            } ?: error("Selected file open nahi hua")
        }.onSuccess {
            Toast.makeText(context, "QR phone mein save ho gaya ✅", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(
                context,
                it.message ?: "QR save nahi hua",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Button(
        onClick = {
            val safeVendor = vendorName
                .trim()
                .replace(Regex("[^A-Za-z0-9_-]+"), "-")
                .trim('-')
                .ifBlank { "Ramesh-Vegetable-Shop" }
            launcher.launch("SabziBill-$safeVendor-UPI-QR.png")
        },
        enabled = bitmap != null,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.Default.Download, contentDescription = null)
        Spacer(Modifier.size(7.dp))
        Text("QR Phone Mein Save Karein", fontWeight = FontWeight.Black)
    }
}
