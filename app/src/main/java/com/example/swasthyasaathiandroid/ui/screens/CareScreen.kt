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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*

@Composable
fun CareScreen(
    ui: UiText,
    // Pregnancy
    pregAge: String, onPregAgeChange: (String) -> Unit,
    pregWeek: String, onPregWeekChange: (String) -> Unit,
    pregBmi: String, onPregBmiChange: (String) -> Unit,
    pregSys: String, onPregSysChange: (String) -> Unit,
    pregDia: String, onPregDiaChange: (String) -> Unit,
    pregPreeclampsia: Boolean, onPreeclampsiaChange: (Boolean) -> Unit,
    pregDiabetes: Boolean, onDiabetesChange: (Boolean) -> Unit,
    pregPreterm: Boolean, onPretermChange: (Boolean) -> Unit,
    pregResult: PregnancyRiskResult?,
    pregError: String,
    onCalculateRisk: () -> Unit,
    onClearPreg: () -> Unit,
    // Trend tracking
    assessmentHistory: List<PregnancyAssessmentEntry> = emptyList(),
    // LLM Summary
    pregLlmSummary: String = "",
    // Guides
    careGuide: CareGuide?,
    onLoadGuide: (String) -> Unit,
    // Checklist
    checkedItems: Set<String> = emptySet(),
    onCheckItem: (String, Boolean) -> Unit = { _, _ -> },
    // Personalized tips
    contextTips: List<String> = emptyList(),
    // TTS
    onSpeak: (String) -> Unit = {},
    // Mental Health
    moodLevel: Int, onMoodChange: (Int) -> Unit,
    phqResult: PhqResult?,
    onNavigateToPhq9: () -> Unit,
    onClearPhq: () -> Unit,
    // Legacy simple reminders (kept for backward compat)
    medName: String, onMedNameChange: (String) -> Unit,
    medTime: String, onMedTimeChange: (String) -> Unit,
    reminders: List<MedicationReminder>,
    reminderError: String,
    onAddReminder: () -> Unit,
    onRemoveReminder: (Int) -> Unit,
    onClearMed: () -> Unit,
    // Alarm system (new)
    alarms: List<MedicationAlarm>,
    onAddAlarm: (MedicationAlarm) -> Unit,
    onRemoveAlarm: (Int) -> Unit,
    onToggleAlarm: (Int, Boolean) -> Unit,
    onScanPrescription: () -> Unit,
    prescriptionLoading: Boolean,
    prescriptionError: String,
    // Helplines
    onDial: (String) -> Unit,
    // Doctor Referral Export
    onShareReport: (String) -> Unit = {},
    // Voice Form Fill
    onVoiceFill: () -> Unit = {}
) {
    // Local state for alarm builder
    var alarmName by remember { mutableStateOf("") }
    var alarmDosage by remember { mutableStateOf("") }
    var alarmHour by remember { mutableStateOf(8) }
    var alarmMinute by remember { mutableStateOf(0) }
    var alarmDays by remember { mutableStateOf(setOf<Int>()) } // empty = daily
    var showTimePicker by remember { mutableStateOf(false) }
    var alarmBuilderError by remember { mutableStateOf("") }

    val dayLabels = listOf("M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7, "S" to 1)

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = alarmHour,
            initialMinute = alarmMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m -> alarmHour = h; alarmMinute = m; showTimePicker = false }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GradientHeaderCard(
                title = "Care Center",
                subtitle = "Pregnancy · Mental Health · Guides",
                icon = Icons.Filled.FavoriteBorder
            )
        }

        // ── Medicine Alarm ──
        item(key = "alarm_builder") {
            TintedCard(tint = CardTintAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Alarm, null, tint = Tertiary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Medicine Alarm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    // Scan prescription button
                    FilledTonalButton(
                        onClick = onScanPrescription,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Primary.copy(alpha = 0.12f), contentColor = Primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        enabled = !prescriptionLoading
                    ) {
                        if (prescriptionLoading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.DocumentScanner, null, Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Scan Rx", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("Set an alarm manually or scan a prescription/bill to auto-fill medicines.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        alarmName, { alarmName = it },
                        label = { Text("Medicine Name") },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Medication, null, Modifier.size(18.dp)) }
                    )
                    OutlinedTextField(
                        alarmDosage, { alarmDosage = it },
                        label = { Text("Dose") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                }

                // Time picker row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("%02d:%02d %s".format(
                            if (alarmHour % 12 == 0) 12 else alarmHour % 12,
                            alarmMinute,
                            if (alarmHour < 12) "AM" else "PM"
                        ), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Daily or pick days →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Days of week selector
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayLabels.forEach { (label, calDay) ->
                        val selected = calDay in alarmDays
                        FilterChip(
                            selected = selected,
                            onClick = {
                                alarmDays = if (selected) alarmDays - calDay else alarmDays + calDay
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Tertiary.copy(alpha = 0.18f),
                                selectedLabelColor = Tertiary
                            )
                        )
                    }
                }
                Text(
                    if (alarmDays.isEmpty()) "No days selected = alarm fires every day" else "Selected: ${alarmDays.size} day(s) per week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            alarmBuilderError = ""
                            if (alarmName.isBlank()) { alarmBuilderError = "Enter medicine name."; return@FilledTonalButton }
                            val newAlarm = MedicationAlarm(
                                id = (System.currentTimeMillis() / 1000).toInt(),
                                name = alarmName.trim(),
                                dosage = alarmDosage.trim(),
                                hour = alarmHour,
                                minute = alarmMinute,
                                daysOfWeek = alarmDays.toList(),
                                enabled = true
                            )
                            onAddAlarm(newAlarm)
                            alarmName = ""; alarmDosage = ""; alarmDays = setOf()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Tertiary.copy(alpha = 0.15f), contentColor = Tertiary)
                    ) {
                        Icon(Icons.Filled.AddAlarm, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Set Alarm")
                    }
                    OutlinedButton(onClick = { alarmName = ""; alarmDosage = ""; alarmDays = setOf(); alarmBuilderError = "" }, shape = RoundedCornerShape(12.dp)) {
                        Text(ui.clear)
                    }
                }
                if (alarmBuilderError.isNotBlank()) ErrorText(alarmBuilderError)
                if (prescriptionError.isNotBlank()) ErrorText(prescriptionError)
            }
        }

        // ── Alarm List ──
        if (alarms.isNotEmpty()) {
            item(key = "alarm_list_title") {
                Text("Scheduled Alarms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
            }
            items(alarms, key = { it.id }) { alarm ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (alarm.enabled) Tertiary.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Alarm, null,
                            tint = if (alarm.enabled) Tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alarm.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val timeStr = "%02d:%02d %s".format(
                                if (alarm.hour % 12 == 0) 12 else alarm.hour % 12,
                                alarm.minute,
                                if (alarm.hour < 12) "AM" else "PM"
                            )
                            val daysStr = if (alarm.daysOfWeek.isEmpty()) "Daily" else {
                                val dayMap = mapOf(1 to "Su", 2 to "Mo", 3 to "Tu", 4 to "We", 5 to "Th", 6 to "Fr", 7 to "Sa")
                                alarm.daysOfWeek.mapNotNull { dayMap[it] }.joinToString(" ")
                            }
                            Text("$timeStr · $daysStr${if (alarm.dosage.isNotBlank()) " · ${alarm.dosage}" else ""}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = alarm.enabled,
                            onCheckedChange = { onToggleAlarm(alarm.id, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Tertiary)
                        )
                        IconButton(onClick = { onRemoveAlarm(alarm.id) }) {
                            Icon(Icons.Filled.Delete, "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ── Pregnancy Risk Assessment ──
        item {
            TintedCard(tint = CardTintCoral) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PregnantWoman, null, tint = Secondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pregnancy Risk Assessment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pregAge, onPregAgeChange, label = { Text("Age") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(pregWeek, onPregWeekChange, label = { Text("Week") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(pregBmi, onPregBmiChange, label = { Text("BMI") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pregSys, onPregSysChange, label = { Text("BP Systolic") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(pregDia, onPregDiaChange, label = { Text("BP Diastolic") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                }
                // Voice Fill Button
                FilledTonalButton(
                    onClick = onVoiceFill,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = InfoBlue.copy(alpha = 0.12f),
                        contentColor = InfoBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Mic, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("🎤 Voice Fill — Say vitals aloud", style = MaterialTheme.typography.labelMedium)
                }
                HistoryToggle("Preeclampsia history", pregPreeclampsia, onPreeclampsiaChange)
                HistoryToggle("Gestational diabetes", pregDiabetes, onDiabetesChange)
                HistoryToggle("Preterm birth", pregPreterm, onPretermChange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onCalculateRisk,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Assessment, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Calculate Risk")
                    }
                    OutlinedButton(onClick = onClearPreg, shape = RoundedCornerShape(12.dp)) { Text(ui.clear) }
                }
                ErrorText(pregError)
            }
        }

        // ── Pregnancy Result ──
        if (pregResult != null) item {
            val riskColor = when { pregResult.score >= 60 -> ErrorRed; pregResult.score >= 30 -> WarningAmber; else -> SuccessGreen }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.08f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Risk gauge + category
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Visual risk gauge circle
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (pregResult.score / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(64.dp),
                                color = riskColor,
                                trackColor = riskColor.copy(alpha = 0.15f),
                                strokeWidth = 6.dp
                            )
                            Text("${pregResult.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = riskColor)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pregResult.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = riskColor)
                            Text(pregResult.action, style = MaterialTheme.typography.bodySmall)
                        }
                        // TTS button
                        IconButton(onClick = {
                            val text = "${pregResult.category}. ${pregResult.action}. ${pregResult.recommendations.joinToString(". ")}"
                            onSpeak(text)
                        }) {
                            Icon(Icons.Filled.VolumeUp, "Listen", tint = riskColor, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Risks
                    if (pregResult.risks.isNotEmpty()) {
                        HorizontalDivider(color = riskColor.copy(alpha = 0.2f))
                        pregResult.risks.forEach { risk ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = riskColor, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(8.dp))
                                Text(risk, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // ── Explainable AI Risk Breakdown ──
                    if (pregResult.breakdown.isNotEmpty()) {
                        HorizontalDivider(color = riskColor.copy(alpha = 0.2f))
                        Text("🧠 Risk Breakdown — Why this score?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InfoBlue)
                        Text("Each factor's contribution to the total score of ${pregResult.score}/100:",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(4.dp))
                        pregResult.breakdown.forEach { bd ->
                            val bdColor = when (bd.severity) {
                                "high" -> ErrorRed
                                "moderate" -> WarningAmber
                                else -> SuccessGreen
                            }
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = bdColor.copy(alpha = 0.06f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Points badge
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(bdColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (bd.points > 0) "+${bd.points}" else "✓",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = bdColor
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(bd.factor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(bd.explanation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Recommendations
                    if (pregResult.recommendations.isNotEmpty()) {
                        HorizontalDivider(color = riskColor.copy(alpha = 0.2f))
                        Text("💡 Recommendations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Primary)
                        pregResult.recommendations.forEach { rec ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = Primary, modifier = Modifier.padding(end = 6.dp))
                                Text(rec, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // LLM Summary
                    if (pregLlmSummary.isNotBlank()) {
                        HorizontalDivider(color = riskColor.copy(alpha = 0.2f))
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = InfoBlue.copy(alpha = 0.06f))) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🤖 AI Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InfoBlue)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { onSpeak(pregLlmSummary) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Filled.VolumeUp, "Listen", tint = InfoBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(pregLlmSummary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // ── Doctor Referral Export ──
        if (pregResult != null) item {
            val report = buildString {
                appendLine("═══════════════════════════════════")
                appendLine("   SWASTHYASAATHI – CLINICAL REPORT")
                appendLine("═══════════════════════════════════")
                appendLine()
                appendLine("📋 Patient Assessment Summary")
                appendLine("─────────────────────────────────")
                appendLine("Risk Score    : ${pregResult.score}/100")
                appendLine("Risk Category : ${pregResult.category}")
                appendLine("Action        : ${pregResult.action}")
                appendLine("Week          : ${pregResult.week}")
                appendLine()
                if (pregResult.risks.isNotEmpty()) {
                    appendLine("⚠️ Identified Risk Factors:")
                    pregResult.risks.forEach { appendLine("  • $it") }
                    appendLine()
                }
                if (pregResult.recommendations.isNotEmpty()) {
                    appendLine("💡 Recommendations:")
                    pregResult.recommendations.forEach { appendLine("  • $it") }
                    appendLine()
                }
                if (pregLlmSummary.isNotBlank()) {
                    appendLine("🤖 AI Clinical Summary:")
                    appendLine(pregLlmSummary)
                    appendLine()
                }
                appendLine("─────────────────────────────────")
                appendLine("Generated by SwasthyaSaathi App")
                appendLine("This is a screening tool, not a diagnosis.")
                appendLine("Please consult a qualified doctor.")
            }
            Button(
                onClick = { onShareReport(report) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("📄 Share Clinical Report with Doctor", style = MaterialTheme.typography.labelLarge)
            }
        }

        // ── Trend Chart ──
        if (assessmentHistory.size >= 2) item {
            TintedCard(tint = CardTintBlue) {
                Text("📊 Health Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                // Mini bar chart for risk scores
                Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    assessmentHistory.takeLast(10).forEach { entry ->
                        val barColor = when { entry.score >= 60 -> ErrorRed; entry.score >= 30 -> WarningAmber; else -> SuccessGreen }
                        val fraction = (entry.score / 100f).coerceIn(0.05f, 1f)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth(0.7f).fillMaxHeight(fraction).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(barColor.copy(alpha = 0.7f)))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("W${assessmentHistory.first().week}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("W${assessmentHistory.last().week}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // BP trend
                val lastBP = assessmentHistory.last()
                Text("Latest: BP ${lastBP.systolic}/${lastBP.diastolic} · Score ${lastBP.score} · ${lastBP.category}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Guides ──
        item {
            TintedCard(tint = CardTintGreen) {
                Text("Care Guides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    GuideChip("1st Tri", Icons.Filled.Looks3, Primary) { onLoadGuide("trimester_1") }
                    GuideChip("2nd Tri", Icons.Filled.Looks4, Primary) { onLoadGuide("trimester_2") }
                    GuideChip("3rd Tri", Icons.Filled.Looks5, Primary) { onLoadGuide("trimester_3") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GuideChip("Postpartum", Icons.Filled.ChildCare, Secondary) { onLoadGuide("postpartum") }
                    GuideChip("Meditation", Icons.Filled.SelfImprovement, Tertiary) { onLoadGuide("meditation") }
                }
            }
        }

        // ── Guide Content ──
        if (careGuide != null && careGuide.title.isNotBlank()) item {
            TintedCard(tint = CardTintAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(careGuide.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val fullText = "${careGuide.title}. ${careGuide.summary}. ${careGuide.points.joinToString(". ")}. Warning signs: ${careGuide.danger.joinToString(", ")}"
                        onSpeak(fullText)
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.VolumeUp, "Listen", tint = Primary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(careGuide.summary, style = MaterialTheme.typography.bodyMedium)

                if (careGuide.points.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Key Points", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Primary)
                    careGuide.points.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }

                // Interactive Checklist
                if (careGuide.checklistItems.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    val done = careGuide.checklistItems.count { it.key in checkedItems }
                    val total = careGuide.checklistItems.size
                    Text("✅ Daily Checklist ($done/$total)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                    LinearProgressIndicator(
                        progress = { if (total > 0) done.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = SuccessGreen, trackColor = SuccessGreen.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(4.dp))
                    careGuide.checklistItems.forEach { item ->
                        val isChecked = item.key in checkedItems
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onCheckItem(item.key, it) },
                                colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                            )
                            Text(item.text, style = MaterialTheme.typography.bodySmall,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Personalized AI Tips
                if (contextTips.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = InfoBlue.copy(alpha = 0.06f))) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🤖 Personalized Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InfoBlue)
                            contextTips.forEach { tip ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("💡", modifier = Modifier.padding(end = 6.dp))
                                    Text(tip, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (careGuide.danger.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.06f)), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("⚠ Seek Help If", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                            careGuide.danger.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = ErrorRed) }
                        }
                    }
                }
            }
        }

        // ── Mental Wellness ──
        item {
            TintedCard(tint = CardTintBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Psychology, null, tint = InfoBlue, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp))
                    Text("Mental Wellness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mood today:", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.width(12.dp))
                    listOf("😔", "😕", "😐", "🙂", "😊").forEachIndexed { i, emoji ->
                        val level = i + 1
                        FilledTonalButton(
                            onClick = { onMoodChange(level) }, shape = CircleShape,
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp),
                            colors = if (moodLevel == level) ButtonDefaults.filledTonalButtonColors(containerColor = InfoBlue.copy(alpha = 0.2f))
                            else ButtonDefaults.filledTonalButtonColors()
                        ) { Text(emoji) }
                        if (i < 4) Spacer(Modifier.width(4.dp))
                    }
                }
                Button(
                    onClick = onNavigateToPhq9, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.Psychology, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Take PHQ-9 Depression Screening", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── PHQ Result ──
        if (phqResult != null) item {
            val sevColor = when { phqResult.score >= 20 -> ErrorRed; phqResult.score >= 15 -> SOSRed; phqResult.score >= 10 -> WarningAmber; phqResult.score >= 5 -> Tertiary; else -> SuccessGreen }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = sevColor.copy(alpha = 0.08f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PHQ-9 Score: ${phqResult.score}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = sevColor)
                        Spacer(Modifier.width(8.dp)); InfoChip(phqResult.severity, sevColor)
                    }
                    Text(phqResult.action, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Helplines ──
        item {
            TintedCard(tint = CardTintCoral) {
                Text("Mental Health Helplines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDial("9152987821") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Phone, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("iCall")
                    }
                    OutlinedButton(onClick = { onDial("18602662345") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Phone, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Vandrevala")
                    }
                    OutlinedButton(onClick = { onDial("181") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Female, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("181")
                    }
                }
            }
        }
    }
}

// ── Time Picker Dialog ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Alarm Time", fontWeight = FontWeight.SemiBold) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HistoryToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun GuideChip(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = color.copy(alpha = 0.1f), contentColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
