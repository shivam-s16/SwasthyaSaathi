package com.example.swasthyasaathiandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleTrackerScreen(
    localStore: LocalStore,
    onSpeak: (String) -> Unit = {},
    onShareReport: (String) -> Unit = {},
    groqApiKey: String = "",
    languageService: LanguageService? = null
) {
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    var settings by remember { mutableStateOf(localStore.getCycleSettings()) }
    var periodEntries by remember { mutableStateOf(localStore.getAllPeriodEntries()) }
    var cycleLength by remember { mutableIntStateOf(settings.cycleLength) }
    var periodDuration by remember { mutableIntStateOf(settings.periodDuration) }
    var lastPeriodStr by remember { mutableStateOf(settings.lastPeriodDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLogPeriod by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var logStartDate by remember { mutableStateOf("") }
    var logEndDate by remember { mutableStateOf("") }
    var logNotes by remember { mutableStateOf("") }
    var aiInsight by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }
    val painEntries = remember { localStore.getAllPainEntries() }
    val cal = Calendar.getInstance()
    var displayMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var displayYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=Calendar, 1=History

    // Dynamic average cycle length from history
    val avgCycleLength = remember(periodEntries) {
        val withLength = periodEntries.filter { it.cycleLength > 0 }
        if (withLength.isNotEmpty()) withLength.map { it.cycleLength }.average().toInt() else cycleLength
    }

    // Predictions using dynamic average
    val lastPeriodDate: Date? = runCatching { sdf.parse(lastPeriodStr) }.getOrNull()
    val effectiveCycle = if (periodEntries.any { it.cycleLength > 0 }) avgCycleLength else cycleLength
    val nextPeriodDate: Date? = lastPeriodDate?.let { Date(it.time + effectiveCycle.toLong() * 86400000L) }
    val ovulationDate: Date? = nextPeriodDate?.let { Date(it.time - 14L * 86400000L) }
    val fertileStart: Date? = ovulationDate?.let { Date(it.time - 3L * 86400000L) }
    val fertileEnd: Date? = ovulationDate?.let { Date(it.time + 2L * 86400000L) }
    val today = Date()
    val currentCycleDay = if (lastPeriodDate != null && today.after(lastPeriodDate))
        ((today.time - lastPeriodDate.time) / 86400000L + 1).toInt() else null
    val daysUntilNext = if (nextPeriodDate != null && nextPeriodDate.after(today))
        ((nextPeriodDate.time - today.time) / 86400000L).toInt() else null

    fun saveCycle() {
        val s = CycleSettings(lastPeriodStr, cycleLength, periodDuration)
        localStore.saveCycleSettings(s); settings = s
    }

    fun isInRange(date: Date, start: Date?, end: Date?): Boolean {
        if (start == null || end == null) return false
        return !date.before(start) && !date.after(end)
    }

    fun logPeriod() {
        if (logStartDate.isBlank()) return
        localStore.savePeriodEntry(PeriodEntry(startDate = logStartDate, endDate = logEndDate, notes = logNotes))
        localStore.recalculateCycleLengths()
        periodEntries = localStore.getAllPeriodEntries()
        // Update last period date to most recent
        val newest = periodEntries.firstOrNull()
        if (newest != null) { lastPeriodStr = newest.startDate; saveCycle() }
        logStartDate = ""; logEndDate = ""; logNotes = ""; showLogPeriod = false
    }

    // Date pickers
    if (showDatePicker) {
        val dps = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { lastPeriodStr = sdf.format(Date(it)); saveCycle() }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dps) }
    }
    if (showStartPicker) {
        val dps = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { logStartDate = sdf.format(Date(it)) }; showStartPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dps) }
    }
    if (showEndPicker) {
        val dps = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { logEndDate = sdf.format(Date(it)) }; showEndPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dps) }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { GradientHeaderCard(title = "Cycle Tracker", subtitle = "Period · Fertility · Ovulation Calendar", icon = Icons.Filled.DateRange) }

        // Tab selector
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("📅 Calendar" to 0, "📜 History" to 1).forEach { (label, idx) ->
                    FilterChip(selected = activeTab == idx, onClick = { activeTab = idx },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                }
            }
        }

        // Log Period Button
        item {
            Button(onClick = { showLogPeriod = !showLogPeriod }, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Log Period")
            }
        }

        // Log Period Form
        if (showLogPeriod) {
            item {
                TintedCard(tint = CardTintCoral) {
                    Text("📝 Log New Period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showStartPicker = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Text(if (logStartDate.isBlank()) "Start Date" else logStartDate, style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { showEndPicker = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Text(if (logEndDate.isBlank()) "End Date" else logEndDate, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedTextField(value = logNotes, onValueChange = { logNotes = it }, label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { logPeriod() }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
                        enabled = logStartDate.isNotBlank()) {
                        Icon(Icons.Filled.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Save Period")
                    }
                }
            }
        }

        // Settings
        item {
            TintedCard(tint = CardTintAmber) {
                Text("⚙ Cycle Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { showDatePicker = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (lastPeriodStr.isBlank()) "Select Last Period Date" else "Last Period: $lastPeriodStr")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Cycle: $cycleLength days", style = MaterialTheme.typography.labelSmall)
                        Slider(value = cycleLength.toFloat(), onValueChange = { cycleLength = it.toInt(); saveCycle() },
                            valueRange = 20f..45f, steps = 24, colors = SliderDefaults.colors(thumbColor = Secondary, activeTrackColor = Secondary))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Period: $periodDuration days", style = MaterialTheme.typography.labelSmall)
                        Slider(value = periodDuration.toFloat(), onValueChange = { periodDuration = it.toInt(); saveCycle() },
                            valueRange = 2f..10f, steps = 7, colors = SliderDefaults.colors(thumbColor = ErrorRed, activeTrackColor = ErrorRed))
                    }
                }
                if (periodEntries.any { it.cycleLength > 0 }) {
                    Text("📊 Avg cycle from history: $avgCycleLength days (using this for predictions)",
                        style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                }
            }
        }

        // ═══ CALENDAR TAB ═══
        if (activeTab == 0 && lastPeriodDate != null) {
            // Predictions
            item {
                TintedCard(tint = CardTintBlue) {
                    Text("🔮 Predictions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PredictionBadge("Day", "${currentCycleDay ?: "—"}", Primary, Modifier.weight(1f))
                        PredictionBadge("Next Period", "${daysUntilNext ?: "—"}d", ErrorRed, Modifier.weight(1f))
                        PredictionBadge("Ovulation", ovulationDate?.let { sdf.format(it).takeLast(5) } ?: "—", SuccessGreen, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    if (nextPeriodDate != null) Text("Next period: ${sdf.format(nextPeriodDate)}", style = MaterialTheme.typography.labelSmall)
                    if (fertileStart != null && fertileEnd != null)
                        Text("Fertile window: ${sdf.format(fertileStart)} → ${sdf.format(fertileEnd)}", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                    FilledTonalButton(onClick = {
                        onSpeak("Cycle day ${currentCycleDay ?: "unknown"}. Next period in ${daysUntilNext ?: "unknown"} days. Ovulation around ${ovulationDate?.let { sdf.format(it) } ?: "unknown"}.")
                    }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.VolumeUp, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("🔊 Listen")
                    }
                }
            }

            // Calendar
            item {
                TintedCard(tint = CardTintGreen) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth-- }) { Icon(Icons.Filled.ChevronLeft, "Prev") }
                        val monthCal2 = Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }
                        Text(SimpleDateFormat("MMMM yyyy", Locale.ROOT).format(monthCal2.time),
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        IconButton(onClick = { if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++ }) { Icon(Icons.Filled.ChevronRight, "Next") }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("S","M","T","W","T","F","S").forEach {
                            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val monthCal = Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }
                    val firstDow = monthCal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val periodEnd = lastPeriodDate?.let { Date(it.time + (periodDuration - 1) * 86400000L) }
                    val highPainDates = painEntries.filter { it.painLevel >= 7 }.map { it.date }.toSet()
                    // Build set of all logged period dates
                    val loggedPeriodDates = remember(periodEntries) {
                        val set = mutableSetOf<String>()
                        periodEntries.forEach { pe ->
                            val s = runCatching { sdf.parse(pe.startDate) }.getOrNull() ?: return@forEach
                            val e = if (pe.endDate.isNotBlank()) runCatching { sdf.parse(pe.endDate) }.getOrNull() else null
                            val endMs = e?.time ?: (s.time + 4 * 86400000L)
                            var cur = s.time
                            while (cur <= endMs) { set.add(sdf.format(Date(cur))); cur += 86400000L }
                        }
                        set
                    }

                    val rows = (firstDow + daysInMonth + 6) / 7
                    for (row in 0 until rows) {
                        Row(Modifier.fillMaxWidth().height(36.dp)) {
                            for (col in 0..6) {
                                val day = row * 7 + col - firstDow + 1
                                if (day in 1..daysInMonth) {
                                    val cellCal = Calendar.getInstance().apply { set(displayYear, displayMonth, day, 0, 0, 0) }
                                    val cellDate = cellCal.time; val cellStr = sdf.format(cellDate)
                                    val isToday = cellStr == sdf.format(today)
                                    val isLoggedPeriod = cellStr in loggedPeriodDates
                                    val isPeriod = isInRange(cellDate, lastPeriodDate, periodEnd)
                                    val isNextPeriod = nextPeriodDate?.let { isInRange(cellDate, it, Date(it.time + (periodDuration - 1) * 86400000L)) } ?: false
                                    val isFertile = isInRange(cellDate, fertileStart, fertileEnd)
                                    val isOvulation = ovulationDate?.let { sdf.format(it) == cellStr } ?: false
                                    val isHighPain = cellStr in highPainDates
                                    val bg = when {
                                        isOvulation -> SuccessGreen.copy(alpha = 0.3f)
                                        isLoggedPeriod -> ErrorRed.copy(alpha = 0.25f)
                                        isPeriod || isNextPeriod -> ErrorRed.copy(alpha = 0.12f)
                                        isFertile -> InfoBlue.copy(alpha = 0.15f)
                                        isHighPain -> WarningAmber.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                    val brd = if (isToday) Modifier.border(2.dp, Primary, CircleShape) else Modifier
                                    Box(Modifier.weight(1f).fillMaxHeight().then(brd).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                                        Text("$day", style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when { isOvulation -> SuccessGreen; isLoggedPeriod || isPeriod || isNextPeriod -> ErrorRed; isToday -> Primary; else -> MaterialTheme.colorScheme.onSurface })
                                    }
                                } else Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendDot(ErrorRed, "Period"); LegendDot(InfoBlue, "Fertile"); LegendDot(SuccessGreen, "Ovulation"); LegendDot(WarningAmber, "Pain")
                    }
                }
            }

            // AI Insights
            item {
                TintedCard(tint = CardTintAmber) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠 AI Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(onClick = {
                            if (groqApiKey.isBlank() || languageService == null) {
                                aiInsight = "Offline: Cycle length $effectiveCycle days. ${if (effectiveCycle < 24 || effectiveCycle > 35) "⚠ Irregular cycle. Consult a doctor." else "Normal range."} ${if (painEntries.any { it.painLevel >= 7 }) "Frequent severe pain reported." else ""}"
                                return@FilledTonalButton
                            }
                            aiLoading = true
                            scope.launch {
                                val prompt = "Analyze: Cycle $effectiveCycle days, period $periodDuration days, ${periodEntries.size} logged cycles. ${if (painEntries.isNotEmpty()) "Avg pain: ${"%.1f".format(painEntries.map { it.painLevel }.average())}/10." else ""} Give 3 brief insights. Never diagnose."
                                aiInsight = withContext(Dispatchers.IO) {
                                    runCatching { languageService!!.groqChat(groqApiKey, "You are a women's health advisor. Never diagnose.", prompt) }.getOrElse { "Error: ${it.message}" }
                                }
                                aiLoading = false
                            }
                        }, shape = RoundedCornerShape(10.dp), enabled = !aiLoading) {
                            if (aiLoading) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.AutoAwesome, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp)); Text("Generate", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (aiInsight.isNotBlank()) {
                        Spacer(Modifier.height(8.dp)); Text(aiInsight, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(onClick = { onSpeak(aiInsight) }, shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Filled.VolumeUp, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Listen", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(onClick = { onShareReport("SwasthyaSaathi Cycle Insights\n\n$aiInsight") }, shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = InfoBlue.copy(alpha = 0.12f), contentColor = InfoBlue)) {
                                Icon(Icons.Filled.Share, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Share", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (activeTab == 0 && lastPeriodDate == null) {
            item { Text("Set your last period date above to see predictions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
        }

        // ═══ HISTORY TAB ═══
        if (activeTab == 1) {
            if (periodEntries.isEmpty()) {
                item { Text("No periods logged yet. Tap 'Log Period' to start tracking!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
            }
            if (periodEntries.isNotEmpty()) {
                item {
                    TintedCard(tint = CardTintBlue) {
                        Text("📊 Cycle Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        val withLen = periodEntries.filter { it.cycleLength > 0 }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("${periodEntries.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Primary)
                                Text("Logged", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(if (withLen.isNotEmpty()) "$avgCycleLength" else "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Secondary)
                                Text("Avg Cycle", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                val shortest = withLen.minByOrNull { it.cycleLength }?.cycleLength
                                val longest = withLen.maxByOrNull { it.cycleLength }?.cycleLength
                                Text(if (shortest != null && longest != null) "$shortest-$longest" else "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Tertiary)
                                Text("Range", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            items(periodEntries, key = { it.id }) { entry ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.06f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(ErrorRed.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Circle, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${entry.startDate}${if (entry.endDate.isNotBlank()) " → ${entry.endDate}" else ""}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(buildString {
                                if (entry.cycleLength > 0) append("Cycle: ${entry.cycleLength} days")
                                if (entry.notes.isNotBlank()) { if (isNotEmpty()) append(" · "); append(entry.notes) }
                                if (isEmpty()) append("Period logged")
                            }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { localStore.deletePeriodEntry(entry.id); periodEntries = localStore.getAllPeriodEntries() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), modifier = modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = 0.6f)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
