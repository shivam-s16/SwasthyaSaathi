package com.example.swasthyasaathiandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFlorist

import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home(
        route = "home",
        title = "Home",
        subtitle = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    Chat(
        route = "chat",
        title = "Ask Didi",
        subtitle = "AI Health Chat",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubble
    ),
    ImageScan(
        route = "image_scan",
        title = "AI Diagnostics",
        subtitle = "Zero-Cost Health Screening",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt
    ),
    Remedies(
        route = "remedies",
        title = "Desi Nuskhe",
        subtitle = "126 Ayurvedic Remedies",
        selectedIcon = Icons.Filled.LocalFlorist,
        unselectedIcon = Icons.Outlined.LocalFlorist
    ),
    Care(
        route = "care",
        title = "Care",
        subtitle = "Pregnancy · Mental Health",
        selectedIcon = Icons.Filled.FavoriteBorder,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    SettingsScreen(
        route = "settings",
        title = "Settings",
        subtitle = "API Key · Language",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    ),

    Phq9(
        route = "phq9",
        title = "PHQ-9 Test",
        subtitle = "Depression Screening",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    ),
    Education(
        route = "education",
        title = "Health Education",
        subtitle = "Offline Medical Library",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    PainJournal(
        route = "pain_journal",
        title = "Pain Journal",
        subtitle = "Symptom & Mood Tracking",
        selectedIcon = Icons.Filled.Create,
        unselectedIcon = Icons.Outlined.Create
    ),
    CycleTracker(
        route = "cycle_tracker",
        title = "Cycle Tracker",
        subtitle = "Period & Fertility Calendar",
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange
    );

    companion object {
        val drawerItems = listOf(Home, Chat, ImageScan, Remedies, Care, Phq9, PainJournal, CycleTracker, Education, SettingsScreen)
    }
}
