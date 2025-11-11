package com.petcare.mascotasapp.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailsScreen(
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    val bgSurface = Color(0xFFF9FAFB)
    val green = Color(0xFF10B981)
    val textPrimary = Color(0xFF111827)
    val textSecondary = Color(0xFF6B7280)

    // Static sample data matching the mock; can be replaced by real arguments later
    val title = "Routine Checkup"
    val dateTime = "Nov 10, 2024 — 10:30 AM"
    val clinic = "Happy Paws Clinic"
    val vet = "Dr. Sarah Johnson"
    val pet = "Max (Golden Retriever)"
    val notes = """
        Routine annual checkup completed successfully. All vaccines have been updated including rabies and DHPP.
        Weight is within normal range at 65 lbs.
        Dental health looks good with minimal
    """.trimIndent()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(green),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White, scrolledContainerColor = Color.White),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        containerColor = bgSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Divider(color = Color(0xFFE5E7EB))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Edit Visit") }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card-like section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = textPrimary)
                // Rows with icons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Today, contentDescription = null, tint = textSecondary)
                    Text(dateTime, style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = textSecondary)
                    Text(clinic, style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = textSecondary)
                    Text(vet, style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Pets, contentDescription = null, tint = textSecondary)
                    Text(pet, style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                }

                // Notes
                Text("Visit Notes", style = MaterialTheme.typography.titleMedium, color = textPrimary)
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
