package com.example.mascotasapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destinations(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Destinations("dashboard", "Dashboard", Icons.Filled.Home)
    data object Health : Destinations("health", "Health", Icons.Filled.Favorite)
    data object Routine : Destinations("routine", "Routine", Icons.Filled.Schedule)

    companion object {
        val bottomItems = listOf(Dashboard, Health, Routine)
    }
}
