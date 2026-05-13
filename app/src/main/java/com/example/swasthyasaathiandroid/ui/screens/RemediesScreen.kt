package com.example.swasthyasaathiandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*
import java.util.Locale

@Composable
fun RemediesScreen(
    ui: UiText,
    remedies: List<RemedyItem>,
    remedyQuery: String,
    onQueryChange: (String) -> Unit,
    remedyCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedRemedy: RemedyItem?,
    onSelectRemedy: (RemedyItem?) -> Unit,
    onAskDidi: (String) -> Unit
) {
    val categories = remember(remedies) { listOf("all") + remedies.map { it.category }.distinct().sorted() }
    val filtered = remember(remedies, remedyQuery, remedyCategory) {
        remedies.filter { remedy ->
            val categoryOk = remedyCategory == "all" || remedy.category == remedyCategory
            val query = remedyQuery.trim().lowercase(Locale.ROOT)
            val queryOk = query.isBlank() ||
                remedy.nameEnglish.lowercase(Locale.ROOT).contains(query) ||
                remedy.nameHindi.lowercase(Locale.ROOT).contains(query) ||
                remedy.category.lowercase(Locale.ROOT).contains(query) ||
                remedy.ingredients.lowercase(Locale.ROOT).contains(query)
            categoryOk && queryOk
        }
    }
    var catMenu by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GradientHeaderCard(
                title = "Desi Nuskhe",
                subtitle = "126 Ayurvedic Home Remedies",
                icon = Icons.Filled.LocalFlorist
            )
        }

        // ── Search & Filter ──
        item {
            TintedCard(tint = CardTintGreen) {
                OutlinedTextField(
                    value = remedyQuery,
                    onValueChange = onQueryChange,
                    label = { Text("Search remedies...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        OutlinedButton(
                            onClick = { catMenu = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FilterList, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (remedyCategory == "all") "All Categories" else remedyCategory)
                        }
                        DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(if (c == "all") "All Categories" else c) },
                                    onClick = { onCategoryChange(c); catMenu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    InfoChip("${filtered.size} found", Primary)
                }
                Text("All remedies stored locally • Offline access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // ── Selected Remedy Detail ──
        if (selectedRemedy != null) item {
            TintedCard(tint = CardTintAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedRemedy.nameEnglish, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onSelectRemedy(null) }) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
                if (selectedRemedy.nameHindi.isNotBlank()) {
                    Text(selectedRemedy.nameHindi, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                RemedyDetailRow(Icons.Filled.Science, "Ingredients", selectedRemedy.ingredients)
                RemedyDetailRow(Icons.Filled.MenuBook, "Preparation", selectedRemedy.preparation)
                RemedyDetailRow(Icons.Filled.LocalDrink, "Dosage", selectedRemedy.dosage)

                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedRemedy.warnings, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoChip("Pregnancy: ${selectedRemedy.pregnancySafe}", if (selectedRemedy.pregnancySafe.lowercase().contains("yes")) SuccessGreen else WarningAmber)
                    Spacer(Modifier.width(8.dp))
                    InfoChip(selectedRemedy.evidenceLevel, InfoBlue)
                }

                Spacer(Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = { onAskDidi("Suggest remedies for ${selectedRemedy.nameEnglish}") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ChatBubble, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ask Didi")
                }
            }
        }

        // ── Remedy List ──
        items(filtered.take(120)) { remedy ->
            Card(
                onClick = { onSelectRemedy(remedy) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalFlorist, null, tint = Secondary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(remedy.nameEnglish, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (remedy.nameHindi.isNotBlank()) {
                            Text(remedy.nameHindi, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            InfoChip(remedy.category, Primary)
                            InfoChip(remedy.evidenceLevel, InfoBlue)
                        }
                    }
                    ArrowIcon()
                }
            }
        }
    }
}

@Composable
private fun RemedyDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
