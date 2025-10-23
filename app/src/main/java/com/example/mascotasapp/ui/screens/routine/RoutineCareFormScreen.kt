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
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.core.JsonUtils
import com.example.mascotasapp.data.repository.RoutinesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCareFormScreen(
    title: String,
    confirmButtonText: String,
    initialName: String = "",
    initialDateTime: String = "",
    initialEveryValue: String = "",
    initialEveryUnit: String = "hours",
    initialAlsoBuddy: Boolean = true,
    initialAlsoLuna: Boolean = false,
    onConfirm: (name: String) -> Unit,
    onBack: () -> Unit
) {
    val bgSurface = Color(0xFFF9FAFB)
    val purple = Color(0xFF8B5CF6)

    var careName by remember { mutableStateOf(initialName) }
    var dateTime by remember { mutableStateOf(initialDateTime) }
    var everyValue by remember { mutableStateOf(initialEveryValue) }
    var everyUnit by remember { mutableStateOf(initialEveryUnit) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateTimeError by remember { mutableStateOf<String?>(null) }
    var everyValueError by remember { mutableStateOf<String?>(null) }
    var everyUnitError by remember { mutableStateOf<String?>(null) }

    val units = listOf("hours", "days", "weeks", "months")
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var alsoBuddy by remember { mutableStateOf(initialAlsoBuddy) }
    var alsoLuna by remember { mutableStateOf(initialAlsoLuna) }

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
            val today = LocalDate.now()
            val timeParts = timePart.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            
            when {
                date.isAfter(today) -> "Cannot be in the future"
                date.year < 1900 -> "Year must be ≥ 1900"
                date.isBefore(today.minusYears(40)) -> "Unrealistic age"
                hour !in 0..23 -> "Invalid hour"
                minute !in 0..59 -> "Invalid minute"
                else -> null
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
    fun validateEveryUnit(v: String): String? = if (v.isBlank()) "Required" else if (!units.contains(v)) "Invalid option" else null

    val formValid by remember(careName, dateTime, everyValue, everyUnit) {
        mutableStateOf(
            validateName(careName) == null &&
                validateDateTime(dateTime) == null &&
                validateEveryValue(everyValue) == null &&
                validateEveryUnit(everyUnit) == null
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    val baseUrl = ApiConfig.BASE_URL
    val isEdit = title.contains("Edit", ignoreCase = true) || confirmButtonText.contains("Save", ignoreCase = true)

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
                                nameError = validateName(careName)
                                dateTimeError = validateDateTime(dateTime)
                                everyValueError = validateEveryValue(everyValue)
                                everyUnitError = validateEveryUnit(everyUnit)
                                val valid = listOf(nameError, dateTimeError, everyValueError, everyUnitError).all { it == null }
                                if (valid) {
                                    val petId = SelectedPetStore.get()
                                    if (petId.isNullOrBlank()) {
                                        scope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar("Select a pet first") }
                                    } else {
                                        isSubmitting = true
                                        scope.launch {
                                            try {
                                                if (!isEdit) {
                                                    val url = URL(baseUrl + "/createRoutine")
                                                    val conn = (url.openConnection() as HttpURLConnection).apply {
                                                        requestMethod = "POST"
                                                        doOutput = true
                                                        setRequestProperty("Content-Type", "application/json")
                                                        setRequestProperty("X-Debug-Uid", "dev-user")
                                                    }
                                                    val payload = """
                                                        {
                                                          "routine_name": ${JsonUtils.q(careName.trim())},
                                                          "start_of_activity": ${JsonUtils.q(dateTime.trim())},
                                                          "perform_every_number": ${JsonUtils.q(everyValue.trim())},
                                                          "perform_every_unit": ${JsonUtils.q(everyUnit.trim())},
                                                          "assign_to_pets": [${JsonUtils.q(petId)}]
                                                        }
                                                    """.trimIndent()
                                                    conn.outputStream.use { os -> java.io.OutputStreamWriter(os).use { it.write(payload) } }
                                                    val code = conn.responseCode
                                                    val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                                                    withContext(Dispatchers.Main) {
                                                        if (code in 200..299) {
                                                            snackbarHostState.showSnackbar("Routine created")
                                                            RoutinesRepository.refresh(baseUrl, petId)
                                                            onConfirm(careName)
                                                        } else {
                                                            snackbarHostState.showSnackbar("Error $code: $resp")
                                                        }
                                                    }
                                                } else {
                                                    // Edit -> PUT /routines
                                                    val url = URL(baseUrl + "/routines")
                                                    val conn = (url.openConnection() as HttpURLConnection).apply {
                                                        requestMethod = "PUT"
                                                        doOutput = true
                                                        setRequestProperty("Content-Type", "application/json")
                                                        setRequestProperty("X-Debug-Uid", "dev-user")
                                                    }
                                                    // In a follow-up we can pass routine_id from nav args; for now using name as placeholder is not sufficient.
                                                    // This block will be completed when edit wiring is defined with IDs.
                                                    val payload = "{}"
                                                    conn.outputStream.use { os -> java.io.OutputStreamWriter(os).use { it.write(payload) } }
                                                    val code = conn.responseCode
                                                    val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                                                    withContext(Dispatchers.Main) {
                                                        if (code in 200..299) {
                                                            snackbarHostState.showSnackbar("Routine updated")
                                                            RoutinesRepository.refresh(baseUrl, petId)
                                                            onConfirm(careName)
                                                        } else {
                                                            snackbarHostState.showSnackbar("Error $code: $resp")
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Network error: ${e.message}") }
                                            } finally {
                                                withContext(Dispatchers.Main) { isSubmitting = false }
                                            }
                                        }
                                    }
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
                            enabled = formValid && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = Color.White)
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
            item { LabeledField(label = "Care Name *") {
                OutlinedTextField(
                    value = careName,
                    onValueChange = {
                        careName = it
                        nameError = validateName(it)
                    },
                    placeholder = { Text("Enter care name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null
                )
                if (nameError != null) {
                    Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }}
            item { LabeledField(label = "First or latest performed execution (YYYY-MM-DD HH:mm) *") {
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
            }}
            item { LabeledField(label = "Perform every *") {
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
                    ExposedDropdownMenuBox(
                        expanded = unitMenuExpanded,
                        onExpandedChange = { unitMenuExpanded = !unitMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = everyUnit,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                            modifier = Modifier.menuAnchor().weight(1f),
                            isError = everyUnitError != null
                        )
                        ExposedDropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            units.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    everyUnit = option
                                    everyUnitError = validateEveryUnit(option)
                                    unitMenuExpanded = false
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
            }}
            if (!isEdit) {
                item { Text("Also add to", style = MaterialTheme.typography.titleSmall) }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = alsoBuddy,
                                onCheckedChange = { alsoBuddy = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = purple,
                                    uncheckedColor = Color(0xFF9CA3AF),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text("Buddy")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = alsoLuna,
                                onCheckedChange = { alsoLuna = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = purple,
                                    uncheckedColor = Color(0xFF9CA3AF),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text("Luna")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        content()
    }
}
