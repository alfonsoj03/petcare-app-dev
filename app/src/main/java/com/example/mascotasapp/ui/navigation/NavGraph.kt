package com.example.mascotasapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destinations(val route: String, val label: String, val icon: ImageVector) {
    data object Login : Destinations("login", "Login", Icons.Filled.Pets)
    data object Dashboard : Destinations("dashboard", "Dashboard", Icons.Filled.Home)
    data object Health : Destinations("health", "Health", Icons.Filled.Favorite)
    data object Routine : Destinations("routine", "Routine", Icons.Filled.Schedule)
    data object Pets : Destinations("pets", "Pets", Icons.Filled.Pets)
    data object AddPet : Destinations("pets/add", "Add Pet", Icons.Filled.Pets)
    data object EditPet : Destinations("pets/edit/{petId}", "Edit Pet", Icons.Filled.Pets) {
        const val ArgPetId = "petId"
        fun routeFor(petId: String) = "pets/edit/$petId"
    }

    companion object {
        val bottomItems = listOf(Dashboard, Health, Pets, Routine)
    }
}
