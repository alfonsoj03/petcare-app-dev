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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun AddPetScreen(onBack: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var speciesExpanded by remember { mutableStateOf(false) }
    val speciesOptions = listOf("Dog", "Cat", "Other")
    var species by remember { mutableStateOf("") }

    var sexExpanded by remember { mutableStateOf(false) }
    val sexOptions = listOf("Male", "Female")
    var sex by remember { mutableStateOf("") }

    var breed by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

    val ctx = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    val baseUrl = "http://10.0.2.2:5001/petcare-ac3c2/us-central1" // Emulator Functions base (host loopback for Android emulator)

    Scaffold(
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
                onValueChange = { name = it },
                label = { Text("Pet Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(expanded = speciesExpanded, onExpandedChange = { speciesExpanded = !speciesExpanded }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = species.ifBlank { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Species *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = speciesExpanded, onDismissRequest = { speciesExpanded = false }) {
                        speciesOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                species = option
                                speciesExpanded = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = sexExpanded, onExpandedChange = { sexExpanded = !sexExpanded }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = sex.ifBlank { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sex *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                        sexOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                sex = option
                                sexExpanded = false
                            })
                        }
                    }
                }
            }

            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Breed *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth") },
                trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight *") },
                    placeholder = { Text("0.0") },
                    trailingIcon = { Text("kg", color = Color(0xFF6B7280)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color") },
                    placeholder = { Text("Pet color") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            ElevatedButton(
                onClick = {
                    // Simple validation min required
                    if (name.isBlank() || species.isBlank() || sex.isBlank()) return@ElevatedButton
                    scope.launch {
                        try {
                            val url = URL("$baseUrl/createPet")
                            val conn = (url.openConnection() as HttpURLConnection).apply {
                                requestMethod = "POST"
                                setRequestProperty("Content-Type", "application/json")
                                // Bypass auth for emulator
                                setRequestProperty("X-Debug-Uid", "demo-user")
                                doOutput = true
                                connectTimeout = 8000
                                readTimeout = 8000
                            }
                            val payload = """
                                {
                                  "name": ${jsonQ(name)},
                                  "species": ${jsonQ(species)},
                                  "sex": ${jsonQ(sex)},
                                  "breed": ${jsonQ(breed)},
                                  "dob": ${jsonQ(dob)},
                                  "weight": ${jsonQ(weight)},
                                  "color": ${jsonQ(color)},
                                  "imageUrl": ""
                                }
                            """.trimIndent()
                            BufferedWriter(OutputStreamWriter(conn.outputStream)).use { it.write(payload) }
                            val code = conn.responseCode
                            if (code in 200..299) {
                                // Go back on success
                                // Switch to main thread to call callback
                                kotlinx.coroutines.withContext(Dispatchers.Main) { onBack() }
                            } else {
                                val err = runCatching {
                                    BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() }
                                }.getOrNull()
                                android.util.Log.e("AddPet", "Failed ($code): $err")
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            android.util.Log.e("AddPet", "Error", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White)
            ) { Text("+ Add Pet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium) }
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
