package com.stackpilotmax.rameshvegetableshop

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stackpilotmax.rameshvegetableshop.data.BillDraftItem
import java.util.Locale

@Composable
internal fun NewBillWorldScreen(viewModel: SabziViewModel) {
    val context = LocalContext.current
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

    var selectedUnit by remember { mutableStateOf("All") }
    var chosenVegetable by remember { mutableStateOf<VegetableOption?>(null) }
    var quantity by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var directMode by remember { mutableStateOf(false) }
    var directTotal by remember { mutableStateOf("") }
    var showTemporaryVegetableDialog by remember { mutableStateOf(false) }
    var temporaryVegetableName by remember { mutableStateOf("") }
    var temporaryVegetableUnit by remember { mutableStateOf("Kg") }

    fun openEditor(vegetable: VegetableOption, existing: BillDraftItem? = null) {
        chosenVegetable = vegetable
        quantity = existing?.quantity?.let(::moneyText).orEmpty()
        rate = existing?.rate?.let(::moneyText).orEmpty()
        directMode = existing?.directAmount != null
        directTotal = existing?.directAmount?.let(::moneyText).orEmpty()
    }

    fun vegetableFor(item: BillDraftItem): VegetableOption =
        viewModel.vegetables.firstOrNull { it.name == item.vegetableName }
            ?: VegetableOption(
                name = item.vegetableName,
                unit = item.unit,
                emoji = "🧺"
            )

    if (showTemporaryVegetableDialog) {
        AlertDialog(
            onDismissRequest = { showTemporaryVegetableDialog = false },
            title = {
                Column {
                    Text("🧺 Temporary vegetable", fontWeight = FontWeight.Black, color = DeepBlue)
                    Text("Sirf is bill ke liye", fontSize = 12.sp, color = MutedText)
                }
            },
            text = {
                Column {
                    Text(
                        "Jo sabzi regular list mein nahi hai uska naam likhiye. Yeh master vegetable list mein save nahi hogi.",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = temporaryVegetableName,
                        onValueChange = { temporaryVegetableName = it },
                        label = { Text("Vegetable name") },
                        supportingText = { Text("Example: Potato") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Unit select karein", fontWeight = FontWeight.Black, color = DeepBlue)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Kg", "Bunch").forEach { unit ->
                            FilterChip(
                                selected = temporaryVegetableUnit == unit,
                                onClick = { temporaryVegetableUnit = unit },
                                label = { Text(unit) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = temporaryVegetableName.trim().isNotEmpty(),
                    onClick = {
                        val typedName = temporaryVegetableName.trim()
                        val existing = cart.firstOrNull {
                            it.vegetableName.equals(typedName, ignoreCase = true)
                        }
                        val vegetable = viewModel.vegetables.firstOrNull {
                            it.name.equals(typedName, ignoreCase = true)
                        } ?: VegetableOption(
                            name = existing?.vegetableName ?: typedName,
                            unit = temporaryVegetableUnit,
                            emoji = "🧺"
                        )
                        selectedUnit = vegetable.unit
                        showTemporaryVegetableDialog = false
                        temporaryVegetableName = ""
                        openEditor(vegetable, existing)
                    }
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showTemporaryVegetableDialog = false }) { Text("Cancel") }
            }
        )
    }

    chosenVegetable?.let { vegetable ->
        AlertDialog(
            onDismissRequest = { chosenVegetable = null },
            title = {
                Column {
                    Text("${vegetable.emoji} ${vegetable.name}", fontWeight = FontWeight.Black, color = DeepBlue)
                    Text("Unit: ${vegetable.unit}", fontSize = 12.sp, color = MutedText)
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = decimalInput(it) },
                        label = { Text("Quantity (${vegetable.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = decimalInput(it) },
                        label = { Text("Rate per ${vegetable.unit}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Direct total", fontWeight = FontWeight.Black, color = DeepBlue)
                            Text("Quantity × rate ko override karega", fontSize = 11.sp, color = MutedText)
                        }
                        Switch(checked = directMode, onCheckedChange = { directMode = it })
                    }
                    AnimatedVisibility(visible = directMode, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        OutlinedTextField(
                            value = directTotal,
                            onValueChange = { directTotal = decimalInput(it) },
                            label = { Text("Direct item total") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    val calculated = if (directMode) {
                        directTotal.toDoubleOrNull()
                    } else {
                        val q = quantity.toDoubleOrNull()
                        val r = rate.toDoubleOrNull()
                        if (q != null && r != null) q * r else null
                    }
                    calculated?.let {
                        Text(
                            "Item total: ₹${moneyText(it)}",
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Black,
                            color = LeafGreen
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addOrUpdateItem(
                        vegetable = vegetable,
                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                        rate = rate.toDoubleOrNull() ?: 0.0,
                        directAmount = if (directMode) directTotal.toDoubleOrNull() else null
                    )
                    chosenVegetable = null
                }) { Text("Add / Update") }
            },
            dismissButton = {
                TextButton(onClick = { chosenVegetable = null }) { Text("Cancel") }
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
                    "Customer → sabzi → payment → save"
                } else {
                    "Same bill ID; old charge replace hoga"
                },
                onBack = viewModel::cancelBill
            )
        }

        item {
            BillingJourneyHeader(editingBillId != null)
        }

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
                        supportingText = { Text("Raju aur Rajesh ka khata kabhi mix nahi hoga") },
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
                        Text("Saved customer select karein", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedText)
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
                                        customer.name + customer.phone.takeIf { it.isNotBlank() }?.let { " • ${it.takeLast(4)}" }.orEmpty()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeading("Step 2", "Sabzi chuniye")
                VoiceVegetableButton(
                    vegetables = viewModel.vegetables,
                    onMatched = { vegetable ->
                        selectedUnit = vegetable.unit
                        openEditor(
                            vegetable,
                            cart.firstOrNull { it.vegetableName == vegetable.name }
                        )
                    }
                )
            }
        }

        item {
            VegetableSearchPanel(
                vegetables = viewModel.vegetables,
                onSelected = { vegetable ->
                    selectedUnit = vegetable.unit
                    openEditor(
                        vegetable,
                        cart.firstOrNull { it.vegetableName == vegetable.name }
                    )
                }
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    temporaryVegetableName = ""
                    temporaryVegetableUnit = "Kg"
                    showTemporaryVegetableDialog = true
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("➕ Temporary vegetable", fontWeight = FontWeight.Black)
            }
            Text(
                "Sirf current bill ke liye • master list update nahi hogi",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = MutedText
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Bunch", "Kg").forEach { unit ->
                    FilterChip(
                        selected = selectedUnit == unit,
                        onClick = { selectedUnit = unit },
                        label = { Text(unit) }
                    )
                }
            }
        }

        val filteredVegetables = viewModel.vegetables.filter {
            selectedUnit == "All" || it.unit == selectedUnit
        }
        items(filteredVegetables.chunked(2)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { vegetable ->
                    val existing = cart.firstOrNull { it.vegetableName == vegetable.name }
                    VegetableWorldTile(
                        vegetable = vegetable,
                        existing = existing,
                        modifier = Modifier.weight(1f)
                    ) { openEditor(vegetable, existing) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
                EmptyWorldState("🧺", "Basket abhi khaali hai", "Upar kisi sabzi par tap karke quantity aur rate add kijiye.")
            }
        } else {
            items(cart, key = { it.vegetableName }) { item ->
                val vegetable = vegetableFor(item)
                BillBasketRow(
                    item = item,
                    vegetable = vegetable,
                    onEdit = { openEditor(vegetable, item) },
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
                        vegetableTotal = billTotal,
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
                    if (editing) "Same bill ID aur safe debt replacement" else "4 simple steps • bade buttons • live total",
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
                modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(when { ambiguous -> "!"; isSelected -> "✓"; else -> "+" }, fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (ambiguous) WarmOrange else if (isSelected) LeafGreen else Lavender)
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
                    Text("₹${moneyText(amount)}", fontSize = 21.sp, fontWeight = FontWeight.Black, color = if (amount > 0) WarmOrange else LeafGreen)
                }
            }
        }
    }
}

@Composable
private fun VegetableWorldTile(
    vegetable: VegetableOption,
    existing: BillDraftItem?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (existing != null) Color(0xFFFFEFD0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (existing != null) 7.dp else 3.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(Color(0xFFFFE0A8), Color(0xFFF0EAFF)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(vegetable.emoji, fontSize = 31.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(vegetable.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = DeepBlue, textAlign = TextAlign.Center)
            Text(vegetable.unit, fontSize = 11.sp, color = Lavender, fontWeight = FontWeight.Bold)
            existing?.let {
                Text("₹${moneyText(it.amount)} added", fontSize = 11.sp, color = LeafGreen, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun BillBasketRow(
    item: BillDraftItem,
    vegetable: VegetableOption,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(vegetable.emoji, fontSize = 31.sp)
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
            Text("₹${moneyText(item.amount)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepBlue)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Lavender) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed) }
        }
    }
}

@Composable
private fun LiveBillSummary(
    previousDebt: Double,
    vegetableTotal: Double,
    paid: Double,
    projected: Double,
    editing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF7E8), Color.White)))
            .padding(16.dp)
    ) {
        SummaryLine("Selected customer ka current baki", previousDebt, WarmOrange)
        SummaryLine("Vegetable bill", vegetableTotal, DeepBlue)
        SummaryLine("Paid in this bill", paid, LeafGreen)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(if (editing) Color(0xFFF0EAFF) else Color(0xFFFFE3AD))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SAVE KE BAAD BAKI", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Lavender)
                AnimatedContent(targetState = projected, label = "projected_total") { value ->
                    Text("₹${moneyText(value)}", fontSize = 25.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: Double, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MutedText)
        Text("₹${moneyText(value)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
    }
}

private fun matchSpokenVegetable(
    spoken: String,
    vegetables: List<VegetableOption>
): VegetableOption? {
    val normalized = spoken.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9अ-ह]"), "")
    val aliases = linkedMapOf(
        "methi" to "Methi",
        "मेथी" to "Methi",
        "palak" to "Palak",
        "पालक" to "Palak",
        "soya" to "Soya",
        "सोया" to "Soya",
        "harakanda" to "Hara Kanda",
        "हराकांदा" to "Hara Kanda",
        "chinakothmir" to "China Kothmir",
        "dhaniya" to "China Kothmir",
        "pudina" to "Pudina",
        "chaulai" to "Chaulai",
        "moolikepatte" to "Mooli ke Patte",
        "desikothmir" to "Desi Kothmir",
        "mooli" to "Mooli",
        "harimirch" to "Hari Mirch",
        "nayaadrak" to "Naya Adrak",
        "puranaadrak" to "Purana Adrak",
        "kadipatta" to "Kadi Patta",
        "nimbu" to "Nimbu",
        "gobhi" to "Gobhi",
        "gobi" to "Gobhi",
        "shimlamirch" to "Shimla Mirch",
        "chotalahsun" to "Chota Lahsun",
        "badalahsun" to "Bada Lahsun",
        "kakdi" to "Kakdi",
        "kheera" to "Kakdi"
    )
    val name = aliases.entries.firstOrNull { normalized.contains(it.key) }?.value
    return vegetables.firstOrNull { it.name == name }
        ?: vegetables.firstOrNull {
            normalized.contains(it.name.lowercase(Locale.getDefault()).replace(" ", ""))
        }
}

private fun decimalInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) filtered
    else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
}
