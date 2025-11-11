@file:OptIn(ExperimentalMaterial3Api::class)
package com.petcare.mascotasapp.ui.screens.pets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.petcare.mascotasapp.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.petcare.mascotasapp.core.ApiConfig
import com.petcare.mascotasapp.data.repository.PetsRepository
import com.petcare.mascotasapp.data.model.Pet
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.petcare.mascotasapp.core.SelectedPetStore
import com.petcare.mascotasapp.data.repository.MedicationsRepository
import com.petcare.mascotasapp.data.repository.RoutinesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

enum class EventType { VACCINE, VET, OVERDUE }

private fun com.petcare.mascotasapp.data.model.Pet.toExtended(): PetExtended {
    val dob = this.date_of_birth
    val ageY = safeYearsFromDob(dob)
    return PetExtended(
        id = this.pet_id.ifBlank { this.name },
        name = this.name,
        breed = this.breed,
        species = this.species,
        ageYears = ageY,
        upToDate = true,
        nextEvent = "—",
        eventType = EventType.VET
    )
}

data class PetExtended(
    val id: String,
    val name: String,
    val breed: String,
    val species: String,
    val ageYears: Int,
    val upToDate: Boolean,
    val nextEvent: String,
    val eventType: EventType,
    val imageRes: Int = R.drawable.mascota
)

@Composable
fun PetsScreen(
    onAddPet: () -> Unit = {},
    onOpenPet: (String) -> Unit = {},
    selectedPetId: String? = null,
    onSelectedPet: (PetExtended) -> Unit = {}
) {

    val repoPets by PetsRepository.pets.collectAsState()
    val pets = remember(repoPets) {
        repoPets.map { it.toExtended() }
    }

    val ctx = LocalContext.current
    LaunchedEffect(Unit) { SelectedPetStore.init(ctx) }
    val storeSelectedId by SelectedPetStore.selectedPetId.collectAsState(initial = selectedPetId)
    var selectedId by remember { mutableStateOf(storeSelectedId) }
    LaunchedEffect(storeSelectedId) { selectedId = storeSelectedId }
    var reloading by remember { mutableStateOf(false) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }

    LaunchedEffect(Unit) {
        // Ensure repo emits current cache, then refresh in background
        scope.launch { runCatching { PetsRepository.refresh(ApiConfig.BASE_URL) } }
    }

    LaunchedEffect(pets) {
        if (selectedId == null && pets.isNotEmpty()) {
            val firstId = pets.first().id
            SelectedPetStore.set(firstId)
        }
    }

    // Initialize Meds/Routines repositories once
    LaunchedEffect(Unit) {
        MedicationsRepository.init(ctx)
        RoutinesRepository.init(ctx)
    }

    // Background refresh per pet to populate upcoming info
    LaunchedEffect(pets) {
        val base = ApiConfig.BASE_URL
        pets.forEach { p ->
            scope.launch { runCatching { MedicationsRepository.refresh(base, p.id) } }
            scope.launch { runCatching { RoutinesRepository.refresh(base, p.id) } }
        }
    }

    // Helpers used to parse/format event times
    fun parseTimeFlexible(s: String): java.time.LocalDateTime? {
        val t = s.trim()
        if (t.isBlank()) return null
        if (t.all { it.isDigit() }) {
            val num = t.toLongOrNull()
            if (num != null) {
                val inst = if (num > 1_000_000_000_000L) java.time.Instant.ofEpochMilli(num) else java.time.Instant.ofEpochSecond(num)
                return inst.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            }
        }
        runCatching { java.time.Instant.parse(t).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() }.getOrNull()?.let { return it }
        runCatching { java.time.LocalDateTime.parse(t, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()?.let { return it }
        return runCatching { java.time.LocalDateTime.parse(t) }.getOrNull()
    }

    fun formatForDisplay(ldt: java.time.LocalDateTime?): String {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return ldt?.format(fmt) ?: "—"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Pets",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                },
                actions = {
                    // Reload action
                    TextButton(onClick = {
                        if (!reloading) {
                            reloading = true
                            scope.launch {
                                runCatching { PetsRepository.refresh(ApiConfig.BASE_URL) }
                                launch(Dispatchers.Main) { reloading = false }
                            }
                        }
                    }) {
                        if (reloading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text("Reload")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    // Green circular paw icon as in wireframe
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        containerColor = Color(0xFFF9FAFB),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPet, containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Add, contentDescription = "Add Pet", tint = Color.White)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pets, key = { it.id }) { pet ->
                // Subscribe to meds/routines for this pet
                val meds by remember(pet.id) { MedicationsRepository.medsFlow(pet.id) }.collectAsState(initial = emptyList())
                val routines by remember(pet.id) { RoutinesRepository.routinesFlow(pet.id) }.collectAsState(initial = emptyList())
                val now = remember { java.time.LocalDateTime.now() }
                val candidate = run {
                    val medCands = meds.mapNotNull { m -> parseTimeFlexible(m.next_dose)?.let { it to ("med" to m.medication_name) } }
                        .filter { it.first.isAfter(now) || it.first.isEqual(now) }
                    val rutCands = routines.mapNotNull { r -> parseTimeFlexible(r.next_activity)?.let { it to ("rut" to r.routine_name) } }
                        .filter { it.first.isAfter(now) || it.first.isEqual(now) }
                    (medCands + rutCands).minByOrNull { it.first }
                }
                val nextText = candidate?.let { c ->
                    val label = c.second.second
                    val whenTxt = formatForDisplay(c.first)
                    "$label at $whenTxt"
                } ?: "—"
                // Determine overdue medication (any next_dose strictly before now)
                val overdueCandidate = meds
                    .mapNotNull { m -> parseTimeFlexible(m.next_dose)?.let { it to m.medication_name } }
                    .filter { it.first.isBefore(now) }
                    .minByOrNull { it.first }

                val petDisplay = if (overdueCandidate != null) {
                    val label = overdueCandidate.second
                    val whenTxt = formatForDisplay(overdueCandidate.first)
                    pet.copy(
                        upToDate = false,
                        nextEvent = "$label atrasado desde $whenTxt",
                        eventType = EventType.OVERDUE
                    )
                } else {
                    pet.copy(
                        upToDate = true,
                        nextEvent = nextText,
                        eventType = EventType.VET
                    )
                }

                PetRowCard(
                    pet = petDisplay,
                    selected = pet.id == selectedId,
                    onSelect = {
                        SelectedPetStore.set(pet.id)
                        onSelectedPet(petDisplay)
                    },
                    onEdit = { onOpenPet(pet.id) }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private fun safeYearsFromDob(dob: String): Int = runCatching {
    if (dob.isBlank()) return 0
    val today = java.time.LocalDate.now()
    val birth = parseFlexibleDate(dob) ?: return 0
    java.time.Period.between(birth, today).years.coerceAtLeast(0)
}.getOrDefault(0)

private fun parseFlexibleDate(input: String): java.time.LocalDate? {
    val s = input.trim()
    // Try ISO instant like 2023-05-01T00:00:00Z
    runCatching {
        val inst = java.time.Instant.parse(s)
        return inst.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }
    // Try common date patterns
    val patterns = arrayOf("yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyyMMdd")
    for (p in patterns) {
        val fmt = java.time.format.DateTimeFormatter.ofPattern(p)
        val parsed = runCatching { java.time.LocalDate.parse(s, fmt) }.getOrNull()
        if (parsed != null) return parsed
    }
    // Try first 10 chars if string contains datetime
    if (s.length >= 10) {
        val first10 = s.substring(0, 10)
        val parsed = runCatching { java.time.LocalDate.parse(first10) }.getOrNull()
        if (parsed != null) return parsed
    }
    // Try only year present
    val yearOnly = s.takeWhile { it.isDigit() }
    if (yearOnly.length == 4) {
        val year = runCatching { yearOnly.toInt() }.getOrNull()
        if (year != null && year in 1900..3000) return java.time.LocalDate.of(year, 1, 1)
    }
    return null
}

@Composable
private fun PetRowCard(
    pet: PetExtended,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else Color(0xFFF3F4F6)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, end = 12.dp, start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = when {
                pet.species.equals("Feline", ignoreCase = true) ->
                    "https://images.unsplash.com/photo-1543852786-1cf6624b9987?q=80&w=600&auto=format&fit=crop" // Persian-like cat
                pet.breed.equals("Beagle", ignoreCase = true) ->
                    "https://images.unsplash.com/photo-1587300003388-59208cc962cb?q=80&w=600&auto=format&fit=crop" // Beagle
                else -> null
            }
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.mascota),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${pet.breed} • ${pet.ageYears} years", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    StatusBadge(upToDate = pet.upToDate)
                }
                EventRow(text = pet.nextEvent, type = pet.eventType)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            androidx.compose.material3.ElevatedButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = androidx.compose.material3.ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34D399),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp) // respira más
            ) {
                Text(
                    "Edit Pet Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(upToDate: Boolean) {
    val bg = if (upToDate) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
    val dot = if (upToDate) Color(0xFF16A34A) else Color(0xFFEA580C)
    val text = if (upToDate) "Up to date" else "Pending"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bg, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).background(dot, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = dot)
    }
}

@Composable
private fun EventRow(text: String, type: EventType) {
    val (icon, tint) = when (type) {
        EventType.VACCINE -> Icons.Filled.MedicalServices to MaterialTheme.colorScheme.secondary
        EventType.VET -> Icons.Filled.Event to MaterialTheme.colorScheme.secondary
        EventType.OVERDUE -> Icons.Filled.WarningAmber to Color(0xFFEA580C)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))
    }
}
