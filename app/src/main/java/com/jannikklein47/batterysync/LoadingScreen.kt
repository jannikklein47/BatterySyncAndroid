package com.jannikklein47.batterysync

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    statusText: String = "Warte auf Netzwerk..."
) {
    val green = Color(0xFF7CDEB4)
    val teal = Color(0xFF28B0A5)
    val blue = Color(0xFF3E73B8)
    val BackgroundDark = Color(0xFF0F1318)

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            blue,  // blue
            teal, // teal
            green, // green
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App-Titel / Branding (pulsiert sanft)
            GradientText(
                text = "BatterySync",
                gradient = gradient,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Der Ladeindikator
            CircularProgressIndicator(
                color = BlueProgress,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamischer Status-Text darunter
            Text(
                text = statusText,
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GradientText(
    text: String,
    gradient: Brush,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = LocalTextStyle.current.copy(
            brush = gradient
        )
    )
}