package com.stackpilotmax.rameshvegetableshop

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stackpilotmax.rameshvegetableshop.data.BillDraftItem

@Composable
internal fun NewBillWorldScreen(viewModel: SabziViewModel) {
    val customers by viewModel.customers.collectAsState()
    val matchingCustomers by viewModel.matchingCustomers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val amountPaid by viewModel.amountPaid.collectAsState()
    val editingBillId by viewModel.editingBillId.collectAsState()
    val currentBalance by viewModel.currentCustomerBalance.collectAsState()
    val projectedBalance by viewModel.projectedCustomerBalance.collectAsState()
    val billTotal by viewModel.billTotal.collectAsState()

    var showItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BillDraftItem?>(null) }
    var itemName by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Pcs") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var directMode by remember { mutableStateOf(false) }
    var directTotal by remember { mutableStateOf("") }

    fun openItemEditor(item: BillDraftItem? = null) {
        editingItem = item
        itemName = item?.vegetableName.orEmpty()
        unit = item?.unit?.ifBlank { "Pcs" } ?: "Pcs"
        quantity = item?.quantity?.let(::moneyText).orEmpty()
        price = item?.rate?.let(::moneyText).orEmpty()
        directMode = item?.directAmount != null
        directTotal = item?.directAmount?.let(::moneyText).orEmpty()
        showItemDialog = true
    }

    if (showItemDialog) {
        val calculated = if (directMode) {
            directTotal.toDoubleOrNull()
        } else {
            val q = quantity.toDoubleOrNull()
            val p = price.toDoubleOrNull()
            if (q != null && p != null) q * p else null
        }

        AlertDialog(
            onDismissRequest = { showItemDialog = false },
            title = {
                Column {
                    Text(
                        if (editingItem == null) "🛒 Add store item" else "✏️ Edit store item",
                        fontWeight = FontWeight.Black,
                        color = DeepBlue
                    )
                    Text(
                        "Sirf is bill ke liye • item master list mein save nahi hoga",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item name") },
                        placeholder = { Text("e.g. Aashirvaad Atta 5kg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(9.dp))
                    Text("Unit", fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf("Pcs", "Kg", "g", "L", "ml", "Packet", "Box", "Bottle", "Dozen", "Set").forEach { choice ->
                            FilterChip(
                                selected = unit == choice,
                                onClick = { unit = choice },
                                label = { Text(choice) }
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = decimalInput(it) },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = decimalInput(it) },
                            label = { Text("Price / unit ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Direct total", fontWeight = FontWeight.Black, color = DeepBlue)
                            Text("Quantity × price ko override kare", fontSize = 11.sp, color = MutedText)
                        }
                        Switch(checked = directMode, onCheckedChange = { directMode = it })
                    }
                    if (directMode) {
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(
                            value = directTotal,
                            onValueChange = { directTotal = decimalInput(it) },
                            label = { Text("Direct item total ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    calculated?.let {
                        Text(
                            "Item total: ₹${moneyText(it)}",
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Black,
                            color = LeafGreen
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = itemName.trim().isNotBlank() &&
                        unit.isNotBlank() &&
                        quantity.toDoubleOrNull()?.let { it > 0 } == true &&
                        (if (directMode) directTotal.toDoubleOrNull()?.let { it >= 0 } == true
                        else price.toDoubleOrNull()?.let { it >= 0 } == true),
                    onClick = {
                        viewModel.addOrUpdateItem(
                            itemName = itemName,
                            unit = unit,
                            quantity = quantity.toDoubleOrNull() ?: 0.0,
                            rate = price.toDoubleOrNull() ?: 0.0,
                            directAmount = if (directMode) directTotal.toDoubleOrNull() else null
                        )
                        showItemDialog = false
                    }
                ) { Text(if (editingItem == null) "Add item" else "Update item") }
            },
            dismissButton = {
                TextButton(onClick = { showItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 34.dp)
    ) {
        item {
            WorldTopBar(
                title = if (editingBillId == null) "Billing Counter" else "Bill Edit Counter",
                subtitle = if (editingBillId == null) {
                    "Customer → items → payment → save"
                } else {
                    "Same bill ID; old charge replace hoga"
                },
                onBack = viewModel::cancelBill
            )
        }

        item { BillingJourneyHeader(editingBillId != null) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionHeading("Step 1", "Customer ka sahi account")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = viewModel::setCustomerName,
                        label = { Text("Customer name") },
                        supportingText = { Text("Har customer ka khata alag ID par rahega") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = viewModel::setCustomerPhone,
                        label = { Text("Phone / WhatsApp (recommended)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    CustomerIdentityCard(
                        customerName = customerName,
                        selectedName = selectedCustomer?.name,
                        selectedPhone = selectedCustomer?.phone,
                        currentBalance = currentBalance,
                        matchingCount = matchingCustomers.size
                    )
                    if (customers.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Saved customer select karein",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedText
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 8.dp)
                        ) {
                            items(
                                if (customerName.isBlank()) customers.take(12)
                                else matchingCustomers.ifEmpty { customers.take(8) },
                                key = { it.id }
                            ) { customer ->
                                OutlinedButton(onClick = { viewModel.selectCustomer(customer) }) {
                                    Text(
                                        customer.name +
                                            customer.phone.takeIf { it.isNotBlank() }
                                                ?.let { " • ${it.takeLast(4)}" }
                                                .orEmpty()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionHeading("Step 2", "Items add karein")
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Lavender,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "Koi catalogue maintain nahi karna. Har bill mein item name, unit, quantity aur price yahin enter karein.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MutedText
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { openItemEditor() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Item", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Text(
                        "Temporary item • bill save hone par sirf line item record rahega",
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }
        }

        item {
            SectionHeading(
                eyebrow = "Step 3",
                title = "Bill basket",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

        if (cart.isEmpty()) {
            item {
                EmptyWorldState(
                    "🛒",
                    "Basket abhi khaali hai",
                    "Add Item dabakar kisi bhi kirana item ka naam, unit, quantity aur price enter kijiye."
                )
            }
        } else {
            items(cart, key = { it.vegetableName }) { item ->
                GenericBillBasketRow(
                    item = item,
                    onEdit = { openItemEditor(item) },
                    onDelete = { viewModel.removeItem(item.vegetableName) }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(19.dp)) {
                    SectionHeading("Step 4", "Hisaab check karke save")
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = viewModel::setAmountPaid,
                        label = { Text("Aaj ke bill mein paid amount") },
                        supportingText = { Text("Purana debt payment Customer Khata se receive karein") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(14.dp))
                    LiveBillSummary(
                        previousDebt = currentBalance,
                        itemTotal = billTotal,
                        paid = amountPaid.toDoubleOrNull() ?: 0.0,
                        projected = projectedBalance,
                        editing = editingBillId != null
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::saveBill,
                        enabled = cart.isNotEmpty() && customerName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (editingBillId == null) "Save Bill" else "Update Same Bill",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (editingBillId != null) {
                        Text(
                            "Old contribution reverse/replaced hoga—corrected total dobara add nahi hoga.",
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = LeafGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingJourneyHeader(editing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    if (editing) {
                        listOf(Color(0xFFFFE8C7), Color(0xFFF2D7FF))
                    } else {
                        listOf(Color(0xFFFFE0A8), Color(0xFFFFF0CF))
                    }
                )
            )
            .padding(horizontal = 18.dp, vertical = 17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (editing) "✏️" else "🧾", fontSize = 42.sp)
            Column(Modifier.padding(start = 13.dp)) {
                Text(
                    if (editing) "Correction journey" else "Fast billing journey",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepBlue
                )
                Text(
                    if (editing) "Same bill ID aur safe debt replacement"
                    else "4 simple steps • item line entries • live total",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
        }
    }
}

@Composable
private fun CustomerIdentityCard(
    customerName: String,
    selectedName: String?,
    selectedPhone: String?,
    currentBalance: Double,
    matchingCount: Int
) {
    val isSelected = selectedName != null
    val ambiguous = !isSelected && matchingCount > 1
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                ambiguous -> Color(0xFFFFE9C9)
                isSelected -> Color(0xFFFFF0D4)
                else -> Color(0xFFF4DEFF)
            }
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        ambiguous -> "!"
                        isSelected -> "✓"
                        else -> "+"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        ambiguous -> WarmOrange
                        isSelected -> LeafGreen
                        else -> Lavender
                    }
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    when {
                        ambiguous -> "$customerName naam ke multiple customers"
                        isSelected -> "$selectedName ka account selected"
                        customerName.isBlank() -> "Customer name enter kijiye"
                        else -> "Naya customer account"
                    },
                    fontWeight = FontWeight.Black,
                    color = DeepBlue
                )
                Text(
                    when {
                        ambiguous -> "Phone ya saved list se sahi account select karein"
                        isSelected -> selectedPhone?.ifBlank { "Phone saved nahi" } ?: "Phone saved nahi"
                        else -> "Purana baki ₹0 se start hoga"
                    },
                    fontSize = 11.sp,
                    color = MutedText
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Purana baki", fontSize = 10.sp, color = MutedText)
                AnimatedContent(targetState = currentBalance, label = "customer_debt") { amount ->
                    Text(
                        "₹${moneyText(amount)}",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = if (amount > 0) WarmOrange else LeafGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun GenericBillBasketRow(
    item: BillDraftItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛍️", fontSize = 29.sp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(item.vegetableName, fontWeight = FontWeight.Black, color = DeepBlue)
                Text(
                    if (item.directAmount != null) {
                        "${moneyText(item.quantity)} ${item.unit} • Direct total"
                    } else {
                        "${moneyText(item.quantity)} ${item.unit} × ₹${moneyText(item.rate)}"
                    },
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
            Text(
                "₹${moneyText(item.amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = DeepBlue
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Lavender)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
        }
    }
}

@Composable
private fun LiveBillSummary(
    previousDebt: Double,
    itemTotal: Double,
    paid: Double,
    projected: Double,
    editing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFF7E8), Color.White)),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        SummaryLine("Selected customer ka current baki", previousDebt, WarmOrange)
        SummaryLine("Aaj ka item bill", itemTotal, DeepBlue)
        SummaryLine("Paid in this bill", paid, LeafGreen)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (editing) Color(0xFFF0EAFF) else Color(0xFFFFE3AD),
                    RoundedCornerShape(17.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SAVE KE BAAD BAKI", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Lavender)
                AnimatedContent(targetState = projected, label = "projected_total") { value ->
                    Text("₹${moneyText(value)}", fontSize = 25.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, amount: Double, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MutedText)
        Text("₹${moneyText(amount)}", fontWeight = FontWeight.Black, color = color)
    }
}
