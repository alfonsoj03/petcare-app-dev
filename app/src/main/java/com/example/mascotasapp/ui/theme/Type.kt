@file:OptIn(ExperimentalTextApi::class)
package com.example.mascotasapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mascotasapp.R

// Variable and local font families (TTF files must exist under res/font)
// Nunito Variable (wght)
private val NunitoVF = FontFamily(
    Font(
        resId = R.font.nunito_variable,
        // Default axis value; per-style weight will further influence rendering
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 600f)
        )
    )
)

// Inter Variable (wght, optional slnt)
private val InterVF = FontFamily(
    Font(
        resId = R.font.inter_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f)
            // You may enable slant globally if desired:
            // , FontVariation.Setting("slnt", 0f)
        )
    )
)

// Itim static
private val Itim = FontFamily(
    Font(resId = R.font.itim_regular, weight = FontWeight.Normal)
)

// Material3 Typography mapping per requirements
val Typography = Typography(
    // Titles: Nunito
    displayLarge = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = NunitoVF, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

    // General text (top app bar, subtitles, body): Inter
    bodyLarge = TextStyle(fontFamily = InterVF, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InterVF, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterVF, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Itim, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp), // Bottom menu
    labelMedium = TextStyle(fontFamily = InterVF, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = InterVF, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)