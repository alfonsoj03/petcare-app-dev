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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mascotasapp.ui.theme.MascotasAppTheme
import com.example.mascotasapp.ui.navigation.Destinations
import com.example.mascotasapp.ui.screens.splash.SplashScreen
import com.example.mascotasapp.ui.screens.dashboard.DashboardScreen
import com.example.mascotasapp.ui.screens.health.HealthScreen
import com.example.mascotasapp.ui.screens.health.AddVisitScreen
import com.example.mascotasapp.ui.screens.health.RescheduleScreen
import com.example.mascotasapp.ui.screens.health.VisitDetailsScreen
import com.example.mascotasapp.ui.screens.health.EditVisitScreen
import com.example.mascotasapp.ui.screens.profile.ProfileScreen
import com.example.mascotasapp.ui.screens.routine.RoutineScreen
import com.example.mascotasapp.ui.screens.pets.PetsScreen
import com.example.mascotasapp.ui.screens.pets.AddPetScreen
import com.example.mascotasapp.ui.screens.pets.EditPetScreen
import com.example.mascotasapp.ui.screens.login.LoginScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mascotasapp.R
// removed ripple/clickable customizations
import androidx.compose.foundation.layout.size

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
    var selectedPetId by remember { mutableStateOf<String?>(null) }
    var selectedPetImageRes by remember { mutableStateOf(R.drawable.foto_stock_perrito) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val hideBottomBarRoutes = setOf(
                Destinations.Login.route,
                Destinations.Splash.route,
                Destinations.AddVisit.route,
                Destinations.Reschedule.route,
                Destinations.VisitDetails.route,
                Destinations.EditVisit.route
            )
            if (currentRoute !in hideBottomBarRoutes) {
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
                                icon = {
                                    if (dest == Destinations.Pets) {
                                        Image(
                                            painter = painterResource(id = selectedPetImageRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        androidx.compose.material3.Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
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
            startDestination = Destinations.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.Splash.route) {
                SplashScreen(onFinished = {
                    navController.navigate(Destinations.Login.route) {
                        popUpTo(Destinations.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
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
            composable(Destinations.Dashboard.route) {
                DashboardScreen(
                    onOpenProfile = { navController.navigate(Destinations.Profile.route) }
                )
            }
            composable(Destinations.Health.route) {
                HealthScreen(
                    onAddExtraVisit = { navController.navigate(Destinations.AddVisit.route) },
                    onReschedule = { navController.navigate(Destinations.Reschedule.route) },
                    onViewDetails = { navController.navigate(Destinations.VisitDetails.route) }
                )
            }
            composable(Destinations.Pets.route) {
                PetsScreen(
                    onAddPet = { navController.navigate(Destinations.AddPet.route) },
                    onOpenPet = { id -> navController.navigate(Destinations.EditPet.routeFor(id)) },
                    selectedPetId = selectedPetId,
                    onSelectedPet = { pet ->
                        selectedPetId = pet.id
                        selectedPetImageRes = pet.imageRes
                    }
                )
            }
            composable(Destinations.AddPet.route) { AddPetScreen(onBack = { navController.navigateUp() }) }
            composable(Destinations.EditPet.route) { backStack ->
                val petId = backStack.arguments?.getString(Destinations.EditPet.ArgPetId) ?: ""
                EditPetScreen(petId, onBack = { navController.navigateUp() })
            }
            composable(Destinations.Routine.route) { RoutineScreen() }
            composable(Destinations.Profile.route) { ProfileScreen(onBack = { navController.navigateUp() }) }
            composable(Destinations.AddVisit.route) {
                AddVisitScreen(
                    onBack = { navController.navigateUp() },
                    onSave = { _,_,_,_,_,_ -> navController.navigateUp() },
                    onCancel = { navController.navigateUp() }
                )
            }
            composable(Destinations.Reschedule.route) {
                RescheduleScreen(
                    onBack = { navController.navigateUp() },
                    onSave = { _,_,_,_,_,_ -> navController.navigateUp() }
                )
            }
            composable(Destinations.VisitDetails.route) {
                VisitDetailsScreen(
                    onBack = { navController.navigateUp() },
                    onEdit = { navController.navigate(Destinations.EditVisit.route) }
                )
            }
            composable(Destinations.EditVisit.route) {
                EditVisitScreen(
                    onBack = { navController.navigateUp() },
                    onSave = { _,_,_,_,_,_ -> navController.navigateUp() }
                )
            }
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