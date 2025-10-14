package com.example.mascotasapp.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onBack: (() -> Unit)? = null,
    onViewAll: () -> Unit = {},
    onReschedule: () -> Unit = {},
    onViewDetails: (String) -> Unit = {},
    onAddExtraVisit: () -> Unit = {}
) {
    // Local palette to mirror the reference UI
    val bgSurface = Color(0xFFF9FAFB)
    val cardStroke = Color(0xFFE5E7EB)
    val muted = Color(0xFF6B7280)
    val brandPurple = Color(0xFF8B5CF6)
    val blue = Color(0xFF3B82F6)
    val green = Color(0xFF10B981)
    val greenSurface = Color(0xFFE6F9EE)
    val red = Color(0xFFEF4444)
    val redSurface = Color(0xFFFDE8E8)
    val lightGray = Color(0xFFF3F4F6)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Health & Vaccination", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(green),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExtraVisit,
                containerColor = brandPurple
            ) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) }
        },
        containerColor = bgSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Vet Visits header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Vet Visits", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = brandPurple,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }

            // Next Vet Visit card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(BorderStroke(1.dp, cardStroke), RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(blue),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text("Next Vet Visit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Annual", style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFD1D5DB)))
                        Spacer(Modifier.width(10.dp))
                        Text("Dec 15, 2024 at 2:30 PM", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFD1D5DB)))
                        Spacer(Modifier.width(10.dp))
                        Text("Happy Paws Clinic", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                    }
                    Button(
                        onClick = onReschedule,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = blue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Reschedule") }
                }
            }

            // Recent Visits header
            Text("Recent Visits", style = MaterialTheme.typography.titleMedium)

            // Completed visit card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(BorderStroke(1.dp, cardStroke), RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(greenSurface),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = green) }
                        Text("Routine Checkup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = {},
                            label = { Text("Completed") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = greenSurface,
                                labelColor = green
                            )
                        )
                    }
                    Text("Nov 10, 2024", style = MaterialTheme.typography.bodySmall, color = muted)
                    Text("Dr. Sarah Johnson – Happy Paws Clinic", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "View Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = green,
                        modifier = Modifier.clickable { onViewDetails("routine_checkup") }
                    )
                }
            }

            // Emergency visit card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(BorderStroke(1.dp, cardStroke), RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(redSurface),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Warning, contentDescription = null, tint = red) }
                        Text("Emergency Visit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = {},
                            label = { Text("Emergency") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = redSurface,
                                labelColor = red
                            )
                        )
                    }
                    Text("Oct 22, 2024", style = MaterialTheme.typography.bodySmall, color = muted)
                    Text("Dr. Mike Wilson – Emergency Vet Center", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "View Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = green,
                        modifier = Modifier.clickable { onViewDetails("emergency_visit") }
                    )
                }
            }

            // Log Extraordinary Visit button-like row
            Surface(
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(12.dp),
                color = lightGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = muted, style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.width(8.dp))
                    Text("Log Extraordinary Visit", style = MaterialTheme.typography.labelLarge, color = muted, modifier = Modifier.clickable { onAddExtraVisit() })
                }
            }
        }
    }
}
