package com.stackpilotmax.rameshvegetableshop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun VegetableSearchPanel(
    vegetables: List<VegetableOption>,
    onSelected: (VegetableOption) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, vegetables) {
        VegetableSearchEngine.search(query, vegetables, limit = 5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DiwaliCream),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔎", fontSize = 25.sp)
                Column(Modifier.padding(start = 9.dp)) {
                    Text(
                        "Sabzi Search",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepBlue
                    )
                    Text(
                        "Naam galat ho tab bhi nearest sabzi dikhayega",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }

            Spacer(Modifier.height(11.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = WarmOrange)
                },
                label = { Text("Search: methi, shimla mirch, kakdi…") },
                supportingText = {
                    if (query.isNotBlank()) {
                        Text("Closest matches neeche tap karke Rate & Qty kholen")
                    }
                }
            )

            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(Modifier.padding(top = 10.dp)) {
                    results.forEachIndexed { index, result ->
                        val best = index == 0
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSelected(result.vegetable)
                                    query = ""
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (best) DiwaliGold.copy(alpha = 0.17f) else Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(Color.White, DiwaliGold.copy(alpha = 0.28f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(result.vegetable.emoji, fontSize = 24.sp)
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(
                                        result.vegetable.name,
                                        fontWeight = FontWeight.Black,
                                        color = DeepBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${result.vegetable.unit} • match: ${result.matchedTerm}",
                                        fontSize = 10.sp,
                                        color = MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    if (best) "BEST" else "NEAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (best) WarmOrange else Lavender
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
