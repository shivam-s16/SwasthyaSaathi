package com.example.swasthyasaathiandroid.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*

// ── Mode definition ──
private data class DiagnosticMode(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val hint: String,
    val category: String   // "general", "maternal", "child", "emergency"
)

@Composable
fun ImageScanScreen(
    ui: UiText,
    imageMode: String,
    onModeChange: (String) -> Unit,
    selectedImageUri: Uri?,
    imageResult: ImageAnalysisResult?,
    imageResultEnglish: ImageAnalysisResult?,
    imageLoading: Boolean,
    imageError: String,
    onPickImage: () -> Unit,
    onAnalyze: () -> Unit,
    onClear: () -> Unit,
    onSpeak: (String) -> Unit = {},
    onShareReport: (String) -> Unit = {}
) {
    val allModes = listOf(
        // General
        DiagnosticMode("general", "General", Icons.Filled.Search, Primary, "📷 Upload any health-related image for general AI analysis.", "general"),
        DiagnosticMode("medicine", "Rx Scanner", Icons.Filled.Medication, InfoBlue, "💊 Photo a medicine strip or prescription label to identify the drug.", "general"),
        // Maternal Health
        DiagnosticMode("nutrition", "Meal Nutrition", Icons.Filled.Restaurant, SuccessGreen, "🍲 Photo your meal to check iron, protein & maternal nutrition quality.", "maternal"),
        DiagnosticMode("anemia", "Anemia Check", Icons.Filled.RemoveRedEye, Secondary, "🔬 Photo the inner eyelid (conjunctiva) to screen for anemia.", "maternal"),
        DiagnosticMode("anemia_advanced", "Advanced Anemia", Icons.Filled.Favorite, ErrorRed, "🩸 Photo conjunctiva + palms + tongue + nails for detailed anemia screening.", "maternal"),
        DiagnosticMode("edema", "Pedal Edema", Icons.Filled.DirectionsWalk, WarningAmber, "🦶 Photo feet/ankles to detect swelling linked to preeclampsia.", "maternal"),
        DiagnosticMode("jaundice", "Jaundice", Icons.Filled.WbSunny, WarningAmber, "👁️ Photo eyes or skin to detect yellowing (jaundice).", "maternal"),
        // Child Health
        DiagnosticMode("neonatal", "Newborn Screen", Icons.Filled.ChildCare, Tertiary, "👶 Photo newborn to screen for jaundice, rashes, and danger signs.", "child"),
        DiagnosticMode("malnutrition", "Malnutrition", Icons.Filled.ChildCare, ErrorRed, "⚖️ Photo the child to screen for visible signs of malnutrition.", "child"),
        // Emergency Screening
        DiagnosticMode("cyanosis", "Cyanosis", Icons.Filled.Air, InfoBlue, "💙 Photo lips/fingertips to detect bluish color (low oxygen). EMERGENCY.", "emergency"),
        DiagnosticMode("stroke", "Stroke (FAST)", Icons.Filled.Warning, ErrorRed, "🚨 Photo face to check for drooping/asymmetry. EMERGENCY SCREENING.", "emergency"),
        DiagnosticMode("koilonychia", "Spoon Nails", Icons.Filled.PanTool, Secondary, "💅 Photo nails to check for spoon-shaped nails (chronic iron deficiency).", "emergency"),
        DiagnosticMode("goiter", "Goiter Check", Icons.Filled.Person, Tertiary, "🫁 Photo the neck to detect visible thyroid swelling.", "emergency"),
        DiagnosticMode("skin", "Skin Screening", Icons.Filled.Face, Tertiary, "🔬 Capture a close-up of the affected skin area for AI screening.", "general"),
    )

    val categories = listOf(
        "maternal" to "🤰 Maternal Health",
        "child" to "👶 Child Health",
        "emergency" to "🚨 Emergency",
        "general" to "🔍 General"
    )

    var selectedCategory by remember { mutableStateOf("maternal") }
    val filteredModes = allModes.filter { it.category == selectedCategory }
    val currentMode = allModes.find { it.key == imageMode } ?: allModes[0]

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GradientHeaderCard(
                title = "AI Diagnostics",
                subtitle = "15 clinical screenings • Zero-cost • No lab needed",
                icon = Icons.Filled.CameraAlt
            )
        }

        // ── Category Tabs ──
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (key, label) ->
                    val isSelected = selectedCategory == key
                    val tabColor = when (key) {
                        "maternal" -> Secondary
                        "child" -> Tertiary
                        "emergency" -> ErrorRed
                        else -> Primary
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tabColor.copy(alpha = 0.15f),
                            selectedLabelColor = tabColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // ── Mode Selection (filtered) ──
        item {
            TintedCard(tint = CardTintCoral) {
                Text("Diagnostic Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredModes.forEach { mode ->
                        val isSelected = imageMode == mode.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onModeChange(mode.key) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = { Icon(mode.icon, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = mode.color.copy(alpha = 0.15f),
                                selectedLabelColor = mode.color
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                // Contextual hint
                Spacer(Modifier.height(6.dp))
                Text(currentMode.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // ── Image Picker ──
        item {
            TintedCard(tint = CardTintGreen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selectedImageUri != null) SuccessGreen.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (selectedImageUri != null) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                            null,
                            tint = if (selectedImageUri != null) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedImageUri != null) "Image selected ✓" else ui.noImage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedImageUri != null) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap 'Pick Image' to select from gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(ui.pickImage)
                    }
                    FilledTonalButton(
                        onClick = onAnalyze,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Scanner, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(ui.analyzeImage)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClear) {
                        Text(ui.clear)
                    }
                }

                if (imageLoading) LoadingRow(ui.loading)
                ErrorText(imageError)
            }
        }

        // ── Result Card ──
        if (imageResult != null) item {
            val triageColor = when (imageResult.triage.lowercase()) {
                "red" -> ErrorRed
                "yellow" -> WarningAmber
                "green" -> SuccessGreen
                else -> Primary
            }
            val triageLabel = when (imageResult.triage.lowercase()) {
                "red" -> "🔴 HIGH RISK — Urgent Action Required"
                "yellow" -> "🟡 MODERATE — Monitor & Follow Up"
                "green" -> "🟢 LOW RISK — Continue Monitoring"
                else -> "⚪ Assessment Complete"
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = triageColor.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Triage Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(triageColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(triageLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = triageColor)
                        Spacer(Modifier.weight(1f))
                        if (imageResult.confidence.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    "Confidence: ${imageResult.confidence.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = triageColor.copy(alpha = 0.2f))

                    // Summary
                    Text(ui.imageResult, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(imageResult.summary, style = MaterialTheme.typography.bodyMedium)

                    // Explainable Observations
                    if (imageResult.observations.isNotEmpty()) {
                        HorizontalDivider(color = triageColor.copy(alpha = 0.2f))
                        Text("🔍 Clinical Observations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InfoBlue)
                        imageResult.observations.forEach { obs ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = InfoBlue, modifier = Modifier.padding(end = 6.dp))
                                Text(obs, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Caution
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(imageResult.caution, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                    }

                    // Next Steps
                    Text("📋 Next Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    imageResult.nextSteps.forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Primary)
                            Spacer(Modifier.width(8.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Disclaimer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "⚕️ This is an AI-assisted screening tool, not a medical diagnosis. Always consult a qualified healthcare professional.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Action Buttons: TTS + Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val text = "${imageResult.summary}. ${imageResult.caution}. ${imageResult.nextSteps.joinToString(". ")}"
                                onSpeak(text)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.VolumeUp, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Listen", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                val report = buildString {
                                    appendLine("═══════════════════════════════════")
                                    appendLine("   SWASTHYASAATHI — AI DIAGNOSTIC REPORT")
                                    appendLine("═══════════════════════════════════")
                                    appendLine()
                                    appendLine("Mode: ${currentMode.label}")
                                    appendLine("Triage: $triageLabel")
                                    appendLine("Confidence: ${imageResult.confidence}")
                                    appendLine()
                                    appendLine("📋 Summary:")
                                    appendLine(imageResult.summary)
                                    appendLine()
                                    if (imageResult.observations.isNotEmpty()) {
                                        appendLine("🔍 Clinical Observations:")
                                        imageResult.observations.forEach { appendLine("  • $it") }
                                        appendLine()
                                    }
                                    appendLine("⚠️ Caution: ${imageResult.caution}")
                                    appendLine()
                                    appendLine("📋 Next Steps:")
                                    imageResult.nextSteps.forEachIndexed { i, s -> appendLine("  ${i+1}. $s") }
                                    appendLine()
                                    appendLine("─────────────────────────────────")
                                    appendLine("Generated by SwasthyaSaathi AI Diagnostics")
                                    appendLine("This is a screening tool, not a diagnosis.")
                                }
                                onShareReport(report)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // English Translation
                    if (imageResultEnglish != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("English Translation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Text(imageResultEnglish.summary, style = MaterialTheme.typography.bodyMedium)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.06f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("⚠ ${imageResultEnglish.caution}", Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                        imageResultEnglish.nextSteps.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
