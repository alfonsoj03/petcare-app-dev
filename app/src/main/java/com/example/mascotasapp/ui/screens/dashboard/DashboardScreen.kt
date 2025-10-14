@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mascotasapp.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow

@Composable
fun DashboardScreen(
    onOpenHealth: () -> Unit = {},
    onOpenRoutine: () -> Unit = {},
    onRegisterBath: () -> Unit = {},
    onRegisterVisit: () -> Unit = {},
    onRegisterFeeding: () -> Unit = {}
) {
    val petName = "Luna"
    val petSpecies = "Canine"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PetCare", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Filled.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { PetCard(onOpenHealth = onOpenHealth, onOpenRoutine = onOpenRoutine) }
            item { QuickLogSection(onRegisterBath, onRegisterVisit, onRegisterFeeding) }
            item { RecentActivitySection() }
        }
    }
}

@Composable
private fun PetCard(onOpenHealth: () -> Unit, onOpenRoutine: () -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://placekitten.com/200/200",
                    contentDescription = "Pet avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Buddy", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Golden Retriever • 3 years", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                }
                Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }

            InfoRowCard(
                title = "Next Vaccine",
                subtitle = "Rabies vaccination due in 5 days",
                icon = Icons.Default.Vaccines,
                onClick = onOpenHealth
            )

            InfoRowCard(
                title = "Next vet visit",
                subtitle = "2025-10-15",
                icon = Icons.Default.Event,
                onClick = onOpenHealth,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
    }
}

@Composable
private fun InfoRowCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF33C59D)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuickLogSection(
    onRegisterBath: () -> Unit,
    onRegisterVisit: () -> Unit,
    onRegisterFeeding: () -> Unit
) {
    Text(text = "Quick Log", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Vet Visit",
                icon = Icons.Default.MedicalServices,
                onClick = onRegisterVisit,
                lightBg = Color(0xFFDBEAFE),
                darkAccent = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Bath",
                icon = Icons.Default.Opacity,
                onClick = onRegisterBath,
                lightBg = Color(0xFFCFFAFE),
                darkAccent = Color(0xFF0891B2),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Dental",
                icon = Icons.Default.MedicalServices,
                onClick = onRegisterVisit,
                lightBg = Color(0xFFDCFCE7),
                darkAccent = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Feeding",
                icon = Icons.Default.Restaurant,
                onClick = onRegisterFeeding,
                lightBg = Color(0xFFFFEDD5),
                darkAccent = Color(0xFFEA580C),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lightBg: Color = MaterialTheme.colorScheme.surface,
    darkAccent: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = MaterialTheme.shapes.large,
                ambientColor = Color(0x66000000),
                spotColor = Color(0x1A000000)
            )
    ) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, Color(0xFFF3F4F6))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = lightBg,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = darkAccent,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentActivitySection() {
    val recent = listOf(
        "Bath completed — Oct 10",
        "Vaccine updated — Oct 03",
        "Feeding recorded — Oct 02",
    )
    Text(text = "Recent Activity", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recent.forEach { item ->
            ActivityItem(title = "Vaccine Recorded", subtitle = item, time = "2 hours ago")
        }
    }
}

@Composable
private fun ActivityItem(title: String, subtitle: String, time: String, modifier: Modifier = Modifier,) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = MaterialTheme.shapes.large,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x1A000000)
            )
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Vaccines,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
