package com.jannikklein47.batterysync

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@Composable
fun LogoutDialog(
    isRegistered: Boolean,
    loading: Boolean,
    onDismiss: () -> Unit,
    onLogout: (delete: Boolean) -> Unit
) {
    BatterySyncDialog(onDismissRequest = onDismiss) {
        // --- Title ---
        Text(
            text = "Möchtest du dich wirklich abmelden?",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Body Content (Conditional) ---
        if (isRegistered) {
            Text(
                text = "Wenn du dich abmeldest, wird die Verknüpfung zu diesem Gerät aufgehoben. Du kannst dein Gerät entweder löschen oder inaktiv setzen.",
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Action Buttons ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Primary Logout Button
            Button(
                onClick = { onLogout(false) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(
                    text = if (isRegistered) "Abmelden und als\ninaktiv markieren" else "Jetzt abmelden",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Secondary "Delete" Button (Only if registered)
            if (isRegistered) {
                Button(
                    onClick = { onLogout(true) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(
                        text = "Abmelden und löschen",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cancel / Dismiss
            TextButton(
                onClick = onDismiss,
                enabled = !loading,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Zurück", color = Color.LightGray)
            }
        }
    }
}