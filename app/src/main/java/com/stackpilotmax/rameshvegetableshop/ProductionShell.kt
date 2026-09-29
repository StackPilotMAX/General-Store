package com.stackpilotmax.rameshvegetableshop

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stackpilotmax.rameshvegetableshop.data.BackupManager
import com.stackpilotmax.rameshvegetableshop.data.BackupSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SabziBillProductionRoot(viewModel: SabziViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screen by viewModel.screen.collectAsState()
    val vendorName by viewModel.vendorName.collectAsState()
    val upiId by viewModel.upiId.collectAsState()
    val selectedThemeKey by FestivalThemeStore.selected.collectAsState()
    val backupManager = remember { BackupManager(context) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var showPaymentQrDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var restoredSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var busy by remember { mutableStateOf(false) }

    BackHandler(enabled = screen != AppScreen.HOME && screen != AppScreen.SPLASH) {
        viewModel.navigateBack()
    }

    val createBackupFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri != null && payload != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(payload.toByteArray(Charsets.UTF_8))
                        } ?: error("Backup file open nahi hui")
                    }
                }.onSuccess {
                    Toast.makeText(context, "Backup safely save ho gaya", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, it.message ?: "Backup save nahi hua", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val chooseRestoreFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Backup file open nahi hui")
                    }
                }.onSuccess {
                    pendingRestore = it
                }.onFailure {
                    Toast.makeText(context, it.message ?: "Backup read nahi hui", Toast.LENGTH_LONG).show()
                }
                busy = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (screen == AppScreen.NEW_BILL) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides Density(
                    density.density,
                    (density.fontScale * 1.12f).coerceAtMost(1.35f)
                )
            ) {
                SabziBillRoot(viewModel)
            }
        } else {
            SabziBillRoot(viewModel)
        }

        if (screen == AppScreen.HOME) {
            ExtendedFloatingActionButton(
                onClick = { showPaymentQrDialog = true },
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                icon = { Icon(Icons.Default.QrCode2, contentDescription = "Payment QR", modifier = Modifier.size(30.dp)) },
                text = { Text("Payment QR", fontWeight = FontWeight.Black) },
                shape = RoundedCornerShape(18.dp)
            )
        }

        if (screen == AppScreen.SETTINGS) {
            ExtendedFloatingActionButton(
                onClick = { showBackupDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                text = { Text("Backup", fontWeight = FontWeight.Black) },
                shape = RoundedCornerShape(18.dp)
            )
            ExtendedFloatingActionButton(
                onClick = { showThemeDialog = true },
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Festival Theme") },
                text = { Text("Festival Theme", fontWeight = FontWeight.Black) },
                shape = RoundedCornerShape(18.dp)
            )
        }
    }

    if (showPaymentQrDialog) {
        PaymentQrKioskDialog(
            vendorName = vendorName,
            upiId = upiId,
            onDismiss = { showPaymentQrDialog = false }
        )
    }

    if (showThemeDialog) {
        FestivalThemePickerDialog(
            selectedKey = selectedThemeKey,
            onSelect = { key ->
                FestivalThemeStore.set(context, key)
                Toast.makeText(context, "${FestivalThemes.byKey(key).name} theme save ho gaya", Toast.LENGTH_SHORT).show()
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showBackupDialog = false },
            title = { Text("Data Backup & Restore", fontWeight = FontWeight.Black, color = DeepBlue) },
            text = {
                Text(
                    "Customers, bills, items, payments, revisions aur shop settings ek checksummed JSON file mein save honge. Backup file ko private jagah rakhein.",
                    color = MutedText
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backupManager.exportJson() }
                                .onSuccess { payload ->
                                    pendingExport = payload
                                    val date = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
                                    createBackupFile.launch("SabziBill-backup-$date.json")
                                    showBackupDialog = false
                                }
                                .onFailure {
                                    Toast.makeText(context, it.message ?: "Backup create nahi hua", Toast.LENGTH_LONG).show()
                                }
                            busy = false
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(" Export")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        chooseRestoreFile.launch(arrayOf("application/json", "text/plain"))
                        showBackupDialog = false
                    }
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Text(" Restore")
                }
            }
        )
    }

    pendingRestore?.let { rawBackup ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingRestore = null },
            title = { Text("Current data replace karein?", fontWeight = FontWeight.Black, color = ErrorRed) },
            text = {
                Text(
                    "Restore current customers, bills aur payments ko selected backup se replace karega. Pehle current backup export karna recommended hai.",
                    color = MutedText
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backupManager.importJson(rawBackup) }
                                .onSuccess {
                                    restoredSummary = it
                                    pendingRestore = null
                                }
                                .onFailure {
                                    Toast.makeText(context, it.message ?: "Restore fail hua", Toast.LENGTH_LONG).show()
                                }
                            busy = false
                        }
                    }
                ) { Text("Yes, Restore") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pendingRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    restoredSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { restoredSummary = null },
            title = { Text("Restore complete ✅", fontWeight = FontWeight.Black, color = LeafGreen) },
            text = {
                Text(
                    "${summary.customers} customers, ${summary.bills} bills, ${summary.items} items aur ${summary.payments} payments restore hue. Updated settings dekhne ke liye app ko ek baar close karke open karein.",
                    color = MutedText
                )
            },
            confirmButton = {
                TextButton(onClick = { restoredSummary = null }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun FestivalThemePickerDialog(
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hindu Festival Themes", fontWeight = FontWeight.Black, color = DeepBlue) },
        text = {
            LazyColumn {
                items(FestivalThemes.all, key = { it.key }) { theme ->
                    OutlinedButton(
                        onClick = { onSelect(theme.key) },
                        modifier = Modifier.padding(vertical = 3.dp).fillMaxSize(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        RadioButton(selected = theme.key == selectedKey, onClick = null)
                        Text("${theme.emoji}  ${theme.name}", fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun PaymentQrKioskDialog(
    vendorName: String,
    upiId: String,
    onDismiss: () -> Unit
) {
    var fixedMode by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    val fixedAmount = amountText.toDoubleOrNull()
    val qr = remember(upiId, vendorName, fixedMode, fixedAmount) {
        BillShare.generateUpiQr(
            upiId = upiId,
            vendorName = vendorName,
            size = 700,
            amount = if (fixedMode) fixedAmount else null
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment QR", fontWeight = FontWeight.Black, color = DeepBlue) },
        text = {
            if (upiId.isBlank()) {
                Text("Settings mein pehle UPI ID save kijiye.", color = MutedText)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (fixedMode) "Fixed amount QR — scan karte hi amount pre-filled hoga"
                        else "Normal merchant QR — customer amount khud dalega",
                        color = MutedText
                    )
                    qr?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Payment QR",
                            modifier = Modifier.size(240.dp)
                        )
                    }
                    Text(upiId, fontWeight = FontWeight.Black, color = DeepBlue)
                    if (fixedMode) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Fixed amount ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = { fixedMode = !fixedMode },
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(if (fixedMode) "Use Normal QR" else "Use Fixed Amount QR")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
