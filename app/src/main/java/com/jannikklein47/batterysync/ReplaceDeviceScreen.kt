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
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.plus

class ReplaceDeviceScreen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DeviceLinkScreen(context: Context, deviceList: List<MainActivity.Device>, loadingState: Boolean, isRefreshing: Boolean, creatableOtp: Boolean?, otpInputWrong: Boolean?, onRefresh: () -> Unit, onSendCode: (device: MainActivity.Device) -> Unit, onConfirmInputCode: (code: String, device: MainActivity.Device) -> Unit, onLinkNow: (device: MainActivity.Device) -> Unit, onSelectDevice: (device: MainActivity.Device) -> Unit) {
        // Variables (you handle functionality later)
        val green = Color(0xFF7CDE89)
        val teal = Color(0xFF28B0A5)
        val blue = Color(0xFF3E73B8)

        var selectedDevice by remember { mutableStateOf<MainActivity.Device?>(null) }
        var showCodeField by remember { mutableStateOf(false) }
        var codeInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gerät verknüpfen",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Verknüpfe dein neues Gerät mit einem Alten. Halte dazu dein altes Gerät bereit - ein OTP (One-Time Password) wird möglicherweise benötigt. Falls es nicht mehr funktioniert, kannst du es spätestens nach 12 Stunden ersetzen.",
                    color = Color.LightGray,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Device Selection List

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,

                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        deviceList.forEach { device ->
                            val isSelected = device.id == selectedDevice?.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFF1A2A2A) else Color(0xFF0F1A1A),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        if (!loadingState) {
                                            selectedDevice = device
                                            onSelectDevice(device)
                                        }
                                    }
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = device.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = if (device.requiresOtp) "OTP" else "Frei",
                                        color = if (device.requiresOtp) Color.Red else green,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                // Selected Device Details
            }

            selectedDevice?.let { device ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(12.dp)
                        .clickable {
                            selectedDevice = null
                            codeInput = ""
                            showCodeField = false
                        },

                    contentAlignment = Alignment.Center

                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF0F1A1A), RoundedCornerShape(20.dp))
                            .padding(40.dp)
                        ) {
                        Text(
                            text = device.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (device.requiresOtp) {
                            if (creatableOtp == null)
                                Text(
                                    text = "Hole Informationen...",
                                    color = Color.White
                                )
                            else if (!showCodeField && creatableOtp) {
                                Text(
                                    text = "Um dieses Gerät zu verknüpfen, benötigst du ein OTP (One-Time Password). Bei erfolgreicher verknüpfung wird die Synchronisierung deines alten Geräts beendet.",
                                    color = Color.LightGray,
                                    fontSize = 15.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                GradientButton(text = "Code senden", green, teal, blue, !loadingState) {
                                    showCodeField = true
                                    onSendCode(device)
                                }
                            } else if (creatableOtp == true) {
                                if (otpInputWrong == true) {
                                    Text(
                                        text = "Deine Eingabe ist falsch.",
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { codeInput = it },
                                    label = { Text("Code", color = Color.LightGray) },
                                    singleLine = true,
                                    enabled =  !loadingState,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = blue,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                GradientButton(text = "Bestätigen", green, teal, blue, !loadingState) {
                                    onConfirmInputCode(codeInput, device)
                                }
                            } else {
                                Text(
                                    text = "Ein Einmalpasswort wurde bereits erstellt. Gib das Aktuelle ein oder warte max. 5 Minuten und generiere ein Neues.",
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (otpInputWrong == true) {
                                    Text(
                                        text = "Deine Eingabe ist falsch.",
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { codeInput = it },
                                    label = { Text("Code", color = Color.LightGray) },
                                    singleLine = true,
                                    enabled =  !loadingState,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = blue,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                GradientButton(text = "Bestätigen", green, teal, blue, !loadingState) {
                                    onConfirmInputCode(codeInput, device)
                                }
                            }
                        } else {
                            Text(
                                text = "Dieses Gerät erfordert für die Verknüpfung kein OTP (One-Time Password). Du kannst es direkt verknüpfen - dein Altes Gerät wird seinen Akku nicht mehr synchronisieren.",
                                color = Color.LightGray,
                                fontSize = 15.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            GradientButton(text = "Jetzt verknüpfen", green, teal, blue, !loadingState) {
                                onLinkNow(device)
                            }
                        }
                    }
                }
            }

            // Popup with spinner
            if (loadingState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF0F1A1A), RoundedCornerShape(20.dp))
                            .padding(40.dp)
                    ) {
                        CircularProgressIndicator(color = green)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Warte...",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }

        }
    }

    @Composable
    fun GradientButton(text: String, green: Color, teal: Color, blue: Color, enabled: Boolean, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(green, teal, blue)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}