package com.example.mascotasapp.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    @DrawableRes topDecorationResId: Int? = null,
    @DrawableRes bottomDecorationResId: Int? = null
) {
    LaunchedEffect(Unit) {
        delay(1500)
        onFinished()
    }
    Surface(color = Color.White) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top decoration (nullable to avoid compile-time resource requirement)
            if (topDecorationResId != null) {
                Image(
                    painter = painterResource(id = topDecorationResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                )
            }

            // Bottom decoration (nullable)
            if (bottomDecorationResId != null) {
                Image(
                    painter = painterResource(id = bottomDecorationResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pets,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(text = "PetCare", color = Color.Black, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(text = "The best care for your pets!", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
