package com.example.mascotasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mascotasapp.ui.theme.MascotasAppTheme
import com.example.mascotasapp.ui.navigation.Destinations
import com.example.mascotasapp.ui.screens.dashboard.DashboardScreen
import com.example.mascotasapp.ui.screens.health.HealthScreen
import com.example.mascotasapp.ui.screens.routine.RoutineScreen
import com.example.mascotasapp.ui.screens.pets.PetsScreen
import com.example.mascotasapp.ui.screens.pets.AddPetScreen
import com.example.mascotasapp.ui.screens.pets.EditPetScreen
import com.example.mascotasapp.ui.screens.login.LoginScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MascotasAppTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val items = Destinations.bottomItems
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != Destinations.Login.route) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .background(Color.White)
                ) {
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        items.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    if (currentRoute != dest.route) {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { androidx.compose.material3.Icon(dest.icon, contentDescription = dest.label) },
                                label = { androidx.compose.material3.Text(dest.label, style = MaterialTheme.typography.labelLarge) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = Color(0xFF9CA3AF),
                                    unselectedTextColor = Color(0xFF9CA3AF),
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.Login.route) {
                LoginScreen(
                    onSignIn = {
                        navController.navigate(Destinations.Dashboard.route) {
                            popUpTo(Destinations.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Destinations.Dashboard.route) { DashboardScreen() }
            composable(Destinations.Health.route) { HealthScreen() }
            composable(Destinations.Pets.route) {
                PetsScreen(
                    onAddPet = { navController.navigate(Destinations.AddPet.route) },
                    onOpenPet = { id -> navController.navigate(Destinations.EditPet.routeFor(id)) }
                )
            }
            composable(Destinations.AddPet.route) { AddPetScreen(onBack = { navController.navigateUp() }) }
            composable(Destinations.EditPet.route) { backStack ->
                val petId = backStack.arguments?.getString(Destinations.EditPet.ArgPetId) ?: ""
                EditPetScreen(petId, onBack = { navController.navigateUp() })
            }
            composable(Destinations.Routine.route) { RoutineScreen() }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MascotasAppTheme {
        AppRoot()
    }
}