package com.example.swasthyasaathiandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun EducationScreen(
    useHindi: Boolean = false,
    onSpeak: (String) -> Unit = {},
    onShareArticle: (String) -> Unit = {}
) {
    val articles = remember { getEducationArticles() }
    val categories = listOf(
        "breastfeeding" to ("🤱 Breastfeeding" to Icons.Filled.ChildCare),
        "newborn" to ("👶 Newborn Care" to Icons.Filled.Favorite),
        "hygiene" to ("🧼 Hygiene" to Icons.Filled.Spa),
        "pregnancy_nutrition" to ("🥗 Pregnancy Nutrition" to Icons.Filled.Restaurant),
        "danger_signs" to ("🚨 Danger Signs" to Icons.Filled.Warning),
        "postpartum" to ("💜 Postpartum Care" to Icons.Filled.Healing)
    )
    var selectedCategory by remember { mutableStateOf("breastfeeding") }
    var expandedArticleId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = articles.filter { it.category == selectedCategory }.filter {
        if (searchQuery.isBlank()) true
        else {
            val q = searchQuery.lowercase()
            it.title.lowercase().contains(q) || it.titleHi.contains(q) ||
                    it.summary.lowercase().contains(q) || it.summaryHi.contains(q)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GradientHeaderCard(
                title = "Health Education",
                subtitle = "Offline medical library • 10 articles • Hindi + English",
                icon = Icons.Filled.MenuBook
            )
        }

        // Search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (useHindi) "खोजें..." else "Search articles...") },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Category chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Show categories in a scrollable row
                categories.take(3).forEach { (key, labelPair) ->
                    val (label, icon) = labelPair
                    val isSelected = selectedCategory == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = key; expandedArticleId = null },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.15f),
                            selectedLabelColor = Primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.drop(3).forEach { (key, labelPair) ->
                    val (label, icon) = labelPair
                    val isSelected = selectedCategory == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = key; expandedArticleId = null },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
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

        // Articles
        if (filtered.isEmpty()) {
            item {
                Text("No articles found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }

        items(filtered, key = { it.id }) { article ->
            val isExpanded = expandedArticleId == article.id
            val title = if (useHindi) article.titleHi else article.title
            val summary = if (useHindi) article.summaryHi else article.summary
            val content = if (useHindi) article.contentHi else article.content
            val catColor = when (article.category) {
                "breastfeeding" -> Secondary
                "newborn" -> Tertiary
                "hygiene" -> SuccessGreen
                "pregnancy_nutrition" -> Primary
                "danger_signs" -> ErrorRed
                "postpartum" -> InfoBlue
                else -> Primary
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = catColor)
                            Spacer(Modifier.height(2.dp))
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Listen button
                        IconButton(onClick = { onSpeak(if (isExpanded) content else summary) }) {
                            Icon(Icons.Filled.VolumeUp, "Listen", tint = catColor, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Expand/Collapse
                    TextButton(
                        onClick = { expandedArticleId = if (isExpanded) null else article.id },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null, Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isExpanded) "Show Less" else "Read More")
                    }

                    // Expanded content
                    if (isExpanded) {
                        HorizontalDivider(color = catColor.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(content, style = MaterialTheme.typography.bodyMedium)

                        // Danger Signs
                        if (article.dangerSigns.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("🚨 Danger Signs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                                    article.dangerSigns.forEach { sign ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("⚠", modifier = Modifier.padding(end = 6.dp))
                                            Text(sign, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }

                        // Tips
                        if (article.tips.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("💡 Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    article.tips.forEach { tip ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("•", color = SuccessGreen, modifier = Modifier.padding(end = 6.dp))
                                            Text(tip, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Share button
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val text = "$title. $content. ${if (article.dangerSigns.isNotEmpty()) "Danger Signs: ${article.dangerSigns.joinToString(", ")}." else ""}"
                                    onSpeak(text)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.VolumeUp, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Listen Full", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine("📚 $title")
                                        appendLine()
                                        appendLine(content)
                                        if (article.dangerSigns.isNotEmpty()) {
                                            appendLine()
                                            appendLine("🚨 Danger Signs:")
                                            article.dangerSigns.forEach { appendLine("  ⚠ $it") }
                                        }
                                        if (article.tips.isNotEmpty()) {
                                            appendLine()
                                            appendLine("💡 Tips:")
                                            article.tips.forEach { appendLine("  • $it") }
                                        }
                                        appendLine()
                                        appendLine("— SwasthyaSaathi Health Education")
                                    }
                                    onShareArticle(shareText)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = InfoBlue.copy(alpha = 0.15f), contentColor = InfoBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
