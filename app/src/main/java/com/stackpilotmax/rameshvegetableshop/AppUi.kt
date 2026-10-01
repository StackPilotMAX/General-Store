package com.stackpilotmax.rameshvegetableshop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stackpilotmax.rameshvegetableshop.data.BillStatus

@Composable
fun SabziBillRoot(viewModel: SabziViewModel) {
    val screen by viewModel.screen.collectAsState()
    val motionEnabled by viewModel.motionEnabled.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    SabziBillTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = SoftBackground
        ) { padding ->
            DiwaliFestivalFrame(motionEnabled = motionEnabled) {
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    transitionSpec = {
                        if (motionEnabled) {
                            (fadeIn(tween(320)) +
                                slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 5 } +
                                scaleIn(initialScale = 0.985f))
                                .togetherWith(
                                    fadeOut(tween(180)) +
                                        slideOutHorizontally(tween(260)) { -it / 6 } +
                                        scaleOut(targetScale = 1.015f)
                                )
                        } else {
                            fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                        }
                    },
                    label = "sabzibill_screens"
                ) { destination ->
                    when (destination) {
                        AppScreen.SPLASH -> WorldSplash(motionEnabled)
                        AppScreen.HOME -> ScrollWorldHome(viewModel)
                        AppScreen.NEW_BILL -> NewBillWorldScreen(viewModel)
                        AppScreen.CUSTOMERS -> CustomerKhataWorldScreen(viewModel)
                        AppScreen.HISTORY -> BillArchiveWorldScreen(viewModel)
                        AppScreen.SETTINGS -> SettingsWorldScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldSplash(motionEnabled: Boolean) {
    val transition = rememberInfiniteTransition(label = "world_splash")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing)),
        label = "orbit"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    val actualOrbit = if (motionEnabled) orbit else 42f
    val actualPulse = if (motionEnabled) pulse else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFFFFF7E5), Color(0xFFFFDE9A), Color(0xFFF2D5FF), Color(0xFFFFE9C4))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(310.dp)) {
            drawCircle(Color.White.copy(alpha = 0.8f), radius = size.minDimension * 0.44f)
            drawCircle(
                DiwaliGold.copy(alpha = 0.42f),
                radius = size.minDimension * 0.47f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)
            )
            val angle = actualOrbit / 180f * Math.PI.toFloat()
            val radius = size.minDimension * 0.42f
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitX = kotlin.math.cos(angle.toDouble()).toFloat() * radius
            val orbitY = kotlin.math.sin(angle.toDouble()).toFloat() * radius
            val secondAngle = angle + 2.1f
            val orbitX2 = kotlin.math.cos(secondAngle.toDouble()).toFloat() * radius
            val orbitY2 = kotlin.math.sin(secondAngle.toDouble()).toFloat() * radius
            drawCircle(
                color = LeafGreen,
                radius = 14f,
                center = Offset(center.x + orbitX, center.y + orbitY)
            )
            drawCircle(
                color = Lavender,
                radius = 10f,
                center = Offset(center.x + orbitX2, center.y + orbitY2)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = actualPulse
                        scaleY = actualPulse
                        rotationZ = if (motionEnabled) {
                            kotlin.math.sin((actualOrbit / 60f).toDouble()).toFloat() * 2f
                        } else {
                            0f
                        }
                    }
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(listOf(SkyBlue, Lavender, LeafGreen))),
                contentAlignment = Alignment.Center
            ) {
                Text("🛍️🪔", fontSize = 52.sp)
            }
            Spacer(Modifier.height(26.dp))
            Text("Shiv Shakti Kirana", fontSize = 42.sp, fontWeight = FontWeight.Black, color = DeepBlue)
            Text("Shiv Shakti Kirana Store", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Lavender)
            Spacer(Modifier.height(12.dp))
            Text("✨ Shiv Shakti Kirana • Saaf hisaab ✨", fontSize = 13.sp, color = MutedText)
        }
    }
}

@Composable
private fun ScrollWorldHome(viewModel: SabziViewModel) {
    val vendorName by viewModel.vendorName.collectAsState()
    val upiId by viewModel.upiId.collectAsState()
    val motionEnabled by viewModel.motionEnabled.collectAsState()
    val activeBillCountVisible by viewModel.activeBillCountVisible.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val listState = rememberLazyListState()
    var showQr by remember { mutableStateOf(false) }

    if (showQr) {
        QrDialog(vendorName, upiId, onDismiss = { showQr = false })
    }

    val totalDue = customers.sumOf { balances[it.id] ?: it.openingDebt }
    val scenes = remember {
        listOf(
            WorldScene(
                key = "billing-counter",
                eyebrow = "Scene 01 • Shop counter",
                title = "Naya Bill Banaiye",
                body = "Customer chuniye, koi bhi kirana item add kijiye aur live baki dekhte hue bill save kijiye.",
                action = "Billing Counter Kholen",
                icon = Icons.Default.AddShoppingCart,
                accent = SkyBlue,
                gradient = listOf(Color(0xFFB3324E), Color(0xFF7B1FA2)),
                islandEmoji = "🧾",
                orbitEmojis = listOf("🥬", "⚖️", "🥕"),
                badges = listOf("Offline", "Live total", "Edit safe")
            ),
            WorldScene(
                key = "khata-desk",
                eyebrow = "Scene 02 • Khata desk",
                title = "Har Customer Ka Alag Hisaab",
                body = "Raju ka debt Raju ke ID se, Rajesh ka Rajesh ke ID se. Payment aur opening debt ek jagah.",
                action = "Customer Khata Dekhein",
                icon = Icons.Default.AccountBalanceWallet,
                accent = LeafGreen,
                gradient = listOf(Color(0xFF2E7D32), Color(0xFFF08A18)),
                islandEmoji = "📒",
                orbitEmojis = listOf("👤", "₹", "✅"),
                badges = listOf("ID isolated", "Payments", "Opening debt")
            ),
            WorldScene(
                key = "bill-archive",
                eyebrow = "Scene 03 • Archive tunnel",
                title = "Purane Bill, Safe Revisions",
                body = "Bill edit karne par old charge replace hota hai. Void karne par debt reverse hota hai.",
                action = "Bill Archive Kholen",
                icon = Icons.Default.History,
                accent = Lavender,
                gradient = listOf(Color(0xFF7B1FA2), Color(0xFFD64B79)),
                islandEmoji = "🗂️",
                orbitEmojis = listOf("🧾", "✏️", "↩️"),
                badges = listOf("Revision audit", "Void", "Image share")
            ),
            WorldScene(
                key = "voice-garden",
                eyebrow = "Scene 04 • Voice garden",
                title = "Har Item Turant Add Karein",
                body = "Har item ke liye name, unit, quantity aur price turant enter kijiye; koi catalogue maintain nahi hota.",
                action = "Item Billing Shuru Karein",
                icon = Icons.Default.Mic,
                accent = WarmOrange,
                gradient = listOf(Color(0xFFFF7A00), Color(0xFFC73562)),
                islandEmoji = "🎙️",
                orbitEmojis = listOf("🌿", "🌶️", "🍋"),
                badges = listOf("Hindi", "Push to talk", "Confirm first")
            ),
            WorldScene(
                key = "payment-kiosk",
                eyebrow = "Scene 05 • Payment kiosk",
                title = "QR Aur Dukaan Settings",
                body = "UPI ID update kijiye, QR preview dekhiye aur animation mode control kijiye.",
                action = "Settings Kholen",
                icon = Icons.Default.Settings,
                accent = DeepBlue,
                gradient = listOf(Color(0xFF5A143F), Color(0xFF8A2D6B)),
                islandEmoji = "📱",
                orbitEmojis = listOf("▦", "₹", "⚙️"),
                badges = listOf("UPI QR", "Motion control", "Local settings")
            )
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item(key = "hero") {
            HomeWorldHero(
                vendorName = vendorName,
                customerCount = customers.size,
                billCount = bills.count { it.bill.status == BillStatus.ACTIVE },
                totalDue = totalDue,
                motionEnabled = motionEnabled,
                showActiveBillCount = activeBillCountVisible,
                onQr = { showQr = true },
                onToggleActiveBillCount = {
                    viewModel.setActiveBillCountVisible(!activeBillCountVisible)
                }
            )
        }

        item(key = "journey-title") {
            SectionHeading(
                eyebrow = "Your shop journey",
                title = "Scroll karke dukaan ki poori duniya dekhiye",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
            )
        }

        scenes.forEachIndexed { index, scene ->
            item(key = scene.key) {
                WorldSceneCard(scene, listState, motionEnabled) {
                    when (index) {
                        0 -> viewModel.startNewBill()
                        1 -> viewModel.open(AppScreen.CUSTOMERS)
                        2 -> viewModel.open(AppScreen.HISTORY)
                        3 -> viewModel.startNewBill()
                        else -> viewModel.open(AppScreen.SETTINGS)
                    }
                }
            }
            if (index != scenes.lastIndex) {
                item(key = "connector-$index") {
                    WorldConnector(
                        label = when (index) {
                            0 -> "Bill se customer khata update hota hai"
                            1 -> "Khata se bill archive linked hai"
                            2 -> "Archive se voice billing tak"
                            else -> "Aakhri stop: payment kiosk"
                        },
                        motionEnabled = motionEnabled
                    )
                }
            }
        }

        item(key = "home-footer") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 34.sp)
                    Text("Aapka data phone ke andar", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                    Text(
                        "Bills, customers aur khata offline Room database mein rehte hain. WhatsApp bill image temporary cache se share hoti hai.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeWorldHero(
    vendorName: String,
    customerCount: Int,
    billCount: Int,
    totalDue: Double,
    motionEnabled: Boolean,
    showActiveBillCount: Boolean,
    onQr: () -> Unit,
    onToggleActiveBillCount: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "home_hero")
    val drift by transition.animateFloat(
        initialValue = -5f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "hero_drift"
    )
    val actualDrift = if (motionEnabled) drift else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFE4AD), Color(0xFFF3D7FF), Color(0xFFFFF0D0), SoftBackground)
                )
            )
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .offset(y = actualDrift.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(Brush.linearGradient(listOf(SkyBlue, Lavender, LeafGreen))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🥬", fontSize = 35.sp)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Shiv Shakti World", fontSize = 27.sp, fontWeight = FontWeight.Black, color = DeepBlue)
                        Text(vendorName, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Lavender)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleActiveBillCount) {
                        Icon(
                            if (showActiveBillCount) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showActiveBillCount) "Hide active bill count" else "Show active bill count",
                            tint = DeepBlue
                        )
                    }
                    Card(
                        modifier = Modifier.size(62.dp).clickable(onClick = onQr),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "Payment QR",
                                tint = DeepBlue,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Namaste Ramesh ji 👋", fontSize = 15.sp, fontWeight = FontWeight.Black, color = LeafGreen)
            Text(
                "Ek scroll mein\npoori dukaan.",
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
                color = DeepBlue
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Billing counter se payment QR tak ek connected, animated aur offline journey.",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MutedText
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                HeroStat(customerCount.toString(), "Customers", LeafGreen, Modifier.weight(1f))
                if (showActiveBillCount) {
                    HeroStat(billCount.toString(), "Active Bills", Lavender, Modifier.weight(1f))
                }
                HeroStat("₹${moneyText(totalDue)}", "Total Baki", WarmOrange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, accent: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}
