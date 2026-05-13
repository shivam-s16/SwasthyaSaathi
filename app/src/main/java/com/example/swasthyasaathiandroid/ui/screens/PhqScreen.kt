package com.example.swasthyasaathiandroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swasthyasaathiandroid.ui.theme.*

private data class PhqQuestion(
    val number: Int,
    val text: String
)

private val phqQuestions = listOf(
    PhqQuestion(1, "Little interest or pleasure in doing things"),
    PhqQuestion(2, "Feeling down, depressed or hopeless"),
    PhqQuestion(3, "Trouble falling asleep, staying asleep, or sleeping too much"),
    PhqQuestion(4, "Feeling tired or having little energy"),
    PhqQuestion(5, "Poor appetite or overeating"),
    PhqQuestion(6, "Feeling bad about yourself — or that you're a failure or have let yourself or your family down"),
    PhqQuestion(7, "Trouble concentrating on things, such as reading the newspaper or watching television"),
    PhqQuestion(8, "Moving or speaking so slowly that other people could have noticed. Or, the opposite — being so fidgety or restless that you have been moving around a lot more than usual"),
    PhqQuestion(9, "Thoughts that you would be better off dead or of hurting yourself in some way")
)

private val frequencyLabels = listOf("Not at all", "Several days", "More than half\nthe days", "Nearly\nevery day")

@Composable
fun PhqScreen(
    onComplete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val answers = rememberSaveable { mutableStateListOf(*Array(9) { -1 }) }
    var showResult by rememberSaveable { mutableStateOf(false) }
    val totalScore = answers.filter { it >= 0 }.sum()
    val allAnswered = answers.all { it >= 0 }
    val q9Flagged = answers.getOrNull(8)?.let { it > 0 } ?: false

    if (showResult && allAnswered) {
        // ── Results View ──
        PhqResultView(
            score = totalScore,
            q9Flagged = q9Flagged,
            onRetake = {
                answers.indices.forEach { answers[it] = -1 }
                showResult = false
            },
            onDone = { onComplete(totalScore) },
            onBack = onBack
        )
    } else {
        // ── Questionnaire View ──
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item(key = "header") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = InfoBlue.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(InfoBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Psychology, null, tint = InfoBlue, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Patient Health Questionnaire",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "PHQ-9 Depression Screening",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InfoBlue
                                )
                            }
                        }
                        Text(
                            "Over the last 2 weeks, how often have you been bothered by the following problems?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Progress
                        val answered = answers.count { it >= 0 }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { answered / 9f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = InfoBlue,
                                trackColor = InfoBlue.copy(alpha = 0.12f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "$answered/9",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = InfoBlue
                            )
                        }
                    }
                }
            }

            // Frequency header row
            item(key = "freq_header") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Spacer(Modifier.weight(1f))
                    frequencyLabels.forEachIndexed { i, label ->
                        Box(
                            Modifier.width(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 13.sp,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Questions
            itemsIndexed(phqQuestions, key = { i, _ -> "q_$i" }) { index, question ->
                val isQ9 = index == 8
                val currentAnswer = answers[index]

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isQ9 && currentAnswer > 0 -> ErrorRed.copy(alpha = 0.06f)
                            currentAnswer >= 0 -> SuccessGreen.copy(alpha = 0.04f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(if (currentAnswer >= 0) 0.dp else 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isQ9) Modifier.border(
                                1.dp,
                                if (currentAnswer > 0) ErrorRed.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            ) else Modifier
                        )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Question text
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isQ9 -> ErrorRed.copy(alpha = 0.12f)
                                            currentAnswer >= 0 -> SuccessGreen.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${question.number}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isQ9 -> ErrorRed
                                        currentAnswer >= 0 -> SuccessGreen
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    question.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isQ9) {
                                    Text(
                                        "⚠ Suicide risk screening question",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Answer options
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            (0..3).forEach { value ->
                                val isSelected = currentAnswer == value
                                Box(
                                    Modifier
                                        .width(56.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                when {
                                                    isQ9 && value > 0 -> ErrorRed.copy(alpha = 0.15f)
                                                    value == 0 -> SuccessGreen.copy(alpha = 0.12f)
                                                    value == 1 -> Tertiary.copy(alpha = 0.12f)
                                                    value == 2 -> WarningAmber.copy(alpha = 0.12f)
                                                    else -> ErrorRed.copy(alpha = 0.12f)
                                                }
                                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable { answers[index] = value },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            if (value == 0) "0" else "+$value",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                when {
                                                    isQ9 && value > 0 -> ErrorRed
                                                    value == 0 -> SuccessGreen
                                                    value <= 1 -> Tertiary
                                                    value == 2 -> WarningAmber
                                                    else -> ErrorRed
                                                }
                                            } else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isSelected) {
                                            Box(
                                                Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Running total + Submit
            item(key = "submit") {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Running score
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Running Total:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                "$totalScore / 27",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    totalScore >= 20 -> ErrorRed
                                    totalScore >= 15 -> SOSRed
                                    totalScore >= 10 -> WarningAmber
                                    totalScore >= 5 -> Tertiary
                                    else -> SuccessGreen
                                }
                            )
                        }
                    }

                    if (!allAnswered) {
                        Text(
                            "Please answer all ${9 - answers.count { it >= 0 }} remaining questions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showResult = true },
                        enabled = allAnswered,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfoBlue
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Filled.Assessment, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View My Results", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhqResultView(
    score: Int,
    q9Flagged: Boolean,
    onRetake: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val severity: String
    val proposedAction: String
    val severityColor: Color

    when {
        score >= 20 -> {
            severity = "Severe Depression"
            proposedAction = "Immediate initiation of pharmacotherapy and, if severe impairment or poor response to therapy, expedited referral to a mental health specialist for psychotherapy and/or collaborative management."
            severityColor = ErrorRed
        }
        score >= 15 -> {
            severity = "Moderately Severe Depression"
            proposedAction = "Active treatment with pharmacotherapy and/or psychotherapy."
            severityColor = SOSRed
        }
        score >= 10 -> {
            severity = "Moderate Depression"
            proposedAction = "Treatment plan, considering counseling, follow-up and/or pharmacotherapy."
            severityColor = WarningAmber
        }
        score >= 5 -> {
            severity = "Mild Depression"
            proposedAction = "Watchful waiting; repeat PHQ-9 at follow-up."
            severityColor = Tertiary
        }
        else -> {
            severity = "None-Minimal Depression"
            proposedAction = "No treatment needed. Maintain self-care habits and check in weekly."
            severityColor = SuccessGreen
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Score card
        item(key = "score_card") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Your PHQ-9 Result", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(severityColor.copy(alpha = 0.12f))
                            .border(3.dp, severityColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$score",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = severityColor
                            )
                            Text("/27", style = MaterialTheme.typography.labelMedium, color = severityColor.copy(alpha = 0.7f))
                        }
                    }
                    Text(
                        severity,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }
        }

        // Proposed action
        item(key = "action") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MedicalServices, null, tint = severityColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Proposed Treatment Action", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Text(proposedAction, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Q9 Warning
        if (q9Flagged) {
            item(key = "q9_warning") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Suicide Risk Identified",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                        Text(
                            "You answered positively on Question 9 (self-harm thoughts). This requires further assessment by an individual who is competent to assess suicide risk. Please reach out to a mental health professional or helpline immediately.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed.copy(alpha = 0.9f)
                        )
                        Text(
                            "📞 iCall: 9152987821\n📞 Vandrevala Foundation: 1860-2662-345\n📞 Women Helpline: 181",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRed
                        )
                    }
                }
            }
        }

        // Interpretation table
        item(key = "interpretation") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Interpretation Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    InterpretationRow("0 – 4", "None-minimal", SuccessGreen, score in 0..4)
                    InterpretationRow("5 – 9", "Mild", Tertiary, score in 5..9)
                    InterpretationRow("10 – 14", "Moderate", WarningAmber, score in 10..14)
                    InterpretationRow("15 – 19", "Moderately Severe", SOSRed, score in 15..19)
                    InterpretationRow("20 – 27", "Severe", ErrorRed, score in 20..27)
                }
            }
        }

        // Actions
        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save & Return to Care", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retake Assessment")
                }
            }
        }

        // Disclaimer
        item(key = "disclaimer") {
            Text(
                "Note: The PHQ-9 is a screening tool, not a diagnostic instrument. Results should be discussed with a qualified healthcare provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InterpretationRow(range: String, label: String, color: Color, isActive: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) color.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            range,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(52.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isActive) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.CheckCircle, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}
