package com.example.mascotasapp.ui.screens.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import com.example.mascotasapp.ui.components.LabeledField
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.data.repository.PetsRepository
import com.example.mascotasapp.data.repository.MedicationsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFormScreen(
    title: String = "Add Medication",
    confirmButtonText: String = "+ Add Medication",
    initialName: String = "",
    initialDateTime: String = "",
    initialEveryValue: String = "",
    initialEveryUnit: String = "hours",
    initialDoseValue: String = "",
    initialDoseUnit: String = "mg",
    initialTotalDoses: String = "",
    initialAlsoBuddy: Boolean = true,
    initialAlsoLuna: Boolean = false,
    onConfirm: (name: String) -> Unit,
    onConfirmWithTotal: (name: String, totalDoses: Int) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val bgSurface = Color(0xFFF9FAFB)
    val purple = Color(0xFF8B5CF6)
    val context = LocalContext.current

    var medName by remember { mutableStateOf(initialName) }
    var dateTime by remember { mutableStateOf(initialDateTime) }
    var everyValue by remember { mutableStateOf(initialEveryValue) }
    var everyUnit by remember { mutableStateOf(initialEveryUnit) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateTimeError by remember { mutableStateOf<String?>(null) }
    var everyValueError by remember { mutableStateOf<String?>(null) }
    var everyUnitError by remember { mutableStateOf<String?>(null) }

    val timeUnits = listOf("hours", "days", "weeks", "months")
    var timeUnitMenu by remember { mutableStateOf(false) }

    var doseValue by remember { mutableStateOf(initialDoseValue) }
    var doseUnit by remember { mutableStateOf(initialDoseUnit) }
    val doseUnits = listOf("mg", "ml", "g", "drops", "tablet")
    var doseUnitMenu by remember { mutableStateOf(false) }
    var doseValueError by remember { mutableStateOf<String?>(null) }
    var doseUnitError by remember { mutableStateOf<String?>(null) }

    var totalDoses by remember { mutableStateOf(initialTotalDoses) }
    var totalDosesError by remember { mutableStateOf<String?>(null) }

    var alsoBuddy by remember { mutableStateOf(initialAlsoBuddy) }
    var alsoLuna by remember { mutableStateOf(initialAlsoLuna) }

    // Dynamic pets list from repository
    LaunchedEffect(Unit) {
        SelectedPetStore.init(context)
        PetsRepository.init(context)
    }
    val selectedPetId by SelectedPetStore.selectedPetId.collectAsState(initial = SelectedPetStore.get())
    val pets by PetsRepository.pets.collectAsState()
    val additionalSelected = remember { mutableStateOf<MutableSet<String>>(mutableSetOf()) }

    // Validators
    fun validateName(v: String): String? {
        val t = v.trim()
        val regex = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$")
        return when {
            t.isEmpty() -> "Required"
            t.length < 2 -> "Min 2 characters"
            t.length > 50 -> "Max 50 characters"
            !regex.matches(t) -> "Only letters, spaces, ', ."
            else -> null
        }
    }
    fun validateDateTime(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        val pattern = Regex("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}$")
        if (!pattern.matches(t)) return "Format YYYY-MM-DD HH:mm"
        return try {
            val parts = t.split(" ")
            val datePart = parts[0]
            val timePart = parts[1]
            
            val date = LocalDate.parse(datePart)
            val timeParts = timePart.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            
            when {
                date.year < 1900 -> "Year must be ≥ 1900"
                hour !in 0..23 -> "Invalid hour"
                minute !in 0..59 -> "Invalid minute"
                else -> {
                    val candidate = LocalDateTime.of(date, LocalTime.of(hour, minute))
                    val now = LocalDateTime.now()
                    if (!candidate.isAfter(now)) "Must be in the future" else null
                }
            }
        } catch (e: Exception) {
            "Invalid date/time"
        }
    }
    fun validateEveryValue(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        if (!t.all { it.isDigit() }) return "Positive integer"
        val n = t.toIntOrNull() ?: return "Positive integer"
        return if (n <= 0) "Must be > 0" else null
    }
    fun validateEveryUnit(v: String): String? = if (v.isBlank()) "Required" else if (!timeUnits.contains(v)) "Invalid option" else null
    fun validateDoseValue(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        val re = Regex("^\\d+(\\.\\d{1,2})?$")
        if (!re.matches(t)) return "Up to 2 decimals"
        val n = t.toDoubleOrNull() ?: return "Invalid number"
        return if (n <= 0.0) "Must be > 0" else null
    }
    fun validateDoseUnit(v: String): String? = if (v.isBlank()) "Required" else if (!doseUnits.contains(v)) "Invalid option" else null
    fun validateTotalDoses(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        if (!t.all { it.isDigit() }) return "Positive integer"
        val n = t.toIntOrNull() ?: return "Positive integer"
        return if (n <= 0) "Must be > 0" else null
    }

    val formValid by remember(medName, dateTime, everyValue, everyUnit, doseValue, doseUnit, totalDoses) {
        mutableStateOf(
            validateName(medName) == null &&
                validateDateTime(dateTime) == null &&
                validateEveryValue(everyValue) == null &&
                validateEveryUnit(everyUnit) == null &&
                validateDoseValue(doseValue) == null &&
                validateDoseUnit(doseUnit) == null &&
                validateTotalDoses(totalDoses) == null
        )
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val vm: MedicationFormViewModel = viewModel()
    val ui by vm.uiState.collectAsState()
    LaunchedEffect(formValid) { vm.updateFormValidity(formValid) }

    LaunchedEffect(ui.snackbarMessage) {
        val msg = ui.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(message = msg, withDismissAction = true, duration = SnackbarDuration.Short)
            vm.consumeSnackbar()
        }
    }

    LaunchedEffect(ui.navigationEvent) {
        when (ui.navigationEvent) {
            is MedicationFormViewModel.NavigationEvent.NavigateBack -> {
                vm.consumeNavigation()
                onBack()
            }
            null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        },
        containerColor = bgSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.White) {
                Column {
                    Divider(color = Color(0xFFE5E7EB))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = {
                                nameError = validateName(medName)
                                dateTimeError = validateDateTime(dateTime)
                                everyValueError = validateEveryValue(everyValue)
                                everyUnitError = validateEveryUnit(everyUnit)
                                doseValueError = validateDoseValue(doseValue)
                                doseUnitError = validateDoseUnit(doseUnit)
                                totalDosesError = validateTotalDoses(totalDoses)
                                if (listOf(nameError, dateTimeError, everyValueError, everyUnitError, doseValueError, doseUnitError, totalDosesError).all { it == null }) {
                                    vm.onSubmit(
                                        context = context,
                                        name = medName,
                                        startOfSupply = dateTime,
                                        everyNumber = everyValue,
                                        everyUnit = everyUnit,
                                        doseNumber = doseValue,
                                        doseUnit = doseUnit,
                                        totalDoses = totalDoses.toInt(),
                                        additionalPetIds = additionalSelected.value
                                    )
                                    onConfirm(medName)
                                    onConfirmWithTotal(medName, totalDoses.toInt())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = purple,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFDDD6FE)
                            ),
                            enabled = formValid && !ui.isSubmitting
                        ) {
                            if (ui.isSubmitting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text(confirmButtonText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Medication Name
            item {
                LabeledField(label = "Medication Name *") {
                    OutlinedTextField(
                        value = medName,
                        onValueChange = {
                            medName = it
                            nameError = validateName(it)
                        },
                        placeholder = { Text("Enter medication name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError != null
                    )
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // First/latest supply datetime
            item {
                LabeledField(label = "First or latest performed supply (YYYY-MM-DD HH:mm) *") {
                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = {
                            dateTime = it
                            dateTimeError = validateDateTime(it)
                        },
                        placeholder = { Text("YYYY-MM-DD HH:mm") },
                        trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = dateTimeError != null
                    )
                    if (dateTimeError != null) {
                        Text(dateTimeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // Perform every (value + unit)
            item {
                LabeledField(label = "Perform every *") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = everyValue,
                            onValueChange = {
                                everyValue = it.filter { ch -> ch.isDigit() }
                                everyValueError = validateEveryValue(everyValue)
                            },
                            placeholder = { Text("0") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            isError = everyValueError != null
                        )
                        ExposedDropdownMenuBox(expanded = timeUnitMenu, onExpandedChange = { timeUnitMenu = !timeUnitMenu }) {
                            OutlinedTextField(
                                value = everyUnit,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeUnitMenu) },
                                modifier = Modifier.menuAnchor().weight(1f),
                                isError = everyUnitError != null
                            )
                            ExposedDropdownMenu(expanded = timeUnitMenu, onDismissRequest = { timeUnitMenu = false }) {
                                timeUnits.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        everyUnit = option
                                        everyUnitError = validateEveryUnit(option)
                                        timeUnitMenu = false
                                    })
                                }
                            }
                        }
                    }
                    if (everyValueError != null) {
                        Text(everyValueError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (everyUnitError != null) {
                        Text(everyUnitError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // Dose (value + unit)
            item {
                LabeledField(label = "Dose *") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = doseValue,
                            onValueChange = {
                                // allow digits and one '.'; validator will enforce max 2 decimals
                                var candidate = it.filter { ch -> ch.isDigit() || ch == '.' }
                                val firstDot = candidate.indexOf('.')
                                if (firstDot != -1) {
                                    // remove extra dots
                                    candidate = candidate.substring(0, firstDot + 1) + candidate.substring(firstDot + 1).replace(".", "")
                                }
                                doseValue = candidate
                                doseValueError = validateDoseValue(doseValue)
                            },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            isError = doseValueError != null
                        )
                        ExposedDropdownMenuBox(expanded = doseUnitMenu, onExpandedChange = { doseUnitMenu = !doseUnitMenu }) {
                            OutlinedTextField(
                                value = doseUnit,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doseUnitMenu) },
                                modifier = Modifier.menuAnchor().weight(1f),
                                isError = doseUnitError != null
                            )
                            ExposedDropdownMenu(expanded = doseUnitMenu, onDismissRequest = { doseUnitMenu = false }) {
                                doseUnits.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        doseUnit = option
                                        doseUnitError = validateDoseUnit(option)
                                        doseUnitMenu = false
                                    })
                                }
                            }
                        }
                    }
                    if (doseValueError != null) {
                        Text(doseValueError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (doseUnitError != null) {
                        Text(doseUnitError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // Total Doses
            item {
                LabeledField(label = "Total Doses *") {
                    OutlinedTextField(
                        value = totalDoses,
                        onValueChange = {
                            totalDoses = it.filter { ch -> ch.isDigit() }
                            totalDosesError = validateTotalDoses(totalDoses)
                        },
                        placeholder = { Text("0") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = totalDosesError != null
                    )
                    if (totalDosesError != null) {
                        Text(totalDosesError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // Also add to
            item { Text("Also add to", style = MaterialTheme.typography.titleSmall) }
            item {
                Column(Modifier.fillMaxWidth()) {
                    val others = pets.filter { it.pet_id != (selectedPetId ?: "") }
                    others.forEach { pet ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val checked = pet.pet_id in additionalSelected.value
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) additionalSelected.value.add(pet.pet_id)
                                    else additionalSelected.value.remove(pet.pet_id)
                                    // trigger recomposition
                                    additionalSelected.value = additionalSelected.value.toMutableSet()
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = purple,
                                    uncheckedColor = Color(0xFF9CA3AF),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(pet.name)
                        }
                    }
                    if (others.isEmpty()) {
                        Text("No other pets available", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
} 