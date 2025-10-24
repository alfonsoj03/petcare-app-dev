@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mascotasapp.ui.screens.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalContext
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.data.repository.PetsRepository
import com.example.mascotasapp.data.model.Pet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks

@Composable
fun AddPetScreen(onBack: () -> Unit = {}, onAdd: () -> Unit = {}) {
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        SelectedPetStore.init(ctx)
        val id = SelectedPetStore.get()
        snackbarHostState.showSnackbar("Selected pet: ${id ?: "none"}")
    }
    var name by remember { mutableStateOf("") }
    var speciesExpanded by remember { mutableStateOf(false) }
    val speciesOptions = listOf("Dog", "Cat", "Rabbit", "Bird", "Other")
    var species by remember { mutableStateOf("") }

    var sexExpanded by remember { mutableStateOf(false) }
    val sexOptions = listOf("Male", "Female", "Unknown")
    var sex by remember { mutableStateOf("") }

    var breed by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var speciesError by remember { mutableStateOf<String?>(null) }
    var sexError by remember { mutableStateOf<String?>(null) }
    var breedError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var colorError by remember { mutableStateOf<String?>(null) }

    val scope = remember { CoroutineScope(Dispatchers.IO) }
    val baseUrl = ApiConfig.BASE_URL

    var isSubmitting by remember { mutableStateOf(false) }

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
    fun validateSpecies(v: String): String? = when {
        v.isBlank() -> "Required"
        !speciesOptions.contains(v) -> "Invalid option"
        else -> null
    }
    fun validateSex(v: String): String? = when {
        v.isBlank() -> "Required"
        !sexOptions.contains(v) -> "Invalid option"
        else -> null
    }
    fun validateBreed(v: String): String? {
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
    fun validateDob(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        val pattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (!pattern.matches(t)) return "Format YYYY-MM-DD"
        return try {
            val date = LocalDate.parse(t)
            val today = LocalDate.now()
            when {
                date.isAfter(today) -> "Cannot be in the future"
                date.year < 1900 -> "Year must be ≥ 1900"
                date.isBefore(today.minusYears(40)) -> "Unrealistic age"
                else -> null
            }
        } catch (e: DateTimeParseException) {
            "Invalid date"
        }
    }
    fun validateWeight(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        val intRegex = Regex("^\\d+$")
        if (!intRegex.matches(t)) return "Positive integer"
        val n = t.toIntOrNull() ?: return "Positive integer"
        return if (n <= 0) "Must be > 0" else null
    }
    fun validateColor(v: String): String? {
        val t = v.trim()
        if (t.isEmpty()) return "Required"
        val colorRegex = Regex("^[A-Za-z ]+$")
        return when {
            !colorRegex.matches(t) -> "Only letters and spaces"
            t.length > 30 -> "Max 30 characters"
            else -> null
        }
    }

    val formValid by remember(name, species, sex, breed, dob, weight, color) {
        mutableStateOf(
            validateName(name) == null &&
                validateSpecies(species) == null &&
                validateSex(sex) == null &&
                validateBreed(breed) == null &&
                validateDob(dob) == null &&
                validateWeight(weight) == null &&
                validateColor(color) == null
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add New Pet", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
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
        containerColor = Color(0xFFF9FAFB),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.White) {
                Column {
                    Divider(color = Color(0xFFE5E7EB))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Button(
                            onClick = {
                                if (isSubmitting) return@Button
                                nameError = null
                                speciesError = null
                                sexError = null
                                breedError = null
                                dobError = null
                                weightError = null
                                colorError = null

                                val nameT = name.trim()
                                val breedT = breed.trim()
                                val dobT = dob.trim()
                                val weightT = weight.trim()
                                val colorT = color.trim()

                                val nameRegex = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ'., ]+$")
                                if (nameT.isEmpty()) nameError = "Required" else if (nameT.length < 2) nameError = "Min 2 characters" else if (nameT.length > 50) nameError = "Max 50 characters" else if (!nameRegex.matches(nameT)) nameError = "Only letters, spaces, ', ."

                                if (species.isBlank()) speciesError = "Required" else if (!speciesOptions.contains(species)) speciesError = "Invalid option"

                                if (sex.isBlank()) sexError = "Required" else if (!sexOptions.contains(sex)) sexError = "Invalid option"

                                if (breedT.isEmpty()) breedError = "Required" else if (breedT.length < 2) breedError = "Min 2 characters" else if (breedT.length > 50) breedError = "Max 50 characters" else if (!nameRegex.matches(breedT)) breedError = "Only letters, spaces, ', ."

                                if (dobT.isEmpty()) {
                                    dobError = "Required"
                                } else {
                                    val pattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                                    if (!pattern.matches(dobT)) {
                                        dobError = "Format YYYY-MM-DD"
                                    } else {
                                        try {
                                            val date = LocalDate.parse(dobT)
                                            val today = LocalDate.now()
                                            if (date.isAfter(today)) {
                                                dobError = "Cannot be in the future"
                                            } else if (date.year < 1900) {
                                                dobError = "Year must be ≥ 1900"
                                            } else {
                                                val fortyYearsAgo = today.minusYears(40)
                                                if (date.isBefore(fortyYearsAgo)) {
                                                    dobError = "Unrealistic age"
                                                }
                                            }
                                        } catch (e: DateTimeParseException) {
                                            dobError = "Invalid date"
                                        }
                                    }
                                }

                                if (weightT.isEmpty()) {
                                    weightError = "Required"
                                } else {
                                    val intRegex = Regex("^\\d+$")
                                    if (!intRegex.matches(weightT)) {
                                        weightError = "Positive integer"
                                    } else if (weightT.toIntOrNull()?.let { it <= 0 } != false) {
                                        weightError = "Must be > 0"
                                    }
                                }

                                if (colorT.isEmpty()) {
                                    colorError = "Required"
                                } else {
                                    val colorRegex = Regex("^[A-Za-z ]+$")
                                    if (!colorRegex.matches(colorT)) {
                                        colorError = "Only letters and spaces"
                                    } else if (colorT.length > 30) {
                                        colorError = "Max 30 characters"
                                    }
                                }

                                val valid = listOf(nameError, speciesError, sexError, breedError, dobError, weightError, colorError).all { it == null }
                                if (valid) {
                                    isSubmitting = true
                                    scope.launch {
                                        try {
                                            val auth = FirebaseAuth.getInstance()
                                            if (auth.currentUser == null) {
                                                Tasks.await(auth.signInAnonymously())
                                            }
                                            val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token

                                            val url = URL("$baseUrl/createPet")
                                            val conn = (url.openConnection() as HttpURLConnection).apply {
                                                requestMethod = "POST"
                                                doOutput = true
                                                setRequestProperty("Content-Type", "application/json")
                                                setRequestProperty("X-Debug-Uid", "dev-user")
                                                if (!idToken.isNullOrBlank()) {
                                                    setRequestProperty("Authorization", "Bearer $idToken")
                                                }
                                            }
                                            val payload = """
                                                {
                                                  "name": ${jsonQ(nameT)},
                                                  "species": ${jsonQ(species.trim())},
                                                  "sex": ${jsonQ(sex.trim())},
                                                  "breed": ${jsonQ(breedT)},
                                                  "dob": ${jsonQ(dobT)},
                                                  "weight": ${jsonQ(weightT)},
                                                  "color": ${jsonQ(colorT)},
                                                  "imageUrl": ${jsonQ("")}
                                                }
                                            """.trimIndent()
                                            conn.outputStream.use { os ->
                                                OutputStreamWriter(os).use { it.write(payload) }
                                            }
                                            val code = conn.responseCode
                                            val respText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                                                ?.bufferedReader()?.use { it.readText() } ?: ""
                                            withContext(Dispatchers.Main) {
                                                if (code in 200..299) {
                                                    // Try to extract pet_id from response
                                                    var newId: String? = null
                                                    try {
                                                        val obj = JSONObject(respText)
                                                        newId = when {
                                                            obj.has("pet_id") -> obj.getString("pet_id")
                                                            obj.has("pet") && obj.getJSONObject("pet").has("pet_id") -> obj.getJSONObject("pet").getString("pet_id")
                                                            else -> null
                                                        }
                                                        // Build Pet model and upsert into repository cache if possible
                                                        val petObj = obj
                                                        val pet = Pet(
                                                            pet_id = petObj.optString("pet_id", newId ?: ""),
                                                            user_id = petObj.optString("user_id"),
                                                            name = petObj.optString("name"),
                                                            imageUrl = petObj.optString("imageUrl"),
                                                            species = petObj.optString("species"),
                                                            sex = petObj.optString("sex"),
                                                            breed = petObj.optString("breed"),
                                                            date_of_birth = petObj.optString("date_of_birth", petObj.optString("dob")),
                                                            weight_kg = petObj.optString("weight_kg", petObj.optString("weight")),
                                                            color = petObj.optString("color"),
                                                            created_at = petObj.optString("created_at")
                                                        )
                                                        // Upsert in background
                                                        launch(Dispatchers.IO) { PetsRepository.upsert(pet) }
                                                    } catch (_: Exception) { }
                                                    if (newId != null) {
                                                        SelectedPetStore.set(newId)
                                                    }
                                                    snackbarHostState.showSnackbar("Pet created")
                                                    onAdd()
                                                    onBack()
                                                } else {
                                                    snackbarHostState.showSnackbar("Error $code: $respText")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                snackbarHostState.showSnackbar("Network error: ${e.message}")
                                            }
                                        } finally {
                                            withContext(Dispatchers.Main) { isSubmitting = false }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFDDD6FE)
                            ),
                            enabled = formValid && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("+ Add Pet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color(0xFF9CA3AF)) }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose from gallery",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { /* open picker */ }
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = validateName(it)
                },
                label = { Text("Pet Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                shape = RoundedCornerShape(12.dp)
            )
            if (nameError != null) {
                Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(expanded = speciesExpanded, onExpandedChange = { speciesExpanded = !speciesExpanded }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = species.ifBlank { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Species *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = speciesError != null
                    )
                    ExposedDropdownMenu(expanded = speciesExpanded, onDismissRequest = { speciesExpanded = false }) {
                        speciesOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                species = option
                                speciesError = validateSpecies(option)
                                speciesExpanded = false
                            })
                        }
                    }
                }
                if (speciesError != null) {
                    Text(speciesError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                ExposedDropdownMenuBox(expanded = sexExpanded, onExpandedChange = { sexExpanded = !sexExpanded }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = sex.ifBlank { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sex *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = sexError != null
                    )
                    ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                        sexOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                sex = option
                                sexError = validateSex(option)
                                sexExpanded = false
                            })
                        }
                    }
                }
                if (sexError != null) {
                    Text(sexError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = breed,
                onValueChange = {
                    breed = it
                    breedError = validateBreed(it)
                },
                label = { Text("Breed *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = breedError != null,
                shape = RoundedCornerShape(12.dp)
            )
            if (breedError != null) {
                Text(breedError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = dob,
                onValueChange = {
                    dob = it
                    dobError = validateDob(it)
                },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = dobError != null,
                shape = RoundedCornerShape(12.dp)
            )
            if (dobError != null) {
                Text(dobError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        weight = it
                        weightError = validateWeight(it)
                    },
                    label = { Text("Weight *") },
                    placeholder = { Text("10") },
                    trailingIcon = { Text("kg", color = Color(0xFF6B7280)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = weightError != null,
                    shape = RoundedCornerShape(12.dp)
                )
                if (weightError != null) {
                    Text(weightError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                }

                OutlinedTextField(
                    value = color,
                    onValueChange = {
                        color = it
                        colorError = validateColor(it)
                    },
                    label = { Text("Color *") },
                    placeholder = { Text("Pet color") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = colorError != null,
                    shape = RoundedCornerShape(12.dp)
                )
                if (colorError != null) {
                    Text(colorError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// naive JSON string escaper for simple demo
private fun jsonQ(v: String): String = buildString {
    append('"')
    v.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}
