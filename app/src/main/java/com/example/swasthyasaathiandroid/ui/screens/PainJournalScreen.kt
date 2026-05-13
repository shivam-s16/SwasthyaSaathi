package com.example.swasthyasaathiandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val AllMoods = listOf("Happy", "Sad", "Irritated", "Tired", "Stressed", "Emotional", "Calm", "Anxious")
private val AllSymptoms = listOf("cramps", "headache", "nausea", "bloating", "back pain", "fatigue", "acne", "dizziness", "breast tenderness")
private val FlowOptions = listOf("None", "Spotting", "Light", "Medium", "Heavy")

@Composable
fun PainJournalScreen(
    localStore: LocalStore,
    onSpeak: (String) -> Unit = {},
    onShareReport: (String) -> Unit = {},
    onVoiceInput: () -> Unit = {},
    voiceText: String = ""
) {
    // State
    var painLevel by remember { mutableIntStateOf(0) }
    var flowIntensity by remember { mutableStateOf("None") }
    var selectedMoods by remember { mutableStateOf(setOf<String>()) }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(localStore.getAllPainEntries()) }
    var showHistory by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }
    var showEmergencyAlert by remember { mutableStateOf(false) }
    var emergencyMessage by remember { mutableStateOf("") }

    // Apply voice text to notes
    LaunchedEffect(voiceText) {
        if (voiceText.isNotBlank()) {
            // Try parsing voice commands
            val lower = voiceText.lowercase()
            val painMatch = Regex("""pain\s*(?:level)?\s*(\d{1,2})""").find(lower)
            if (painMatch != null) {
                val v = painMatch.groupValues[1].toIntOrNull()
                if (v != null && v in 0..10) painLevel = v
            } else {
                notes = if (notes.isBlank()) voiceText else "$notes $voiceText"
            }
        }
    }

    fun painColor(level: Int): Color = when {
        level <= 3 -> SuccessGreen
        level <= 6 -> WarningAmber
        else -> ErrorRed
    }

    fun checkEmergency(): Boolean {
        val severe = painLevel >= 8
        val heavyBleed = flowIntensity == "Heavy"
        val dizzy = "dizziness" in selectedSymptoms
        if (severe && heavyBleed) {
            emergencyMessage = "🚨 EMERGENCY: Severe pain (${painLevel}/10) with heavy bleeding detected.\n\n⚠ This may indicate:\n• Ectopic pregnancy\n• Miscarriage\n• Hemorrhage\n\n📞 Call 108 (ambulance) immediately.\n📋 ASHA Worker: Refer patient to nearest PHC urgently."
            showEmergencyAlert = true; return true
        }
        if (dizzy && heavyBleed) {
            emergencyMessage = "🚨 WARNING: Dizziness with heavy bleeding.\n\n⚠ Possible signs of:\n• Severe blood loss\n• Anemia crisis\n\n📞 Seek immediate medical attention.\n📋 ASHA Worker: Monitor vitals and arrange transport."
            showEmergencyAlert = true; return true
        }
        if (severe) {
            emergencyMessage = "⚠ HIGH PAIN ALERT: Pain level ${painLevel}/10.\n\nPersistent severe pain should be assessed by a doctor.\n\n💡 Consult a healthcare professional if pain persists > 2 days."
            showEmergencyAlert = true; return true
        }
        return false
    }

    fun saveEntry() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val entry = PainJournalEntry(
            id = editingId ?: 0,
            date = dateFormat.format(Date()),
            timestamp = System.currentTimeMillis(),
            painLevel = painLevel,
            flowIntensity = flowIntensity.lowercase(),
            moods = selectedMoods.toList(),
            symptoms = selectedSymptoms.toList(),
            notes = notes
        )
        if (editingId != null) localStore.updatePainEntry(entry) else localStore.savePainEntry(entry)
        entries = localStore.getAllPainEntries()
        // Reset
        painLevel = 0; flowIntensity = "None"; selectedMoods = emptySet(); selectedSymptoms = emptySet(); notes = ""; editingId = null
        saveSuccess = true
        checkEmergency()
    }

    // Emergency Alert Dialog
    if (showEmergencyAlert) {
        AlertDialog(
            onDismissRequest = { showEmergencyAlert = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Health Alert", color = ErrorRed, fontWeight = FontWeight.Bold) },
            text = { Text(emergencyMessage) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onSpeak(emergencyMessage); showEmergencyAlert = false }) {
                        Icon(Icons.Filled.VolumeUp, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Listen")
                    }
                    Button(onClick = {
                        onShareReport("SWASTHYASAATHI EMERGENCY ALERT\n\n$emergencyMessage\n\nGenerated: ${Date()}")
                        showEmergencyAlert = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                        Icon(Icons.Filled.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Share")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showEmergencyAlert = false }) { Text("Dismiss") } }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GradientHeaderCard(
                title = "Pain Journal",
                subtitle = "Track symptoms · Moods · Flow · Analytics",
                icon = Icons.Filled.Create
            )
        }

        // Tab selector
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("📝 Log" to false to false, "📜 History" to true to false, "📊 Analytics" to false to true).forEach { (pair, isAnalytics) ->
                    val (label, isHistory) = pair
                    val selected = if (isAnalytics) showAnalytics else if (isHistory) showHistory && !showAnalytics else !showHistory && !showAnalytics
                    FilterChip(
                        selected = selected,
                        onClick = { showHistory = isHistory; showAnalytics = isAnalytics },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.15f),
                            selectedLabelColor = Primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ═══════════════════════
        // LOG TAB
        // ═══════════════════════
        if (!showHistory && !showAnalytics) {
            // Pain Level Slider
            item {
                TintedCard(tint = CardTintCoral) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, tint = painColor(painLevel), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pain Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(painColor(painLevel).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$painLevel", fontWeight = FontWeight.Bold, color = painColor(painLevel),
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Slider(
                        value = painLevel.toFloat(),
                        onValueChange = { painLevel = it.toInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = painColor(painLevel), activeTrackColor = painColor(painLevel)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        when {
                            painLevel <= 2 -> "😊 Minimal pain"
                            painLevel <= 4 -> "😐 Mild discomfort"
                            painLevel <= 6 -> "😣 Moderate pain"
                            painLevel <= 8 -> "😰 Severe pain"
                            else -> "🚨 Extreme pain — seek medical attention"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = painColor(painLevel)
                    )
                }
            }

            // Flow Intensity
            item {
                TintedCard(tint = CardTintAmber) {
                    Text("🩸 Flow Intensity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        FlowOptions.forEach { option ->
                            val selected = flowIntensity == option
                            FilterChip(
                                selected = selected,
                                onClick = { flowIntensity = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Secondary.copy(alpha = 0.18f),
                                    selectedLabelColor = Secondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Mood Tracking
            item {
                TintedCard(tint = CardTintBlue) {
                    Text("😊 Mood Tracking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Select all that apply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(4.dp))
                    // Two rows of 4
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        AllMoods.take(4).forEach { mood ->
                            val sel = mood in selectedMoods
                            FilterChip(
                                selected = sel,
                                onClick = { selectedMoods = if (sel) selectedMoods - mood else selectedMoods + mood },
                                label = { Text(mood, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InfoBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = InfoBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        AllMoods.drop(4).forEach { mood ->
                            val sel = mood in selectedMoods
                            FilterChip(
                                selected = sel,
                                onClick = { selectedMoods = if (sel) selectedMoods - mood else selectedMoods + mood },
                                label = { Text(mood, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InfoBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = InfoBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Symptoms Checklist
            item {
                TintedCard(tint = CardTintGreen) {
                    Text("🩺 Symptoms Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    // 3 rows of 3
                    AllSymptoms.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { symptom ->
                                val sel = symptom in selectedSymptoms
                                FilterChip(
                                    selected = sel,
                                    onClick = { selectedSymptoms = if (sel) selectedSymptoms - symptom else selectedSymptoms + symptom },
                                    label = { Text(symptom, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Tertiary.copy(alpha = 0.15f),
                                        selectedLabelColor = Tertiary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Pad if row has fewer than 3
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Notes + Voice
            item {
                TintedCard(tint = CardTintAmber) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📝 Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(
                            onClick = onVoiceInput,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = InfoBlue.copy(alpha = 0.12f), contentColor = InfoBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Mic, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Voice", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("How do you feel today?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            // Save Button
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { saveEntry() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (editingId != null) "Update Entry" else "Save Entry")
                    }
                    if (editingId != null) {
                        OutlinedButton(
                            onClick = { painLevel = 0; flowIntensity = "None"; selectedMoods = emptySet(); selectedSymptoms = emptySet(); notes = ""; editingId = null },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Cancel") }
                    }
                }
                if (saveSuccess) {
                    Text("✅ Entry saved!", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                    LaunchedEffect(saveSuccess) { kotlinx.coroutines.delay(2000); saveSuccess = false }
                }
            }
        }

        // ═══════════════════════
        // HISTORY TAB
        // ═══════════════════════
        if (showHistory && !showAnalytics) {
            if (entries.isEmpty()) {
                item { Text("No journal entries yet. Start logging!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
            }
            // Read last log button
            if (entries.isNotEmpty()) {
                item {
                    FilledTonalButton(
                        onClick = {
                            val last = entries.first()
                            val text = "Last entry on ${last.date}. Pain level ${last.painLevel}. Flow: ${last.flowIntensity}. Moods: ${last.moods.joinToString(", ")}. Symptoms: ${last.symptoms.joinToString(", ")}. Notes: ${last.notes}"
                            onSpeak(text)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.VolumeUp, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("🔊 Read My Last Log")
                    }
                }
            }

            items(entries, key = { it.id }) { entry ->
                val pc = painColor(entry.painLevel)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = pc.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(pc.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${entry.painLevel}", fontWeight = FontWeight.Bold, color = pc, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.date, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Flow: ${entry.flowIntensity} · ${entry.moods.joinToString(", ").ifBlank { "—" }}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                painLevel = entry.painLevel; flowIntensity = entry.flowIntensity.replaceFirstChar { it.uppercase() }
                                selectedMoods = entry.moods.toSet(); selectedSymptoms = entry.symptoms.toSet(); notes = entry.notes
                                editingId = entry.id; showHistory = false; showAnalytics = false
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Edit, "Edit", tint = InfoBlue, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = {
                                localStore.deletePainEntry(entry.id)
                                entries = localStore.getAllPainEntries()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (entry.symptoms.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Symptoms: ${entry.symptoms.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = Tertiary)
                        }
                        if (entry.notes.isNotBlank()) {
                            Text("\"${entry.notes}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ═══════════════════════
        // ANALYTICS TAB
        // ═══════════════════════
        if (showAnalytics) {
            item {
                TintedCard(tint = CardTintBlue) {
                    Text("📊 Pain Trend (Last 14 Days)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    val recentEntries = entries.take(14).reversed()
                    if (recentEntries.isEmpty()) {
                        Text("No data yet. Log entries to see trends.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            recentEntries.forEach { e ->
                                val barColor = painColor(e.painLevel)
                                val fraction = (e.painLevel / 10f).coerceIn(0.05f, 1f)
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${e.painLevel}", style = MaterialTheme.typography.labelSmall, color = barColor)
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        Modifier.fillMaxWidth(0.7f).fillMaxHeight(fraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(barColor.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Symptom Frequency
            item {
                TintedCard(tint = CardTintGreen) {
                    Text("🩺 Symptom Frequency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    val freq = mutableMapOf<String, Int>()
                    entries.forEach { e -> e.symptoms.forEach { s -> freq[s] = (freq[s] ?: 0) + 1 } }
                    val sorted = freq.entries.sortedByDescending { it.value }
                    if (sorted.isEmpty()) {
                        Text("No symptom data yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        sorted.take(6).forEach { (symptom, count) ->
                            val maxCount = sorted.first().value.coerceAtLeast(1)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(symptom, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(100.dp))
                                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(
                                        Modifier.fillMaxHeight().fillMaxWidth(count.toFloat() / maxCount)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Tertiary.copy(alpha = 0.6f))
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Tertiary)
                            }
                        }
                    }
                }
            }

            // Mood Frequency
            item {
                TintedCard(tint = CardTintCoral) {
                    Text("😊 Mood Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    val moodFreq = mutableMapOf<String, Int>()
                    entries.forEach { e -> e.moods.forEach { m -> moodFreq[m] = (moodFreq[m] ?: 0) + 1 } }
                    val sortedMoods = moodFreq.entries.sortedByDescending { it.value }
                    if (sortedMoods.isEmpty()) {
                        Text("No mood data yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        sortedMoods.take(5).forEach { (mood, count) ->
                            val maxC = sortedMoods.first().value.coerceAtLeast(1)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(mood, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(80.dp))
                                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(
                                        Modifier.fillMaxHeight().fillMaxWidth(count.toFloat() / maxC)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Secondary.copy(alpha = 0.6f))
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Secondary)
                            }
                        }
                    }
                }
            }

            // Average Pain
            item {
                val avgPain = if (entries.isNotEmpty()) entries.map { it.painLevel }.average() else 0.0
                val heavyDays = entries.count { it.flowIntensity == "heavy" }
                TintedCard(tint = CardTintAmber) {
                    Text("📈 Summary Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("%.1f".format(avgPain), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = painColor(avgPain.toInt()))
                            Text("Avg Pain", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("${entries.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Primary)
                            Text("Total Logs", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("$heavyDays", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                            Text("Heavy Days", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
