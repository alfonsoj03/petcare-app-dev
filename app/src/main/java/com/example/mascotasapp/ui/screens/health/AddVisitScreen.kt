package com.petcare.mascotasapp.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.imePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitScreen(
    onBack: () -> Unit = {},
    onSave: (visitType: String, date: String, time: String, clinic: String, vet: String, notes: String) -> Unit = { _,_,_,_,_,_ -> },
    onCancel: () -> Unit = {}
) {
    val brandPurple = Color(0xFF8B5CF6)
    val bgSurface = Color(0xFFF9FAFB)
    val green = Color(0xFF10B981)

    var visitTypeExpanded by remember { mutableStateOf(false) }
    val visitTypes = listOf("Routine Checkup", "Vaccination", "Emergency", "Surgery", "Follow-up", "Other")
    var visitType by remember { mutableStateOf("") }

    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var clinic by remember { mutableStateOf(TextFieldValue("")) }
    var vet by remember { mutableStateOf(TextFieldValue("")) }
    var notes by remember { mutableStateOf(TextFieldValue("")) }

    var visitTypeError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var clinicError by remember { mutableStateOf<String?>(null) }
    var vetError by remember { mutableStateOf<String?>(null) }
    var notesError by remember { mutableStateOf<String?>(null) }

    // Track interaction per field
    var touchedVisitType by remember { mutableStateOf(false) }
    var touchedDate by remember { mutableStateOf(false) }
    var touchedTime by remember { mutableStateOf(false) }
    var touchedClinic by remember { mutableStateOf(false) }
    var touchedVet by remember { mutableStateOf(false) }
    var touchedNotes by remember { mutableStateOf(false) }

    fun validateVisitType(value: String): String? {
        if (value.isBlank()) return "Visit type is required"
        if (!visitTypes.contains(value)) return "Invalid visit type"
        return null
    }

    fun validateDate(value: String): String? {
        if (value.isBlank()) return "Date is required"
        val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (!regex.matches(value)) return "Use format YYYY-MM-DD"
        return try {
            // Basic validity check
            java.time.LocalDate.parse(value)
            null
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    fun validateTime(value: String): String? {
        if (value.isBlank()) return "Time is required"
        val regex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        if (!regex.matches(value)) return "Use format HH:mm (24h)"
        return null
    }

    fun validateClinic(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "Clinic is required"
        if (trimmed.length < 2) return "Minimum 2 characters"
        if (trimmed.length > 100) return "Maximum 100 characters"
        val regex = Regex("^[A-Za-z0-9 .`,]+$")
        if (!regex.matches(trimmed)) return "Only letters, numbers, spaces, and , . `"
        return null
    }

    fun validateVet(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null // Optional
        if (trimmed.length < 2) return "Minimum 2 characters"
        if (trimmed.length > 100) return "Maximum 100 characters"
        val regex = Regex("^[A-Za-z '.]+$")
        if (!regex.matches(trimmed)) return "Only letters, spaces, apostrophes and periods"
        return null
    }

    fun validateNotes(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.length > 500) return "Maximum 500 characters"
        return null
    }

    fun validateAll() {
        visitTypeError = validateVisitType(visitType)
        dateError = validateDate(date)
        timeError = validateTime(time)
        clinicError = validateClinic(clinic.text)
        vetError = validateVet(vet.text)
        notesError = validateNotes(notes.text)
    }

    LaunchedEffect(visitType, date, time, clinic.text, vet.text, notes.text) {
        validateAll()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Visit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(green),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),
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
                    val formValid = listOf(visitTypeError, dateError, timeError, clinicError, vetError, notesError).all { it == null } &&
                            visitType.isNotBlank() && date.isNotBlank() && time.isNotBlank() && clinic.text.trim().isNotBlank()
                    Button(
                        onClick = {
                            validateAll()
                            if (formValid) {
                                onSave(visitType, date.trim(), time.trim(), clinic.text.trim(), vet.text.trim(), notes.text.trim())
                            }
                        },
                        enabled = formValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandPurple, contentColor = Color.White, disabledContainerColor = Color(0xFFDDD6FE)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save Visit") }
                }
            }
        }
    ) { inner ->
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scroll)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Visit Type *", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                ExposedDropdownMenuBox(
                    expanded = visitTypeExpanded,
                    onExpandedChange = {
                        visitTypeExpanded = !visitTypeExpanded
                        touchedVisitType = true
                    }
                ) {
                    OutlinedTextField(
                        value = visitType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = visitTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD1D5DB),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        placeholder = { Text("Select type") }
                    )
                    ExposedDropdownMenu(expanded = visitTypeExpanded, onDismissRequest = { visitTypeExpanded = false }) {
                        visitTypes.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                visitType = it
                                visitTypeExpanded = false
                                touchedVisitType = true
                            })
                        }
                    }
                }
                if (touchedVisitType && visitTypeError != null) {
                    Text(visitTypeError!!, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                }

                Text("Date & Time *", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it; touchedDate = true },
                        trailingIcon = { Icon(Icons.Filled.Today, contentDescription = null, tint = Color(0xFF111827)) },
                        modifier = Modifier.weight(1.08f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD1D5DB),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        placeholder = { Text("YYYY-MM-DD") }
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it; touchedTime = true },
                        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF111827)) },
                        modifier = Modifier.weight(0.92f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD1D5DB),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        placeholder = { Text("HH:mm") }
                    )
                }
                if (touchedDate && dateError != null) {
                    Text(dateError!!, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                }
                if (touchedTime && timeError != null) {
                    Text(timeError!!, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                }

                Text("Clinic / Location *", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = clinic,
                    onValueChange = { clinic = it; touchedClinic = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    placeholder = { Text("Clinic or location") }
                )
                if (touchedClinic && clinicError != null) {
                    Text(clinicError!!, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                }

                Text("Veterinarian", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = vet,
                    onValueChange = { vet = it; touchedVet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    placeholder = { Text("Optional") }
                )
                if (touchedVet && vetError != null) {
                    Text(vetError!!, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                }

                Text("Visit Notes", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; touchedNotes = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    placeholder = { Text("Add notes (optional)") }
                )
                if (touchedNotes && notesError != null) {
                    Text(notesError!!, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
