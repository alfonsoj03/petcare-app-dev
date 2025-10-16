package com.example.mascotasapp.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun RescheduleScreen(
    onBack: () -> Unit = {},
    onSave: (visitType: String, date: String, time: String, clinic: String, vet: String, reason: String) -> Unit = { _,_,_,_,_,_ -> },
    initialVisitType: String = "Routine Checkup",
    initialDate: String = "2025-12-15",
    initialTime: String = "2:30",
    initialClinic: String = "Happy Paws Clinic",
    initialVet: String = "Dr. Michael Williams",
) {
    val brandPurple = Color(0xFF8B5CF6)
    val bgSurface = Color(0xFFF9FAFB)
    val green = Color(0xFF10B981)

    var visitTypeExpanded by remember { mutableStateOf(false) }
    val visitTypes = listOf("Routine Checkup", "Vaccine", "Emergency", "Dental", "Other")
    var visitType by remember { mutableStateOf(initialVisitType) }

    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    var clinic by remember { mutableStateOf(TextFieldValue(initialClinic)) }
    var vet by remember { mutableStateOf(TextFieldValue(initialVet)) }
    var reason by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reschedule", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(green),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White, scrolledContainerColor = Color.White),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        containerColor = bgSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
            ) {
                Divider(color = Color(0xFFE5E7EB))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = { onSave(visitType, date, time, clinic.text, vet.text, reason.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save Changes") }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
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
                ExposedDropdownMenuBox(expanded = visitTypeExpanded, onExpandedChange = { visitTypeExpanded = !visitTypeExpanded }) {
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
                        )
                    )
                    ExposedDropdownMenu(expanded = visitTypeExpanded, onDismissRequest = { visitTypeExpanded = false }) {
                        visitTypes.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                visitType = it
                                visitTypeExpanded = false
                            })
                        }
                    }
                }

                Text("Date *", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    trailingIcon = { Icon(Icons.Filled.Today, contentDescription = null, tint = Color(0xFF111827)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text("Time *", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF111827)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text("Suggested times:", style = MaterialTheme.typography.labelLarge, color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val suggestionColors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFF3F4F6),
                        labelColor = Color(0xFF111827)
                    )
                    listOf("09:30", "10:00", "10:30").forEach { t ->
                        AssistChip(onClick = { time = t }, label = { Text(t) }, colors = suggestionColors)
                    }
                }

                Text("Clinic / Location", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = clinic,
                    onValueChange = { clinic = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text("Veterinarian", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = vet,
                    onValueChange = { vet = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text("Reason for reschedule", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111827))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD1D5DB),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }
    }
}
