@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mascotasapp.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import com.example.mascotasapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.data.repository.PetsRepository
import com.example.mascotasapp.data.repository.MedicationsRepository
import com.example.mascotasapp.data.repository.RoutinesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONArray
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable
fun DashboardScreen(
    onOpenHealth: () -> Unit = {},
    onOpenRoutine: () -> Unit = {},
    onAddVisit: () -> Unit = {},
    onAddMedication: () -> Unit = {},
    onAddRoutine: () -> Unit = {},
    onAddPet: () -> Unit = {},
    addPetIconResId: Int? = null,
    onOpenPetProfile: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        SelectedPetStore.init(ctx)
        PetsRepository.init(ctx)
        MedicationsRepository.init(ctx)
        RoutinesRepository.init(ctx)
        val baseUrl = ApiConfig.BASE_URL
        // Load cache immediately, then refresh in background
        runCatching { PetsRepository.refresh(baseUrl) }
        var id = SelectedPetStore.get()
        if (id == null) {
            val first = PetsRepository.pets.value.firstOrNull()
            if (first != null) {
                SelectedPetStore.set(first.pet_id)
                id = first.pet_id
            }
        }
        snackbarHostState.showSnackbar("Selected pet: ${id ?: "none"}")
    }
    // Determine if user has pets and which one is selected
    val pets by PetsRepository.pets.collectAsStateWithLifecycle()
    val selectedPetId by SelectedPetStore.selectedPetId.collectAsStateWithLifecycle()
    val selectedPet = remember(pets, selectedPetId) {
        pets.firstOrNull { it.pet_id == selectedPetId } ?: pets.firstOrNull()
    }
    val hasPets = pets.isNotEmpty()

    // Load meds and routines for selected pet, and build UI models
    val medsFlow = remember(selectedPetId) {
        selectedPetId?.let { MedicationsRepository.medsFlow(it) }
    }
    val routinesFlow = remember(selectedPetId) {
        selectedPetId?.let { RoutinesRepository.routinesFlow(it) }
    }
    val meds = medsFlow?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val routines = routinesFlow?.collectAsStateWithLifecycle()?.value ?: emptyList()

    LaunchedEffect(selectedPetId) {
        val id = selectedPetId ?: return@LaunchedEffect
        val base = ApiConfig.BASE_URL
        runCatching { MedicationsRepository.refresh(base, id) }
        runCatching { RoutinesRepository.refresh(base, id) }
    }

    // Compute next upcoming action (top green card)
    fun parseTime(s: String): java.time.LocalDateTime? = runCatching {
        if (s.isBlank()) null else java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }.getOrNull()
    data class NextCard(val title: String, val subtitle: String)
    val nextCard: NextCard? = run {
        val nextMed = meds.minByOrNull { parseTime(it.next_dose) ?: java.time.LocalDateTime.MAX }
        val nextRut = routines.minByOrNull { parseTime(it.next_activity) ?: java.time.LocalDateTime.MAX }
        val medTime = nextMed?.let { parseTime(it.next_dose) }
        val rutTime = nextRut?.let { parseTime(it.next_activity) }
        when {
            medTime != null && (rutTime == null || medTime.isBefore(rutTime)) ->
                NextCard(title = "Next medication", subtitle = "${nextMed.medication_name} at ${nextMed.next_dose}")
            rutTime != null ->
                NextCard(title = "Next routine", subtitle = "${nextRut.routine_name} at ${nextRut.next_activity}")
            else -> null
        }
    }

    // Build recent created items list (bottom section)
    val recent: List<Activity> = run {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        fun parseCreated(v: String): java.time.LocalDateTime = runCatching { java.time.LocalDateTime.parse(v, fmt) }.getOrElse { java.time.LocalDateTime.MIN }
        val medActivities = meds.filter { it.created_at.isNotBlank() }.map {
            Activity(
                title = "Medication added",
                subtitle = it.medication_name,
                time = it.created_at,
                icon = Icons.Default.Vaccines,
                bg = Color(0xFFCFFAFE),
                tint = Color(0xFF0891B2)
            ) to parseCreated(it.created_at)
        }
        val rutActivities = routines.filter { it.created_at.isNotBlank() }.map {
            Activity(
                title = "Routine added",
                subtitle = it.routine_name,
                time = it.created_at,
                icon = Icons.Default.Schedule,
                bg = Color(0xFFDCFCE7),
                tint = Color(0xFF16A34A)
            ) to parseCreated(it.created_at)
        }
        medActivities.plus(rutActivities)
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = Color.White, tonalElevation = 0.dp, shadowElevation = 0.dp) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Pets,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PetCare",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111827)
                            )
                        }
                    },
                    actions = {
                        val showMenu = remember { mutableStateOf(false) }
                        IconButton(onClick = { /* notifications */ }, modifier = Modifier.padding(end = 1.dp)) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Notifications"
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu.value = true }, modifier = Modifier.padding(end = 1.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            DropdownMenu(expanded = showMenu.value, onDismissRequest = { showMenu.value = false }) {
                                DropdownMenuItem(
                                    text = { Text("Profile") },
                                    onClick = {
                                        showMenu.value = false
                                        onOpenProfile()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        scrolledContainerColor = Color.White
                    ),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
            }
        },
        containerColor = Color(0xFFF9FAFB),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)

        val primary = MaterialTheme.colorScheme.primary

        when (hasPets) {
            null -> { // loading: show nothing special to keep it light
                Box(modifier = contentModifier)
            }
            false -> {
                EmptyWelcome(onCreatePet = onAddPet, modifier = contentModifier)
            }
            else -> {
                LazyColumn(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PetCard(
                            petName = selectedPet?.name ?: "Pet",
                            petBreed = selectedPet?.breed ?: "",
                            petAgeYears = selectedPet?.date_of_birth?.let { safeYearsFromDob(it) } ?: 0,
                            nextTitle = nextCard?.title,
                            nextSubtitle = nextCard?.subtitle,
                            onOpenHealth = onOpenHealth,
                            onOpenRoutine = onOpenRoutine,
                        )
                    }
                    item { QuickLogSection(onAddVisit, onAddMedication, onAddRoutine, onAddPet, addPetIconResId) }
                    item {
                        val petName = selectedPet?.name ?: "your pet"
                        RecentActivitySectionForPet(petName = petName, recent = recent)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWelcome(onCreatePet: () -> Unit, modifier: Modifier = Modifier) {
    val brandPurple = Color(0xFF8B5CF6)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = Color(0xFF00B784), shape = CircleShape) {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = Color(0xFFF9FAFB),
                modifier = Modifier
                    .size(80.dp)
                    .padding(16.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Welcome to\nPetCare!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF111827),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Keep your pets happy and healthy with personalized care routines",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreatePet,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = brandPurple,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFDDD6FE)
            )
        ) {
            Text("Create New Pet")
        }
    }
}

data class Activity(
    val title: String,
    val subtitle: String,
    val time: String,
    val icon: ImageVector,
    val bg: Color,
    val tint: Color
)

@Composable
private fun PetCard(
    petName: String,
    petBreed: String,
    petAgeYears: Int,
    nextTitle: String?,
    nextSubtitle: String?,
    onOpenHealth: () -> Unit,
    onOpenRoutine: () -> Unit,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.mascota),
                    contentDescription = "Pet avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(petName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    val subtitle = buildString {
                        if (petBreed.isNotBlank()) append(petBreed)
                        if (petAgeYears >= 0) {
                            if (isNotEmpty()) append(" • ")
                            append("$petAgeYears years")
                        }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                }
                Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }

            if (nextTitle.isNullOrBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF33C59D)), shape = MaterialTheme.shapes.medium) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("No recent activity.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            } else {
                InfoRowCard(
                    title = nextTitle,
                    subtitle = nextSubtitle ?: "",
                    icon = Icons.Default.Event,
                    onClick = onOpenRoutine
                )
            }
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
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuickLogSection(
    onAddVisit: () -> Unit,
    onAddMedication: () -> Unit,
    onAddRoutine: () -> Unit,
    onAddPet: () -> Unit,
    addPetIconResId: Int?
) {
    Text(text = "Quick Log", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Add Visit",
                icon = Icons.Default.MedicalServices,
                onClick = onAddVisit,
                lightBg = Color(0xFFDBEAFE),
                darkAccent = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Add Medication",
                icon = Icons.Default.Vaccines,
                onClick = onAddMedication,
                lightBg = Color(0xFFCFFAFE),
                darkAccent = Color(0xFF0891B2),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Add Routine",
                icon = Icons.Default.Schedule,
                onClick = onAddRoutine,
                lightBg = Color(0xFFDCFCE7),
                darkAccent = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Add pet",
                icon = Icons.Default.Pets,
                onClick = onAddPet,
                lightBg = Color(0xFFFFEDD5),
                darkAccent = Color(0xFFEA580C),
                modifier = Modifier.weight(1f),
                painterResId = addPetIconResId
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
    darkAccent: Color = MaterialTheme.colorScheme.primary,
    painterResId: Int? = null
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
                    if (painterResId != null) {
                        Image(
                            painter = painterResource(id = painterResId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = darkAccent,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(8.dp)
                        )
                    }
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
private fun RecentActivitySectionForPet(petName: String, recent: List<Activity>) {
    Text(text = "Recent Activity", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    if (recent.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recent activity for $petName.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { item ->
            ActivityItem(
                title = item.title,
                subtitle = item.subtitle,
                time = item.time,
                icon = item.icon,
                bg = item.bg,
                tint = item.tint
            )
            }
        }
    }
}

// Age helpers (keep consistent with PetsScreen)
private fun safeYearsFromDob(dob: String): Int = runCatching {
    if (dob.isBlank()) return 0
    val today = java.time.LocalDate.now()
    val birth = parseFlexibleDate(dob) ?: return 0
    java.time.Period.between(birth, today).years.coerceAtLeast(0)
}.getOrDefault(0)

private fun parseFlexibleDate(input: String): java.time.LocalDate? {
    val s = input.trim()
    runCatching {
        val inst = java.time.Instant.parse(s)
        return inst.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }
    val patterns = arrayOf("yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyyMMdd")
    for (p in patterns) {
        val fmt = java.time.format.DateTimeFormatter.ofPattern(p)
        val parsed = runCatching { java.time.LocalDate.parse(s, fmt) }.getOrNull()
        if (parsed != null) return parsed
    }
    if (s.length >= 10) {
        val first10 = s.substring(0, 10)
        val parsed = runCatching { java.time.LocalDate.parse(first10) }.getOrNull()
        if (parsed != null) return parsed
    }
    val yearOnly = s.takeWhile { it.isDigit() }
    if (yearOnly.length == 4) {
        val year = runCatching { yearOnly.toInt() }.getOrNull()
        if (year != null && year in 1900..3000) return java.time.LocalDate.of(year, 1, 1)
    }
    return null
}

@Composable
private fun ActivityItem(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    bg: Color,
    tint: Color,
    modifier: Modifier = Modifier,
) {
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
                Surface(color = bg, shape = CircleShape) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(10.dp))
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
