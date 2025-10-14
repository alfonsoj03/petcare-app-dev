@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mascotasapp.ui.screens.pets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mascotasapp.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.text.font.FontWeight

data class PetExtended(
    val id: String,
    val name: String,
    val breed: String,
    val species: String,
    val ageYears: Int,
    val upToDate: Boolean,
    val nextEvent: String
)

@Composable
fun PetsScreen(
    onAddPet: () -> Unit = {},
    onOpenPet: (String) -> Unit = {}
) {
    val sample = listOf(
        PetExtended("1", "Buddy", "Golden Retriever", "Canine", 2, true, "Next vaccine: Oct 20"),
        PetExtended("2", "Luna", "Mixed", "Canine", 4, false, "Next vet visit: Nov 02"),
    )
    val selectedId = sample.first().id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Pets", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    val sel = sample.firstOrNull { it.id == selectedId }
                    if (sel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(sel.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(sel.breed, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.width(6.dp))
                            Image(
                                painter = painterResource(id = R.drawable.foto_stock_perrito),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
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
            items(sample, key = { it.id }) { pet ->
                PetRowCard(
                    pet = pet,
                    selected = pet.id == selectedId,
                    onClick = { onOpenPet(pet.id) }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PetRowCard(
    pet: PetExtended,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.secondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.foto_stock_perrito),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${pet.breed} • ${pet.ageYears}y", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    StatusBadge(upToDate = pet.upToDate)
                }
                Spacer(Modifier.height(6.dp))
                Text(pet.nextEvent, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
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
