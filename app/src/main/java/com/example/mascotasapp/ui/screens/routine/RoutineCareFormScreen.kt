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

    val units = listOf("minutes", "hours", "days", "weeks")
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var alsoBuddy by remember { mutableStateOf(initialAlsoBuddy) }
    var alsoLuna by remember { mutableStateOf(initialAlsoLuna) }

    Scaffold(
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
                ElevatedButton(
                    onClick = { onConfirm(careName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = purple, contentColor = Color.White)
                ) {
                    Text(confirmButtonText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { LabeledField(label = "Care Name *") {
                OutlinedTextField(
                    value = careName,
                    onValueChange = { careName = it },
                    placeholder = { Text("Enter care name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }}
            item { LabeledField(label = "First or latest performed execution *") {
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    placeholder = { Text("mm/dd/yyyy, 10:00am") },
                    trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }}
            item { LabeledField(label = "Perform every *") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = everyValue,
                        onValueChange = { everyValue = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("0") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
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
                            modifier = Modifier.menuAnchor().weight(1f)
                        )
                        ExposedDropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            units.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    everyUnit = option
                                    unitMenuExpanded = false
                                })
                            }
                        }
                    }
                }
            }}
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

@Composable
private fun LabeledField(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        content()
    }
}
