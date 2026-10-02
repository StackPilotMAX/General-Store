package com.stackpilotmax.rameshvegetableshop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val DiwaliMaroon = Color(0xFF0B5CAD)
internal val DiwaliPurple = Color(0xFF1565C0)
internal val DiwaliSaffron = Color(0xFF1976D2)
internal val DiwaliGold = Color(0xFF64B5F6)
internal val DiwaliCream = Color(0xFFEAF4FF)
internal val DiwaliRose = Color(0xFF42A5F5)
internal val DiwaliGreen = Color(0xFF00838F)

@Composable
internal fun DiwaliFestivalFrame(
    motionEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { FestivalThemeStore.load(context) }
    val selectedKey by FestivalThemeStore.selected.collectAsState()
    val theme = FestivalThemes.byKey(selectedKey)

    val transition = rememberInfiniteTransition(label = "festival_frame")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "festival_phase"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "festival_twinkle"
    )
    val p = if (motionEnabled) phase else 0.28f
    val glow = if (motionEnabled) twinkle else 0.8f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom, SoftBackground)))
    ) {
        content()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.72f }
        ) {
            val width = size.width
            val height = size.height
            val festiveColors = listOf(theme.accent, theme.primary, theme.secondary, theme.accent, LeafGreen)

            repeat(16) { index ->
                val lane = (index + 1f) / 17f
                val speed = 0.45f + (index % 5) * 0.11f
                val y = ((p * speed + index * 0.071f) % 1f) * height
                val sway = sin((p * 2f * PI + index).toDouble()).toFloat() * 18f
                val x = width * lane + sway
                drawCircle(
                    color = festiveColors[index % festiveColors.size].copy(alpha = 0.18f + glow * 0.25f),
                    radius = 2.5f + (index % 4) * 1.8f,
                    center = Offset(x.coerceIn(8f, width - 8f), y)
                )
            }

            fun firework(center: Offset, radius: Float, color: Color, offset: Float) {
                repeat(12) { ray ->
                    val angle = ray / 12f * 2f * PI.toFloat() + offset
                    val inner = radius * 0.28f
                    val outer = radius * (0.72f + glow * 0.22f)
                    val cosA = cos(angle.toDouble()).toFloat()
                    val sinA = sin(angle.toDouble()).toFloat()
                    drawLine(
                        color = color.copy(alpha = 0.14f + glow * 0.22f),
                        start = Offset(center.x + cosA * inner, center.y + sinA * inner),
                        end = Offset(center.x + cosA * outer, center.y + sinA * outer),
                        strokeWidth = 2.2f,
                        pathEffect = PathEffect.cornerPathEffect(4f)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.28f + glow * 0.3f),
                        radius = 2.4f,
                        center = Offset(center.x + cosA * outer, center.y + sinA * outer)
                    )
                }
            }

            firework(Offset(width * 0.10f, height * 0.17f), 58f, theme.accent, p * 4f)
            firework(Offset(width * 0.91f, height * 0.27f), 48f, theme.primary, -p * 3f)
            firework(Offset(width * 0.86f, height * 0.77f), 40f, theme.secondary, p * 2.4f)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 3.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(theme.emoji, fontSize = 16.sp)
            Text(
                "  ✨  ${theme.name} • SabziBill  ✨  ",
                modifier = Modifier.weight(1f),
                color = theme.primary.copy(alpha = 0.72f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
            Text(theme.emoji, fontSize = 16.sp)
        }
    }
}

@Composable
internal fun DiwaliDiyasRow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "diya_row")
    val glow by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "diya_glow"
    )
    val selectedKey by FestivalThemeStore.selected.collectAsState()
    val theme = FestivalThemes.byKey(selectedKey)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("🪔", fontSize = 22.sp, modifier = Modifier.graphicsLayer { scaleX = glow; scaleY = glow })
        Text(
            "━━━━ ✨ ━━━━ ✨ ━━━━",
            modifier = Modifier.weight(1f),
            color = theme.accent,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
        Text("🪔", fontSize = 22.sp, modifier = Modifier.graphicsLayer { scaleX = glow; scaleY = glow })
    }
}