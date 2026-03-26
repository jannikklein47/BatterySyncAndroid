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
import androidx.compose.ui.draw.blur
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


class HomeScreen {



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun display(username: String, isRegistered: Boolean, currentDeviceName: String?, currentUuid: String?, loadingState: Boolean, errorMessage: String?, deviceList: List<MainActivity.Device>, isRefreshing: Boolean, foregroundServiceIsRunning: Boolean, onOpenReplaceOld: () -> Unit, registerDevice: () -> Unit, logout: (delete: Boolean) -> Unit, renameDevice: (name: String, success: () -> Unit) -> Unit, onRefresh: () -> Unit, startForegroundService: () -> Unit, openWebsite: () -> Unit, openDevice: (id: Int) -> Unit) {
        val green = Color(0xFF7CDE89)
        val teal = Color(0xFF28B0A5)
        val blue = Color(0xFF3E73B8)

        var registerDeviceProgress by remember { mutableStateOf("Generiere Zertifikate") }

        var newNameInput by remember { mutableStateOf("") }
        var renamePopup by remember { mutableStateOf(false) }
        var logoutPopup by remember { mutableStateOf(false) }

        // Define if ANY popup is showing
        val isPopupVisible = renamePopup || logoutPopup || loadingState

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .blur(if (isPopupVisible) 5.dp else 0.dp)
        ) {

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(50.dp))

                    Text(
                        text = "Hallo, $username",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(50.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = Color(0xF01C1C1E).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!errorMessage.isNullOrEmpty()) {
                                Text(
                                    text= "$errorMessage",
                                    color = Color.Red,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                            }

                            if (!isRegistered) {
                                Text(
                                    text = "Dieses Gerät wurde noch nicht registriert.",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                Button(
                                    onClick = {
                                        registerDevice()

                                    },
                                    enabled = !loadingState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(listOf(green, teal, blue)),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Als neu registrieren",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onOpenReplaceOld()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    enabled = !loadingState,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(listOf(green, teal, blue)),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Altes Gerät ersetzen",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Gerät ist registriert als:",
                                    color = Color.Gray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = "$currentDeviceName",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(15.dp))
                                Button(
                                    onClick = {
                                        renamePopup = true
                                        newNameInput = if (!currentDeviceName.isNullOrEmpty()) currentDeviceName else ""
                                    },
                                    enabled = !loadingState && !renamePopup,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Color.Gray,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Benenne dein Gerät um",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = Color(0xF01C1C1E).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            Text(
                                text = "Meine Geräte",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            deviceList.forEach { device ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0x20FFFFFF),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(20.dp)
                                        .clickable {
                                            openDevice(device.id)
                                        }
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
                                            text = "${(device.battery * 100).toInt()}%",
                                            color = if (device.battery > 0.3 ) green
                                                    else if (device.battery > 0.15) Color.Yellow
                                                    else Color.Red,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                            }

                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle()) { append("Nutze unsere ") }
                                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) { append("Website") }
                                    withStyle(style = SpanStyle()) { append(" für weitere Einstellungen.") }
                                },
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    openWebsite()
                                }
                            )

                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = Color(0xF01C1C1E).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            Button(
                                onClick = {
                                    // Logout functionality --> reset all datastore values & move to login page

                                    logoutPopup = true
                                },
                                enabled = !loadingState && !renamePopup,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Color.Red,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Abmelden",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            if (!foregroundServiceIsRunning) {
                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        // Logout functionality --> reset all datastore values & move to login page

                                        startForegroundService()
                                    },
                                    enabled = !loadingState && !renamePopup,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(68.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Color.Gray,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Akkusynchronisierung\nstarten",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isRegistered) {
                        Spacer(modifier = Modifier.height(25.dp))
                        Text(
                            text = "Identifier ${if (!currentUuid.isNullOrEmpty()) currentUuid.toString().take(13) + "-****" else "_"}",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "App-Version: (Build) ${Globals().BUILD}",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                }
            }
        }

        if (renamePopup) {
            RenameDeviceDialog(
                initialName = newNameInput,
                onDismiss = { renamePopup = false },
                onConfirm = { name -> renameDevice(name) { renamePopup = false } },
                loading = loadingState
            )
        }

        if (logoutPopup) {
            LogoutDialog(
                isRegistered = isRegistered,
                loading = loadingState,
                onDismiss = { logoutPopup = false },
                onLogout = { delete ->
                    logout(delete)
                    logoutPopup = false // Close after action
                }
            )
        }

        if (loadingState) {
            LoadingDialog(message = registerDeviceProgress)
        }


    }

}