package com.example.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Typography

val CareBlue = Color(0xFF155EEF)
val CareGreen = Color(0xFF008A4B)
val CareRed = Color(0xFFE52B38)
val CareYellow = Color(0xFFFFD43B)
val CareInk = Color(0xFF111111)

@Composable
fun CareTheme(content: @Composable () -> Unit) {
    MaterialTheme(typography = Typography, colorScheme = lightColorScheme(
        primary = CareBlue, onPrimary = Color.White, secondary = CareGreen,
        background = Color.White, surface = Color.White, onSurface = CareInk,
        onBackground = CareInk, surfaceVariant = Color(0xFFF3F4F6), onSurfaceVariant = CareInk,
        outline = Color(0xFFDDE1E7), error = CareRed
    ), content = content)
}

@Composable
fun GlassNavigation(tab: Int, labels: List<String>, icons: List<ImageVector>, onSelect: (Int) -> Unit) {
    Row(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)
        .fillMaxWidth().shadow(18.dp, RoundedCornerShape(32.dp))
        .clip(RoundedCornerShape(32.dp))
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .96f), Color(0xFFEFF3FA).copy(alpha = .94f))))
        .border(1.dp, Color.White, RoundedCornerShape(32.dp)).selectableGroup().padding(7.dp),
        horizontalArrangement = Arrangement.SpaceAround) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(26.dp))
                .background(if (index == tab) CareInk else Color.Transparent)
                .selectable(index == tab, role = Role.Tab, onClick = { onSelect(index) })
                .padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icons[index], contentDescription = label, tint = if (index == tab) Color.White else CareInk, modifier = Modifier.size(25.dp))
                if (index == tab) Text(label, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
