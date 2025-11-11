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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.mascotasapp.data.repository.RoutinesRepository
import com.example.mascotasapp.data.repository.MedicationsRepository
import com.example.mascotasapp.core.SelectedPetStore
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.launch
import com.example.mascotasapp.core.ApiConfig
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log

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
    val selectedPetId by SelectedPetStore.selectedPetId.collectAsState(initial = null)
    val routineViewModel: RoutineViewModel = viewModel()
    val medicationViewModel: MedicationViewModel = viewModel()
    val uiState = routineViewModel.uiState
    // Palette
    val bgSurface = Color(0xFFF9FAFB)
    val muted = Color(0xFF6B7280)
    val green = Color(0xFF10B981)
    val greenSurface = Color(0xFFE6F9EE)
    val orange = Color(0xFFF59E0B)
    val orangeSurface = Color(0xFFFFF3E0)
    val brandPurple = Color(0xFF8B5CF6)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Confirm-delete dialog state: Pair<assignment_id, routine_id>
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var pendingMedDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isDeletingMed by remember { mutableStateOf(false) }
    var markingMedicationId by remember { mutableStateOf<String?>(null) }
    var completedMedDialogFor by remember { mutableStateOf<MedicationsRepository.MedicationItem?>(null) }
    var completedMedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var markingRoutineId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        val routinesFlow = if (selectedPetId.isNullOrBlank()) {
            MutableStateFlow(emptyList<RoutinesRepository.RoutineItem>())
        } else {
            RoutinesRepository.routinesFlow(selectedPetId!!)
        }
        val routines by routinesFlow.collectAsState(initial = emptyList())

        val medsFlow = if (selectedPetId.isNullOrBlank()) {
            MutableStateFlow(emptyList<MedicationsRepository.MedicationItem>())
        } else {
            MedicationsRepository.medsFlow(selectedPetId!!)
        }
        val medications by medsFlow.collectAsState(initial = emptyList())

        LaunchedEffect(selectedPetId) {
            val pid = selectedPetId
            if (!pid.isNullOrBlank()) {
                runCatching { MedicationsRepository.refresh(ApiConfig.BASE_URL, pid) }
            }
        }

        // Precompute sorted medications in a @Composable context
        val sortedMeds = remember(medications, completedMedIds) {
            medications.sortedBy { m ->
                val endStr = m.end_of_supply.trim()
                val endEpoch: Long? = try {
                    when {
                        endStr.isEmpty() -> null
                        endStr.matches(Regex("^\\d+(\\.\\d+)?$")) -> {
                            val num = endStr.toDouble()
                            if (num > 1_000_000_000_000.0) num.toLong() else (num.toLong() * 1000)
                        }
                        else -> {
                            val dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            java.time.LocalDateTime.parse(endStr, dtf).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                    }
                } catch (_: Throwable) { null }
                val startEpoch: Long? = try {
                    val s = m.start_of_medication.trim()
                    when {
                        s.matches(Regex("^\\d+(\\.\\d+)?$")) -> {
                            val num = s.toDouble()
                            if (num > 1_000_000_000_000.0) num.toLong() else (num.toLong() * 1000)
                        }
                        else -> {
                            val dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            java.time.LocalDateTime.parse(s, dtf).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                    }
                } catch (_: Throwable) { null }
                val isLast = (startEpoch != null && endEpoch != null && startEpoch >= endEpoch)
                val completed = m.is_completed || completedMedIds.contains(m.assignment_id) || isLast
                completed
            }
        }

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
                        label = { Text("+ Add") },
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
                        loading = (markingRoutineId == r.routine_id),
                        onMarkDone = {
                            val pid = selectedPetId
                            if (!pid.isNullOrBlank() && markingRoutineId == null) {
                                markingRoutineId = r.routine_id
                                scope.launch {
                                    try {
                                        RoutinesRepository.performRoutine(
                                            baseUrl = ApiConfig.BASE_URL,
                                            routineId = r.routine_id,
                                            petId = pid
                                        )
                                        snackbarHostState.showSnackbar("Marked as done")
                                        runCatching { RoutinesRepository.refresh(ApiConfig.BASE_URL, pid) }
                                    } catch (t: Throwable) {
                                        snackbarHostState.showSnackbar(t.message ?: "Error")
                                    } finally {
                                        markingRoutineId = null
                                    }
                                }
                            }
                        },
                        onDelete = { pendingDelete = r.assignment_id to r.routine_id }
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
            if (sortedMeds.isNotEmpty()) {
                items(sortedMeds, key = { it.assignment_id }) { m ->
                    Log.d("MedUI", "item assignment=${m.assignment_id} start_raw=${m.start_of_medication} next_raw=${m.next_dose} take_every=${m.take_every_number} ${m.take_every_unit} total=${m.total_doses}")
                    val startPretty = formatDateTimePretty(m.start_of_medication)
                    val nextPretty = formatDateTimePretty(m.next_dose)
                    Log.d("MedUI", "item assignment=${m.assignment_id} start_fmt=$startPretty next_fmt=$nextPretty")
                    val freq = when {
                        m.take_every_number.isBlank() || m.take_every_unit.isBlank() -> ""
                        else -> " - Every ${m.take_every_number} ${m.take_every_unit}"
                    }
                    val doseText = (m.dose.ifBlank { "" }) + freq
                    // Use backend-provided end_of_supply; never recompute so it doesn't move in frontend
                    val (endPretty, endEpochForCmp) = run {
                        val endStr = m.end_of_supply.trim()
                        if (endStr.isEmpty()) "" to null else formatDateTimePretty(endStr) to run {
                            try {
                                when {
                                    endStr.matches(Regex("^\\d+(\\.\\d+)?$")) -> {
                                        val num = endStr.toDouble()
                                        if (num > 1_000_000_000_000.0) num.toLong() else (num.toLong() * 1000)
                                    }
                                    else -> {
                                        val dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                        java.time.LocalDateTime.parse(endStr, dtf).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    }
                                }
                            } catch (_: Throwable) { null }
                        }
                    }
                    // Compute start epoch for comparison
                    val startEpochForCmp: Long? = run {
                        try {
                            val s = m.start_of_medication.trim()
                            when {
                                s.matches(Regex("^\\d+(\\.\\d+)?$")) -> {
                                    val num = s.toDouble()
                                    if (num > 1_000_000_000_000.0) num.toLong() else (num.toLong() * 1000)
                                }
                                else -> {
                                    val dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                    java.time.LocalDateTime.parse(s, dtf).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                }
                            }
                        } catch (_: Throwable) { null }
                    }
                    val isLastDose = (startEpochForCmp != null && endEpochForCmp != null && startEpochForCmp >= endEpochForCmp)
                    val isCompleted = m.is_completed || completedMedIds.contains(m.assignment_id) || isLastDose
                    val nextDisplay = if (isCompleted) "Medication completed, no upcoming doses" else nextPretty
                    MedicationCard(
                        name = m.medication_name.ifBlank { "Medication" },
                        dose = doseText.trim(),
                        reminder = "",
                        start = startPretty,
                        end = endPretty,
                        nextDose = nextDisplay,
                        loading = (markingMedicationId == m.medication_id),
                        enabled = !isCompleted,
                        onMarkDone = {
                            val pid = selectedPetId
                            if (!pid.isNullOrBlank() && markingMedicationId == null) {
                                if (isLastDose) {
                                    completedMedIds = completedMedIds + m.assignment_id
                                    completedMedDialogFor = m
                                    return@MedicationCard
                                }
                                markingMedicationId = m.medication_id
                                scope.launch {
                                    try {
                                        MedicationsRepository.performMedication(
                                            baseUrl = ApiConfig.BASE_URL,
                                            medicationId = m.medication_id,
                                            petId = pid
                                        )
                                        snackbarHostState.showSnackbar("Marked as done")
                                        runCatching { MedicationsRepository.refresh(ApiConfig.BASE_URL, pid) }
                                    } catch (t: Throwable) {
                                        snackbarHostState.showSnackbar(t.message ?: "Error")
                                    } finally {
                                        markingMedicationId = null
                                    }
                                }
                            }
                        },
                        onDelete = { pendingMedDelete = m.assignment_id to m.medication_id }
                    )
                }
            } else {
                item {
                    Text("No medications yet.", style = MaterialTheme.typography.bodyMedium, color = muted)
                }
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

    if (pendingDelete != null) {
        val (assignmentId, routineId) = pendingDelete!!
        AlertDialog(
            onDismissRequest = { if (!isDeleting) pendingDelete = null },
            title = { Text("¿Eliminar rutina?") },
            text = { Text("Borrará esta rutina para todos los pets asociados. ¿Estás seguro?") },
            confirmButton = {
                TextButton(onClick = {
                    if (!isDeleting) {
                        isDeleting = true
                        scope.launch {
                            try {
                                routineViewModel.deleteRoutine(
                                    assignmentId = assignmentId,
                                    routineId = routineId,
                                    petId = selectedPetId ?: "",
                                    baseUrl = ApiConfig.BASE_URL
                                )
                                snackbarHostState.showSnackbar("Routine deleted successfully")
                                pendingDelete = null
                            } catch (t: Throwable) {
                                snackbarHostState.showSnackbar(
                                    message = t.message ?: "Error al eliminar rutina",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                            } finally {
                                isDeleting = false
                            }
                        }
                    }
                }, enabled = !isDeleting) {
                    if (isDeleting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminando...")
                        }
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { if (!isDeleting) pendingDelete = null }, enabled = !isDeleting) { Text("Cancelar") } }
        )
    }

    if (pendingMedDelete != null) {
        val (assignmentId, medicationId) = pendingMedDelete!!
        AlertDialog(
            onDismissRequest = { if (!isDeletingMed) pendingMedDelete = null },
            title = { Text("¿Eliminar medicamento?") },
            text = { Text("Se eliminará este medicamento y sus asignaciones. ¿Estás seguro?") },
            confirmButton = {
                TextButton(onClick = {
                    if (!isDeletingMed) {
                        isDeletingMed = true
                        scope.launch {
                            try {
                                val pid = selectedPetId ?: ""
                                if (pid.isNotBlank()) {
                                    medicationViewModel.deleteMedication(
                                        assignmentId = assignmentId,
                                        medicationId = medicationId,
                                        petId = pid,
                                        baseUrl = ApiConfig.BASE_URL
                                    )
                                    snackbarHostState.showSnackbar("Medication deleted")
                                    pendingMedDelete = null
                                }
                            } catch (t: Throwable) {
                                snackbarHostState.showSnackbar(
                                    message = t.message ?: "Error al eliminar medicamento",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                            } finally {
                                isDeletingMed = false
                            }
                        }
                    }
                }, enabled = !isDeletingMed) {
                    if (isDeletingMed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminando...")
                        }
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { if (!isDeletingMed) pendingMedDelete = null }, enabled = !isDeletingMed) { Text("Cancelar") } }
        )
    }

    if (completedMedDialogFor != null) {
        AlertDialog(
            onDismissRequest = { completedMedDialogFor = null },
            title = { Text("Medicamento completado") },
            text = { Text("No hay próximas dosis.") },
            confirmButton = {
                TextButton(onClick = { completedMedDialogFor = null }) { Text("OK") }
            }
        )
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
    onDelete: () -> Unit,
    highlightNextColor: Color? = null,
    loading: Boolean = false
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
                        .height(44.dp),
                    enabled = !loading
                ) {
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Marking...")
                        }
                    } else {
                        Text("Mark as done")
                    }
                }
                val danger = Color(0xFFEF4444)
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, danger),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = danger
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) { Text("Delete") }
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
    loading: Boolean = false,
    enabled: Boolean = true,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        .height(44.dp),
                    enabled = enabled && !loading
                ) {
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Marking...")
                        }
                    } else {
                        Text("Mark as done")
                    }
                }
                val danger = Color(0xFFEF4444)
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, danger),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = danger
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = enabled
                ) { Text("Delete") }
            }
            Text("Next dose: $nextDose", style = MaterialTheme.typography.bodySmall, color = Color.Black)
        }
    }
}

private fun formatDateTimePretty(raw: String?): String {
  val s = raw?.trim().orEmpty()
  if (s.isBlank()) return "--"
  val zone = ZoneId.systemDefault()
  val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH)
  runCatching { Instant.parse(s).atZone(zone).format(fmt) }.onSuccess { return it }
  val normalizedSpaceSlashes = s.replace(" / ", "/")
  // Try dd/MM/yyyy HH:mm:ss (e.g., 02/11/2025 20:00:00)
  runCatching { LocalDateTime.parse(normalizedSpaceSlashes, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")).atZone(zone).format(fmt) }.onSuccess { return it }
  // Try dd/MM/yyyy HH:mm (e.g., 02/11/2025 20:00)
  runCatching { LocalDateTime.parse(normalizedSpaceSlashes, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")).atZone(zone).format(fmt) }.onSuccess { return it }
  // Try yyyy-MM-dd HH:mm -> yyyy-MM-ddTHH:mm
  val normalized = if (s.matches(Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"))) s.replace(' ', 'T') else s
  runCatching { LocalDateTime.parse(normalized).atZone(zone).format(fmt) }.onSuccess { return it }
  // Try dd/MM/yyyy (date only)
  runCatching { LocalDate.parse(normalizedSpaceSlashes, DateTimeFormatter.ofPattern("dd/MM/yyyy")).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())) }.onSuccess { return it }
  // Try yyyy-MM-dd (date only)
  runCatching { LocalDate.parse(s).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())) }.onSuccess { return it }
  // Try numeric inputs
  if (s.matches(Regex("^\\d+(\\.\\d+)?$"))) {
    // Epoch millis/seconds detection first
    runCatching {
      val asLong = s.toLong()
      val instant = when {
        asLong > 1_000_000_000_000L -> java.time.Instant.ofEpochMilli(asLong) // clearly millis
        asLong in 1_000_000_000L..1_000_000_000_000L -> java.time.Instant.ofEpochSecond(asLong) // seconds range
        else -> null
      }
      if (instant != null) return instant.atZone(zone).format(fmt)
    }
    // Fallback: Google Sheets/Excel serial number (e.g., 45968.333333336)
    runCatching {
      val v = s.toDouble()
      val days = kotlin.math.floor(v)
      val seconds = Math.round((v - days) * 86400.0) // Long
      val base = LocalDateTime.of(1899, 12, 30, 0, 0) // Excel epoch
      val dt = base.plusDays(days.toLong()).plusSeconds(seconds)
      return dt.atZone(zone).format(fmt)
    }
  }
  return s
}
