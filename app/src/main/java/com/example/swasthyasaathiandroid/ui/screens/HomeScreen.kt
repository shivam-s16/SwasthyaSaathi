package com.example.swasthyasaathiandroid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.UiText
import com.example.swasthyasaathiandroid.ui.QuickActionButton
import com.example.swasthyasaathiandroid.ui.TintedCard
import com.example.swasthyasaathiandroid.ui.theme.*

@Composable
fun HomeScreen(
    ui: UiText,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Hero Banner ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.HealthAndSafety, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Namaste! 🙏",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "How can Didi help you today?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // ── Quick Actions Grid ──
        TintedCard(tint = CardTintGreen) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                item { QuickActionButton("Ask Didi", Icons.Filled.ChatBubble, Primary) { onNavigate("chat") } }
                item { QuickActionButton("Nuskhe", Icons.Filled.LocalFlorist, Secondary) { onNavigate("remedies") } }
                item { QuickActionButton("AI Diagnosis", Icons.Filled.AutoAwesome, InfoBlue) { onNavigate("image_scan") } }
                item { QuickActionButton("Care", Icons.Filled.FavoriteBorder, Tertiary) { onNavigate("care") } }
            }
        }

        // ── Feature Highlight Cards ──
        Text("Health Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Anemia Scan",
                subtitle = "Camera check",
                icon = Icons.Filled.RemoveRedEye,
                color = Secondary,
                modifier = Modifier.weight(1f)
            ) { onNavigate("image_scan") }
            FeatureCard(
                title = "Skin Check",
                subtitle = "Image analysis",
                icon = Icons.Outlined.Face,
                color = Tertiary,
                modifier = Modifier.weight(1f)
            ) { onNavigate("image_scan") }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Pregnancy",
                subtitle = "Risk check",
                icon = Icons.Filled.PregnantWoman,
                color = Primary,
                modifier = Modifier.weight(1f)
            ) { onNavigate("care") }
            FeatureCard(
                title = "Mental Health",
                subtitle = "PHQ-9 screen",
                icon = Icons.Filled.Psychology,
                color = InfoBlue,
                modifier = Modifier.weight(1f)
            ) { onNavigate("phq9") }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Pain Journal",
                subtitle = "Track symptoms",
                icon = Icons.Filled.Create,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            ) { onNavigate("pain_journal") }
            FeatureCard(
                title = "Cycle Tracker",
                subtitle = "Period calendar",
                icon = Icons.Filled.DateRange,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            ) { onNavigate("cycle_tracker") }
        }

        // ── Emergency banner ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SOSRed.copy(alpha = 0.08f))
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SOSRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, null, tint = SOSRed, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Emergency?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SOSRed)
                    Text("Tap to call ambulance (108) immediately", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:108") }
                        context.startActivity(callIntent)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SOSRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("📞 108") }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
