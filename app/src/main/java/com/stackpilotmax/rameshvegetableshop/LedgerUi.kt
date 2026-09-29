package com.stackpilotmax.rameshvegetableshop

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stackpilotmax.rameshvegetableshop.data.BillRevisionEntity
import com.stackpilotmax.rameshvegetableshop.data.BillStatus
import com.stackpilotmax.rameshvegetableshop.data.BillWithItems
import com.stackpilotmax.rameshvegetableshop.data.CustomerEntity
import com.stackpilotmax.rameshvegetableshop.data.PaymentEntity

@Composable
internal fun CustomerKhataWorldScreen(viewModel: SabziViewModel) {
    val customers by viewModel.customers.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val paymentHistory by viewModel.customerPayments.collectAsState()

    var editorCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showNewCustomer by remember { mutableStateOf(false) }
    var paymentCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var historyCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var deleteCustomerCandidate by remember { mutableStateOf<CustomerEntity?>(null) }

    if (showNewCustomer || editorCustomer != null) {
        CustomerEditorDialog(
            customer = editorCustomer,
            onDismiss = {
                showNewCustomer = false
                editorCustomer = null
            },
            onSave = { name, phone, openingDebt ->
                val existing = editorCustomer
                if (existing == null) {
                    viewModel.createCustomer(name, phone, openingDebt)
                } else {
                    viewModel.updateCustomer(existing.id, name, phone, openingDebt)
                }
                showNewCustomer = false
                editorCustomer = null
            }
        )
    }

    paymentCustomer?.let { customer ->
        PaymentDialog(
            customer = customer,
            balance = balances[customer.id] ?: customer.openingDebt,
            onDismiss = { paymentCustomer = null },
            onSave = { amount, note ->
                viewModel.receivePayment(customer.id, amount, note)
                paymentCustomer = null
            }
        )
    }

    historyCustomer?.let { customer ->
        PaymentHistoryDialog(
            customer = customer,
            payments = paymentHistory,
            onDismiss = { historyCustomer = null },
            onReverse = { paymentId -> viewModel.reversePayment(paymentId, customer.id) }
        )
    }

    deleteCustomerCandidate?.let { customer ->
        AlertDialog(
            onDismissRequest = { deleteCustomerCandidate = null },
            title = { Text("${customer.name} ko delete karein?", fontWeight = FontWeight.Black, color = ErrorRed) },
            text = {
                Text(
                    "Customer account aur uski payment history permanently delete hogi. Purane bills Bill Archive mein rahenge, lekin is khate se unlink ho jayenge. Delete karne se pehle backup recommended hai.",
                    color = MutedText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer.id)
                        deleteCustomerCandidate = null
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.size(5.dp))
                    Text("Delete Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCustomerCandidate = null }) { Text("Cancel") }
            }
        )
    }

    val totalDue = customers.sumOf { balances[it.id] ?: it.openingDebt }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            WorldTopBar(
                title = "Khata Desk",
                subtitle = "Har customer ka alag ID aur alag balance",
                onBack = viewModel::backHome,
                trailing = {
                    IconButton(onClick = { showNewCustomer = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add customer", tint = DeepBlue)
                    }
                }
            )
        }

        item {
            WorldSummaryHero(
                emoji = "📒",
                eyebrow = "Customer ledger world",
                title = "Raju ka khata Raju ka.\nRajesh ka Rajesh ka.",
                body = "Phone aur permanent customer ID se accounts separate rehte hain.",
                primaryValue = "₹${moneyText(totalDue)}",
                primaryLabel = "Total outstanding",
                secondaryValue = customers.size.toString(),
                secondaryLabel = "Customers"
            )
        }

        item {
            Button(
                onClick = { showNewCustomer = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(58.dp),
                shape = RoundedCornerShape(19.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text("Naya Customer / Opening Debt", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        if (customers.isEmpty()) {
            item {
                EmptyWorldState(
                    emoji = "👤",
                    title = "Abhi customer account nahi hai",
                    body = "Naya customer add karein ya pehla bill save karein."
                )
            }
        } else {
            items(customers, key = { it.id }) { customer ->
                CustomerLedgerCard(
                    customer = customer,
                    balance = balances[customer.id] ?: customer.openingDebt,
                    onPayment = { paymentCustomer = customer },
                    onHistory = {
                        historyCustomer = customer
                        viewModel.loadCustomerPayments(customer.id)
                    },
                    onEdit = { editorCustomer = customer },
                    onDelete = { deleteCustomerCandidate = customer }
                )
            }
        }
    }
}

@Composable
private fun CustomerLedgerCard(
    customer: CustomerEntity,
    balance: Double,
    onPayment: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFFFFE2AC), Color(0xFFEDE9FF)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(customer.name.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(customer.name, fontSize = 19.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                    Text(customer.phone.ifBlank { "Phone saved nahi" }, fontSize = 12.sp, color = MutedText)
                    if (customer.openingDebt > 0) {
                        Text("Opening debt ₹${moneyText(customer.openingDebt)}", fontSize = 11.sp, color = Lavender, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (balance >= 0) "Total baki" else "Advance credit", fontSize = 11.sp, color = MutedText)
                    AnimatedContent(targetState = balance, label = "ledger_balance") { value ->
                        Text(
                            "₹${moneyText(kotlin.math.abs(value))}",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black,
                            color = if (value > 0) WarmOrange else LeafGreen
                        )
                    }
                }
            }

            Spacer(Modifier.height(13.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPayment,
                    enabled = balance > 0,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(
                        "Receive Payment",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Payments", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Lavender, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit", color = Lavender, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Delete", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerEditorDialog(
    customer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit
) {
    var name by remember(customer?.id) { mutableStateOf(customer?.name.orEmpty()) }
    var phone by remember(customer?.id) { mutableStateOf(customer?.phone.orEmpty()) }
    var openingDebt by remember(customer?.id) {
        mutableStateOf(customer?.openingDebt?.let(::moneyText).orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (customer == null) "Naya Customer" else "Customer Edit",
                fontWeight = FontWeight.Black,
                color = DeepBlue
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() || char == '+' || char == ' ' || char == '-' } },
                    label = { Text("Phone / WhatsApp") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = openingDebt,
                    onValueChange = { openingDebt = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Opening debt") },
                    supportingText = { Text("Purane notebook ka baki sirf ek baar yahan daalein") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, openingDebt.toDoubleOrNull() ?: 0.0) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PaymentDialog(
    customer: CustomerEntity,
    balance: Double,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember(customer.id) { mutableStateOf("") }
    var note by remember(customer.id) { mutableStateOf("Cash payment") }
    val entered = amount.toDoubleOrNull() ?: 0.0
    val tooHigh = entered > balance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${customer.name} se payment", fontWeight = FontWeight.Black, color = DeepBlue) },
        text = {
            Column {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2E5))
                ) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current baki", color = MutedText)
                        Text("₹${moneyText(balance)}", fontWeight = FontWeight.Black, color = WarmOrange)
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Received amount") },
                    isError = tooHigh,
                    supportingText = {
                        if (tooHigh) Text("Payment baki se zyada nahi ho sakti")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (entered > 0 && !tooHigh) {
                    Text(
                        "Payment ke baad baki: ₹${moneyText(balance - entered)}",
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Black,
                        color = LeafGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(entered, note) }, enabled = entered > 0 && !tooHigh) {
                Text("Save Payment")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PaymentHistoryDialog(
    customer: CustomerEntity,
    payments: List<PaymentEntity>,
    onDismiss: () -> Unit,
    onReverse: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${customer.name} Payment History", fontWeight = FontWeight.Black, color = DeepBlue) },
        text = {
            if (payments.isEmpty()) {
                Text("Abhi koi payment entry nahi hai.", color = MutedText)
            } else {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(payments, key = { it.id }) { payment ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "₹${moneyText(payment.amount)}",
                                    fontWeight = FontWeight.Black,
                                    color = if (payment.isReversed) MutedText else LeafGreen
                                )
                                Text(payment.note, fontSize = 12.sp, color = MutedText)
                                Text(dateText(payment.createdAt), fontSize = 10.sp, color = MutedText)
                                if (payment.isReversed) Text("REVERSED", fontSize = 10.sp, color = ErrorRed, fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = { onReverse(payment.id) }, enabled = !payment.isReversed) {
                                Icon(Icons.Default.Undo, contentDescription = "Reverse payment", tint = if (payment.isReversed) MutedText else WarmOrange)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF0D8C6))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun BillArchiveWorldScreen(viewModel: SabziViewModel) {
    val context = LocalContext.current
    val bills by viewModel.bills.collectAsState()
    val vendorName by viewModel.vendorName.collectAsState()
    val upiId by viewModel.upiId.collectAsState()
    val shareStatement by viewModel.shareStatement.collectAsState()
    val revisions by viewModel.billRevisions.collectAsState()

    var voidCandidate by remember { mutableStateOf<BillWithItems?>(null) }
    var deleteBillCandidate by remember { mutableStateOf<BillWithItems?>(null) }
    var revisionBill by remember { mutableStateOf<BillWithItems?>(null) }

    LaunchedEffect(shareStatement) {
        shareStatement?.let { statement ->
            BillShare.shareBillImage(context, statement, vendorName, upiId)
                .onFailure {
                    Toast.makeText(context, it.message ?: "Bill share nahi hua", Toast.LENGTH_LONG).show()
                }
            viewModel.consumeShareStatement()
        }
    }

    voidCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { voidCandidate = null },
            title = { Text("Bill #${candidate.bill.id} void karein?", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Is bill ka outstanding contribution customer debt se reverse ho jayega. Record delete nahi hoga.",
                    color = MutedText
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.voidBill(candidate.bill.id)
                    voidCandidate = null
                }) { Text("Void & Reverse") }
            },
            dismissButton = { TextButton(onClick = { voidCandidate = null }) { Text("Cancel") } }
        )
    }

    deleteBillCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteBillCandidate = null },
            title = { Text("Bill #${candidate.bill.id} permanently delete?", fontWeight = FontWeight.Black, color = ErrorRed) },
            text = {
                Text(
                    "Bill, uske items aur audit revisions permanently delete honge. Customer balance turant recalculate hoga. Is action ko undo nahi kiya ja sakta; pehle backup recommended hai.",
                    color = MutedText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(candidate.bill.id)
                        deleteBillCandidate = null
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.size(5.dp))
                    Text("Delete Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteBillCandidate = null }) { Text("Cancel") }
            }
        )
    }

    revisionBill?.let { bill ->
        RevisionDialog(
            bill = bill,
            revisions = revisions,
            onDismiss = { revisionBill = null }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            WorldTopBar(
                title = "Bill Archive",
                subtitle = "Saved, edited aur voided records",
                onBack = viewModel::backHome
            )
        }
        item {
            WorldSummaryHero(
                emoji = "🗂️",
                eyebrow = "Audit-safe archive",
                title = "Har bill ka\napna permanent ID.",
                body = "Edit same bill ko replace karta hai; revision internally save hoti hai.",
                primaryValue = bills.count { it.bill.status == BillStatus.ACTIVE }.toString(),
                primaryLabel = "Active bills",
                secondaryValue = bills.count { it.bill.status == BillStatus.VOIDED }.toString(),
                secondaryLabel = "Voided"
            )
        }

        if (bills.isEmpty()) {
            item { EmptyWorldState("🧾", "Archive khaali hai", "Pehla bill save karne ke baad yahan dikhai dega.") }
        } else {
            items(bills, key = { it.bill.id }) { saved ->
                ArchiveBillCard(
                    saved = saved,
                    onEdit = { viewModel.editBill(saved.bill.id) },
                    onShare = { viewModel.prepareShare(saved.bill.id) },
                    onVoid = { voidCandidate = saved },
                    onDelete = { deleteBillCandidate = saved },
                    onAudit = {
                        revisionBill = saved
                        viewModel.loadBillRevisions(saved.bill.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ArchiveBillCard(
    saved: BillWithItems,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onVoid: () -> Unit,
    onDelete: () -> Unit,
    onAudit: () -> Unit
) {
    val active = saved.bill.status == BillStatus.ACTIVE
    val totalAtBill = saved.bill.previousDebtSnapshot +
        (saved.bill.total - saved.bill.amountPaid).coerceAtLeast(0.0)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) Color.White else Color(0xFFF3F3F5)
        ),
        elevation = CardDefaults.cardElevation(if (active) 5.dp else 1.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(saved.bill.customerName, fontSize = 19.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                        Spacer(Modifier.size(7.dp))
                        InfoPill(if (active) "SAVED" else "VOIDED", if (active) LeafGreen else ErrorRed)
                    }
                    Text(dateText(saved.bill.updatedAt), fontSize = 11.sp, color = MutedText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Bill #${saved.bill.id}", fontSize = 11.sp, color = MutedText)
                    Text("₹${moneyText(saved.bill.total)}", fontSize = 23.sp, fontWeight = FontWeight.Black, color = if (active) DeepBlue else MutedText)
                }
            }

            Spacer(Modifier.height(11.dp))
            saved.items.take(5).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.vegetableName} • ${moneyText(item.quantity)} ${item.unit}", fontSize = 12.sp, color = MutedText)
                    Text("₹${moneyText(item.amount)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                }
            }
            if (saved.items.size > 5) {
                Text("+ ${saved.items.size - 5} aur items", fontSize = 11.sp, color = Lavender, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(11.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFFFF7E8)).padding(12.dp)
            ) {
                ArchiveSummaryLine("Pehle ka debt snapshot", saved.bill.previousDebtSnapshot)
                ArchiveSummaryLine("Paid in bill", saved.bill.amountPaid, LeafGreen)
                ArchiveSummaryLine("Bill ke baad outstanding", if (active) totalAtBill else saved.bill.previousDebtSnapshot, WarmOrange)
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onEdit, enabled = active, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit")
                }
                Button(onClick = onShare, enabled = active, modifier = Modifier.weight(1.25f)) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Image Share")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextButton(onClick = onAudit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Audit")
                }
                TextButton(onClick = onVoid, enabled = active, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Void", color = if (active) ErrorRed else MutedText)
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp), tint = ErrorRed)
                    Spacer(Modifier.size(4.dp))
                    Text("Delete", color = ErrorRed)
                }
            }
        }
    }
}

@Composable
private fun ArchiveSummaryLine(label: String, value: Double, color: Color = DeepBlue) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MutedText)
        Text("₹${moneyText(value)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun RevisionDialog(
    bill: BillWithItems,
    revisions: List<BillRevisionEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bill #${bill.bill.id} Audit", fontWeight = FontWeight.Black, color = DeepBlue) },
        text = {
            if (revisions.isEmpty()) {
                Text("Is bill mein abhi koi previous revision nahi hai.", color = MutedText)
            } else {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(revisions, key = { it.id }) { revision ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Revision ${revision.revisionNumber}", fontWeight = FontWeight.Black, color = DeepBlue)
                                InfoPill(revision.reason, if (revision.reason == "VOIDED") ErrorRed else Lavender)
                            }
                            Text("Customer: ${revision.customerName}", fontSize = 12.sp, color = MutedText)
                            Text("Old total ₹${moneyText(revision.total)} • paid ₹${moneyText(revision.amountPaid)}", fontSize = 12.sp, color = MutedText)
                            Text(dateText(revision.createdAt), fontSize = 10.sp, color = MutedText)
                        }
                        HorizontalDivider(color = Color(0xFFF0D8C6))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun SettingsWorldScreen(viewModel: SabziViewModel) {
    val currentVendor by viewModel.vendorName.collectAsState()
    val currentUpi by viewModel.upiId.collectAsState()
    val currentMotion by viewModel.motionEnabled.collectAsState()

    var vendor by remember(currentVendor) { mutableStateOf(currentVendor) }
    var upi by remember(currentUpi) { mutableStateOf(currentUpi) }
    var motion by remember(currentMotion) { mutableStateOf(currentMotion) }

    val upiValid = upi.isBlank() || Regex("^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$").matches(upi.trim())

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            WorldTopBar(
                title = "Payment Kiosk",
                subtitle = "Shop name, QR aur motion settings",
                onBack = viewModel::backHome,
                trailing = {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = DeepBlue, modifier = Modifier.padding(end = 14.dp))
                }
            )
        }
        item {
            WorldSummaryHero(
                emoji = "📱",
                eyebrow = "Last world stop",
                title = "QR ready.\nDukaan ready.",
                body = "Settings local phone storage mein save hoti hain.",
                primaryValue = if (upi.isBlank()) "Not set" else "Ready",
                primaryLabel = "Payment QR",
                secondaryValue = if (motion) "Ultra" else "Calm",
                secondaryLabel = "Motion"
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(29.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionHeading("Shop identity", "Dukaan aur UPI")
                    Spacer(Modifier.height(13.dp))
                    OutlinedTextField(
                        value = vendor,
                        onValueChange = { vendor = it },
                        label = { Text("Vendor / shop name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = upi,
                        onValueChange = { upi = it.trim() },
                        label = { Text("UPI ID, e.g. ramesh@upi") },
                        isError = !upiValid,
                        supportingText = {
                            Text(if (upiValid) "QR landing page aur bill image mein use hoga" else "UPI ID format sahi nahi hai")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFFF4DEFF)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Ultra motion", fontWeight = FontWeight.Black, color = DeepBlue)
                            Text("Parallax, floating worlds aur camera transitions", fontSize = 11.sp, color = MutedText)
                        }
                        Switch(checked = motion, onCheckedChange = { motion = it })
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.saveSettings(vendor, upi, motion) },
                        enabled = vendor.isNotBlank() && upiValid,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.size(7.dp))
                        Text("Save Settings", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            AnimatedVisibility(visible = upi.isNotBlank() && upiValid, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = DeepBlue)
                            Spacer(Modifier.size(7.dp))
                            Text("🪔 Diwali QR Preview", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                        }
                        val qr = remember(upi, vendor) { BillShare.generateUpiQr(upi, vendor, 700) }
                        qr?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "UPI QR preview",
                                modifier = Modifier.size(245.dp).padding(top = 10.dp)
                            )
                        }
                        Text(upi, fontWeight = FontWeight.Black, color = Lavender)
                        Spacer(Modifier.height(10.dp))
                        Text("✨ शुभ लाभ • Safe UPI Payment ✨", fontSize = 11.sp, color = WarmOrange, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        SaveQrToPhoneButton(bitmap = qr, vendorName = vendor)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0D4))
            ) {
                Column(Modifier.padding(17.dp)) {
                    Text("🔒 Data Safety", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                    Text("• Bills aur khata Room database mein offline", fontSize = 12.sp, color = MutedText)
                    Text("• Bill images private temporary cache se share", fontSize = 12.sp, color = MutedText)
                    Text("• Same package ID + signing key se future updates", fontSize = 12.sp, color = MutedText)
                    Text("• Destructive migration disabled", fontSize = 12.sp, color = MutedText)
                }
            }
        }
    }
}

@Composable
private fun WorldSummaryHero(
    emoji: String,
    eyebrow: String,
    title: String,
    body: String,
    primaryValue: String,
    primaryLabel: String,
    secondaryValue: String,
    secondaryLabel: String
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFFFFE2AC), Color(0xFFF4DBFF), Color(0xFFFFEFD0)))
        ).padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(23.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 40.sp) }
                Column(Modifier.padding(start = 13.dp)) {
                    Text(eyebrow.uppercase(), fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black, color = Lavender)
                    Text(title, fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = MutedText)
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryStat(primaryValue, primaryLabel, WarmOrange, Modifier.weight(1f))
                SummaryStat(secondaryValue, secondaryLabel, Lavender, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
            Text(label, fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold)
        }
    }
}
