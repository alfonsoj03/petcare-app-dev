package com.example.mascotasapp.ui.screens.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.mascotasapp.data.repository.RoutinesRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.mascotasapp.ui.screens.routine.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    onBack: (() -> Unit)? = null,
    onAddCustom: () -> Unit = {},
    onAddMedication: () -> Unit = {},
    onMarkDone: (String) -> Unit = {},
    onEditItem: (String) -> Unit = {},
    onEditMedication: (String) -> Unit = {}
) {
    val routineViewModel: RoutineViewModel = viewModel()
    val uiState = routineViewModel.uiState
    // Palette
    val bgSurface = Color(0xFFF9FAFB)
    val muted = Color(0xFF6B7280)
    val green = Color(0xFF10B981)
    val greenSurface = Color(0xFFE6F9EE)
    val orange = Color(0xFFF59E0B)
    val orangeSurface = Color(0xFFFFF3E0)
    val brandPurple = Color(0xFF8B5CF6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Routine",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgSurface,
                    scrolledContainerColor = bgSurface
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        containerColor = bgSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val routines by RoutinesRepository.routines.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Routines", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    AssistChip(
                        onClick = onAddCustom,
                        label = { Text("+ Add Custom") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = brandPurple, labelColor = Color.White)
                    )
                }
            }
            if (routines.isEmpty()) {
                item {
                    Text("No routines yet.", style = MaterialTheme.typography.bodyMedium, color = muted)
                }
            } else {
                items(routines, key = { it.assignment_id }) { r ->
                    val statusFg = when {
                        r.next_activity.isBlank() -> muted
                        else -> green
                    }
                    val statusBg = when {
                        r.next_activity.isBlank() -> Color(0xFFF3F4F6)
                        else -> greenSurface
                    }
                    val lastPretty = formatDateTimePretty(r.last_performed_at)
                    val nextPretty = formatDateTimePretty(r.next_activity)
                    RoutineCard(
                        title = r.routine_name.ifBlank { "Routine" },
                        statusText = if (r.next_activity.isBlank()) "--" else "Scheduled",
                        statusBg = statusBg,
                        statusFg = statusFg,
                        lastDate = lastPretty,
                        nextDate = nextPretty,
                        onMarkDone = { onMarkDone(r.routine_id) },
                        onEdit = { onEditItem(r.routine_id) }
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Current Medications", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    AssistChip(
                        onClick = onAddMedication,
                        label = { Text("+ Add") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = brandPurple, labelColor = Color.White)
                    )
                }
            }
            item {
                MedicationCard(
                    name = "Heartgard Plus",
                    dose = "68mg - Monthly",
                    reminder = "Reminder On",
                    start = "Oct 1, 2024",
                    end = "Oct 1, 2025",
                    nextDose = "Dec 1, 2024",
                    onMarkDone = {
                        routineViewModel.onMarkDone("heartgard_plus")
                        onMarkDone("heartgard_plus")
                    },
                    onEdit = { onEditMedication("heartgard_plus") }
                )
            }
            item {
                MedicationCard(
                    name = "Apoquel",
                    dose = "16mg - Twice daily",
                    reminder = "Reminder Off",
                    start = "Nov 15, 2024",
                    end = "Dec 15, 2024",
                    nextDose = "Today, 6:00 PM",
                    onMarkDone = {
                        routineViewModel.onMarkDone("apoquel")
                        onMarkDone("apoquel")
                    },
                    onEdit = { onEditMedication("apoquel") }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (uiState.overdueDialog != null) {
            AlertDialog(
                onDismissRequest = { routineViewModel.dismissOverdueDialog() },
                title = { Text("Te pasaste de la hora") },
                text = { Text("¿Cómo calculamos la próxima ejecución?") },
                confirmButton = {
                    TextButton(onClick = { routineViewModel.chooseOverdueRescheduleFromNow() }) {
                        Text("Desde ahora")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineViewModel.chooseOverdueRescheduleFromOriginal() }) {
                        Text("Desde la hora original")
                    }
                }
            )
        }
    }
}

@Composable
private fun RoutineCard(
    title: String,
    statusText: String,
    statusBg: Color,
    statusFg: Color,
    lastDate: String,
    nextDate: String,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    highlightNextColor: Color? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFEFF1F5))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusFg)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Last Date", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
                    Text(lastDate, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Color(0xFF111827))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Next Date", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
                    Text(nextDate, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = highlightNextColor ?: Color(0xFF111827))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onMarkDone,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33C59D), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Mark as done")
                }
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color.Black
                    )
                ) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun MedicationCard(
    name: String,
    dose: String,
    reminder: String,
    start: String,
    end: String,
    nextDose: String,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Medication,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(dose, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
                // Removed reminder pill
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start Date", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
                    Text(start, style = MaterialTheme.typography.bodyMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("End Date", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
                    Text(end, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onMarkDone,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33C59D), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) { Text("Mark as done") }
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color.Black
                    )
                ) { Text("Edit") }
            }
            Text("Next dose: $nextDose", style = MaterialTheme.typography.bodySmall, color = Color.Black)
        }
    }
}

private fun formatDateTimePretty(raw: String?): String {
    val s = raw?.trim().orEmpty()
    if (s.isBlank()) return "--"
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.getDefault())
    runCatching { Instant.parse(s).atZone(zone).format(fmt) }.onSuccess { return it }
    val normalized = if (s.matches(Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"))) s.replace(' ', 'T') else s
    runCatching { LocalDateTime.parse(normalized).atZone(zone).format(fmt) }.onSuccess { return it }
    runCatching { LocalDate.parse(s).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())) }.onSuccess { return it }
    return s
}
