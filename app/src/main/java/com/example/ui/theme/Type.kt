package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val Inter = FontFamily(Font(R.font.inter))
private fun style(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.35).sp
)
val Typography = Typography(
    displayLarge = style(48, FontWeight.Bold), displayMedium = style(40, FontWeight.Bold), displaySmall = style(36, FontWeight.Bold),
    headlineLarge = style(32, FontWeight.Bold), headlineMedium = style(28, FontWeight.Bold), headlineSmall = style(24, FontWeight.Bold),
    titleLarge = style(22, FontWeight.SemiBold), titleMedium = style(20, FontWeight.SemiBold), titleSmall = style(18, FontWeight.SemiBold),
    bodyLarge = style(18), bodyMedium = style(17), bodySmall = style(15),
    labelLarge = style(16, FontWeight.SemiBold), labelMedium = style(14, FontWeight.Medium), labelSmall = style(13, FontWeight.Medium)
)
