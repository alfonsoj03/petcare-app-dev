package com.petcare.mascotasapp.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.petcare.mascotasapp.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.google.firebase.auth.FirebaseAuth
import com.petcare.mascotasapp.core.SelectedPetStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit = {}, onSignOut: () -> Unit = {}) {
    val bgSurface = Color(0xFFF9FAFB)
    val accent = Color(0xFF8B5CF6)
    val green = Color(0xFF10B981)
    val user = FirebaseAuth.getInstance().currentUser
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
    val nameSurname = displayName?.let {
        val parts = it.trim().split(" ").filter { p -> p.isNotBlank() }
        when {
            parts.size >= 2 -> parts[0] + " " + parts[1]
            else -> it
        }
    } ?: "Guest"
    val emailText = user?.email ?: ""

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(green),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications, // just a small badge icon
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        containerColor = bgSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Image(
                painter = painterResource(id = R.drawable.mascota),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
            )
            Text(nameSurname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(emailText, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            Button(
                onClick = { /* edit profile */ },
                colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) { Text("Edit Profile") }

            // Cards list
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    title = "Privacy & Security",
                    subtitle = "Control your data",
                    icon = Icons.Filled.Lock,
                    bgColor = Color(0xFFDCFCE7), // soft green
                    iconTint = Color(0xFF16A34A)
                )
                SettingRow(
                    title = "About",
                    subtitle = "App info & support",
                    icon = Icons.Filled.Info,
                    bgColor = Color(0xFFDBEAFE), // soft blue
                    iconTint = Color(0xFF3B82F6)
                )
            }

            // Sign out
            OutlinedButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    SelectedPetStore.clear()
                    onSignOut()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE5E7EB)))
            ) { Text("Sign Out") }

            Spacer(Modifier.height(8.dp))
            Text("Version 2.1.0", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconTint: Color,
    showDot: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = bgColor, shape = CircleShape) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
            }
        }
    }
}
