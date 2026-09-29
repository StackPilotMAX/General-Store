package com.stackpilotmax.rameshvegetableshop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stackpilotmax.rameshvegetableshop.data.LedgerRules
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

internal val DeepBlue = DiwaliMaroon
internal val SkyBlue = DiwaliSaffron
internal val Lavender = DiwaliPurple
internal val LeafGreen = DiwaliGreen
internal val Mint = Color(0xFFFFF0CE)
internal val SoftBackground = Color(0xFFFFF8EA)
internal val MutedText = Color(0xFF765B68)
internal val WarmOrange = Color(0xFFFF6A00)
internal val ErrorRed = Color(0xFFD43A4C)
internal val Ink = Color(0xFF2B1630)

private val SabziColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0A4),
    onPrimaryContainer = DeepBlue,
    secondary = Lavender,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2D8FF),
    onSecondaryContainer = DeepBlue,
    tertiary = LeafGreen,
    onTertiary = Color.White,
    tertiaryContainer = Mint,
    onTertiaryContainer = Color(0xFF155D3A),
    background = SoftBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = ErrorRed
)

@Composable
internal fun SabziBillTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SabziColors, content = content)
}

internal data class WorldScene(
    val key: String,
    val eyebrow: String,
    val title: String,
    val body: String,
    val action: String,
    val icon: ImageVector,
    val accent: Color,
    val gradient: List<Color>,
    val islandEmoji: String,
    val orbitEmojis: List<String>,
    val badges: List<String>
)

@Composable
internal fun WorldTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFFE2A8), Color(0xFFF3D9FF), Color(0xFFFFEED1))
                )
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepBlue)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = DeepBlue)
            Text(subtitle, fontSize = 12.sp, color = MutedText)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun SectionHeading(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            eyebrow.uppercase(Locale.getDefault()),
            fontSize = 11.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Black,
            color = Lavender
        )
        Text(
            title,
            fontSize = 23.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            color = DeepBlue
        )
    }
}

@Composable
internal fun WorldSceneCard(
    scene: WorldScene,
    listState: LazyListState,
    motionEnabled: Boolean,
    onClick: () -> Unit
) {
    val distance by remember(listState, scene.key, motionEnabled) {
        derivedStateOf {
            if (!motionEnabled) return@derivedStateOf 0f
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == scene.key }
                ?: return@derivedStateOf 0f
            val viewportSize = (info.viewportEndOffset - info.viewportStartOffset)
                .coerceAtLeast(1)
                .toFloat()
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val itemCenter = item.offset + item.size / 2f
            ((itemCenter - viewportCenter) / viewportSize).coerceIn(-1f, 1f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                translationY = -distance * 34f
                scaleX = 1f - abs(distance) * 0.055f
                scaleY = 1f - abs(distance) * 0.055f
                rotationZ = distance * 1.4f
                alpha = 1f - abs(distance) * 0.14f
            },
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(scene.gradient))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(scene.icon, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            scene.eyebrow.uppercase(Locale.getDefault()),
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            scene.title,
                            color = Color.White,
                            fontSize = 27.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    scene.body,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    scene.badges.take(3).forEach { badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                badge,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                DioramaIsland(
                    mainEmoji = scene.islandEmoji,
                    orbitEmojis = scene.orbitEmojis,
                    accent = scene.accent,
                    motionEnabled = motionEnabled,
                    distance = distance
                )

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DeepBlue
                    )
                ) {
                    Text(scene.action, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("  →", fontSize = 21.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DioramaIsland(
    mainEmoji: String,
    orbitEmojis: List<String>,
    accent: Color,
    motionEnabled: Boolean,
    distance: Float
) {
    val transition = rememberInfiniteTransition(label = "diorama")
    val floating by transition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            tween(1700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "diorama_float"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2100), RepeatMode.Reverse),
        label = "diorama_pulse"
    )
    val movement = if (motionEnabled) floating else 0f
    val islandScale = if (motionEnabled) pulse else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .graphicsLayer {
                translationX = -distance * 24f
                rotationY = distance * 5f
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 230.dp, height = 128.dp)
                .graphicsLayer {
                    rotationX = 58f
                    rotationZ = 45f
                    cameraDistance = 20f * density
                    scaleX = islandScale
                    scaleY = islandScale
                }
                .shadow(18.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.95f), accent.copy(alpha = 0.36f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(y = (-8f + movement).dp)
                .size(94.dp)
                .shadow(7.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
        ) {
            Text(mainEmoji, fontSize = 52.sp)
        }
        orbitEmojis.getOrNull(0)?.let {
            Text(
                it,
                fontSize = 31.sp,
                modifier = Modifier
                    .offset(x = (-92).dp, y = (25f - movement).dp)
                    .graphicsLayer { rotationZ = distance * 10f }
            )
        }
        orbitEmojis.getOrNull(1)?.let {
            Text(
                it,
                fontSize = 30.sp,
                modifier = Modifier
                    .offset(x = 92.dp, y = (34f + movement).dp)
                    .graphicsLayer { rotationZ = -distance * 10f }
            )
        }
        orbitEmojis.getOrNull(2)?.let {
            Text(
                it,
                fontSize = 27.sp,
                modifier = Modifier
                    .offset(x = 72.dp, y = (-54f - movement).dp)
                    .graphicsLayer { rotationZ = distance * 10f }
            )
        }
    }
}

@Composable
internal fun WorldConnector(label: String, motionEnabled: Boolean) {
    val transition = rememberInfiniteTransition(label = "connector")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "connector_glow"
    )
    Column(
        modifier = Modifier.fillMaxWidth().height(94.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(width = 64.dp, height = 66.dp)) {
            drawLine(
                brush = Brush.verticalGradient(listOf(SkyBlue, Lavender, LeafGreen)),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 8f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(15f, 11f),
                    if (motionEnabled) glow * 25f else 0f
                )
            )
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = Lavender.copy(alpha = if (motionEnabled) glow else 0.75f),
                radius = 6f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
        Text(label, fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun InfoPill(text: String, accent: Color = Lavender) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.11f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun EmptyWorldState(emoji: String, title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(38.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 58.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = DeepBlue,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        Text(body, fontSize = 14.sp, color = MutedText, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun QrDialog(
    vendorName: String,
    upiId: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pay $vendorName", fontWeight = FontWeight.Black, color = DeepBlue)
        },
        text = {
            if (upiId.isBlank()) {
                Text("Settings mein pehle UPI ID save kijiye.", color = MutedText)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val qr = remember(upiId, vendorName) {
                        BillShare.generateUpiQr(upiId, vendorName, 700)
                    }
                    qr?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Payment QR",
                            modifier = Modifier.size(240.dp)
                        )
                    }
                    Text(upiId, fontWeight = FontWeight.Black, color = DeepBlue)
                    Text("UPI app se scan karein", fontSize = 12.sp, color = MutedText)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

internal fun moneyText(value: Double): String = LedgerRules.moneyText(value)

internal fun dateText(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
