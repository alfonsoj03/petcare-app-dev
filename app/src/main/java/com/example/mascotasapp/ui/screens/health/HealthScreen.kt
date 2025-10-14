package com.example.mascotasapp.ui.screens.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HealthScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Health", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Vaccines", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(text = "• Rabies — last: Oct 01 — next: Oct 29")
        Text(text = "• Distemper — last: Aug 03 — next: Feb 03")
        Spacer(Modifier.height(12.dp))
        Text(text = "Medications", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(text = "• Omega 3 — 1x daily — ongoing")
        Spacer(Modifier.height(12.dp))
        Text(text = "Vet Visits", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(text = "• Next visit: Oct 28, 10:00 AM")
    }
}
