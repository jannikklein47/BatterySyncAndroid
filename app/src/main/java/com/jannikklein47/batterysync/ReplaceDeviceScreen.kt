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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    fun DeviceLinkScreen(
        context: Context,
        deviceList: List<MainActivity.Device>,
        loadingState: Boolean,
        isRefreshing: Boolean,
        creatableOtp: Boolean?,
        otpInputWrong: Boolean?,
        onRefresh: () -> Unit,
        onSendCode: (device: MainActivity.Device) -> Unit,
        onConfirmInputCode: (code: String, device: MainActivity.Device) -> Unit,
        onLinkNow: (device: MainActivity.Device) -> Unit,
        onSelectDevice: (device: MainActivity.Device) -> Unit
    ) {
        val green = Color(0xFF7CDE89)
        val teal = Color(0xFF28B0A5)
        val blue = Color(0xFF3E73B8)

        var selectedDevice by remember { mutableStateOf<MainActivity.Device?>(null) }
        var codeInput by remember { mutableStateOf("") }
        var showCodeField by remember { mutableStateOf(false) }

        // Determine if we are blurring the background
        val isPopupActive = selectedDevice != null || loadingState

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // --- MAIN CONTENT ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .blur(if (isPopupActive) 5.dp else 0.dp) // Professional Blur
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gerät verknüpfen",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Verknüpfe dein neues Gerät mit einem Alten. Halte dazu dein altes Gerät bereit...",
                    color = Color.LightGray,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(32.dp))

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
                            DeviceRow(
                                device = device,
                                isSelected = device.id == selectedDevice?.id,
                                green = green,
                                onClick = {
                                    if (!loadingState) {
                                        selectedDevice = device
                                        onSelectDevice(device)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // --- POPUPS (DIALOGS) ---

            // Device Interaction Dialog
            selectedDevice?.let { device ->
                DeviceActionDialog(
                    device = device,
                    loading = loadingState,
                    creatableOtp = creatableOtp,
                    otpInputWrong = otpInputWrong,
                    codeInput = codeInput,
                    showCodeField = showCodeField,
                    onCodeChange = { codeInput = it },
                    onDismiss = {
                        selectedDevice = null
                        codeInput = ""
                        showCodeField = false
                    },
                    onSendCode = {
                        showCodeField = true
                        onSendCode(device)
                    },
                    onConfirmCode = { onConfirmInputCode(codeInput, device) },
                    onLinkNow = { onLinkNow(device) }
                )
            }

            // Loading Overlay
            if (loadingState) {
                LoadingDialog(message = "Warte...")
            }
        }
    }

    @Composable
    private fun DeviceActionDialog(
        device: MainActivity.Device,
        loading: Boolean,
        creatableOtp: Boolean?,
        otpInputWrong: Boolean?,
        codeInput: String,
        showCodeField: Boolean,
        onCodeChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSendCode: () -> Unit,
        onConfirmCode: () -> Unit,
        onLinkNow: () -> Unit
    ) {
        val blue = Color(0xFF3E73B8)

        // Using your refined Dialog wrapper
        BatterySyncDialog(onDismissRequest = onDismiss) {
            Text(
                text = device.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (device.requiresOtp) {
                OTPFlowContent(
                    loading = loading,
                    creatableOtp = creatableOtp,
                    otpInputWrong = otpInputWrong,
                    codeInput = codeInput,
                    showCodeField = showCodeField,
                    onCodeChange = onCodeChange,
                    onSendCode = onSendCode,
                    onConfirmCode = onConfirmCode
                )
            } else {
                Text(
                    text = "Dieses Gerät erfordert kein OTP. Du kannst es direkt verknüpfen.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                GradientButton(text = "Jetzt verknüpfen", enabled = !loading, onClick = onLinkNow)
            }

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Text("Abbrechen", color = Color.Gray)
            }
        }
    }

    @Composable
    private fun OTPFlowContent(
        loading: Boolean,
        creatableOtp: Boolean?,
        otpInputWrong: Boolean?,
        codeInput: String,
        showCodeField: Boolean,
        onCodeChange: (String) -> Unit,
        onSendCode: () -> Unit,
        onConfirmCode: () -> Unit
    ) {
        val blue = Color(0xFF3E73B8)

        when {
            creatableOtp == null -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            !showCodeField && creatableOtp -> {
                Text("Du benötigst ein OTP zur Verknüpfung.", color = Color.LightGray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                GradientButton(text = "Code senden", enabled = !loading, onClick = onSendCode)
            }
            else -> {
                if (otpInputWrong == true) {
                    Text("Code ist falsch.", color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = onCodeChange,
                    label = { Text("6-stelliger Code") },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.LightGray

                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                GradientButton(text = "Bestätigen", enabled = !loading, onClick = onConfirmCode)
            }
        }
    }

    @Composable
    private fun DeviceRow(device: MainActivity.Device, isSelected: Boolean, green: Color, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) Color(0xFF1A2A2A) else Color(0xFF0F1A1A),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(device.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = if (device.requiresOtp) "OTP" else "Frei",
                    color = if (device.requiresOtp) Color.Red else green,
                    fontSize = 14.sp
                )
            }
        }
    }

    @Composable
    fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit) {
        val green = Color(0xFF7CDE89)
        val teal = Color(0xFF28B0A5)
        val blue = Color(0xFF3E73B8)

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(green, teal, blue))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}