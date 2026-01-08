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


class HomeScreen {



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun display(username: String, isRegistered: Boolean, currentDeviceName: String?, currentUuid: String?, loadingState: Boolean, errorMessage: String?, deviceList: List<MainActivity.Device>, isRefreshing: Boolean, foregroundServiceIsRunning: Boolean, onOpenReplaceOld: () -> Unit, registerDevice: () -> Unit, logout: (delete: Boolean) -> Unit, renameDevice: (name: String, success: () -> Unit) -> Unit, onRefresh: () -> Unit, startForegroundService: () -> Unit, openWebsite: () -> Unit) {
        val green = Color(0xFF7CDE89)
        val teal = Color(0xFF28B0A5)
        val blue = Color(0xFF3E73B8)

        var registerDeviceProgress by remember { mutableStateOf("Generiere Zertifikate") }

        var newNameInput by remember { mutableStateOf("") }
        var renamePopup by remember { mutableStateOf(false) }
        var logoutPopup by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
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
                                color = Color(0xFF0F1A1A).copy(alpha = 0.85f),
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
                                color = Color(0xFF0F1A1A).copy(alpha = 0.85f),
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
                                color = Color(0xFF0F1A1A).copy(alpha = 0.85f),
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
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                }
            }
        }

        if (renamePopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { renamePopup = false }
                    .padding(24.dp),

                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF0F1A1A), RoundedCornerShape(20.dp))
                        .padding(40.dp)
                ) {
                    Text(
                        text = "Vergib einen neuen Namen",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Neuer Name", color = Color.LightGray) },
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

                    Button(
                        onClick = {
                            renameDevice(newNameInput) {
                                renamePopup = false
                            }
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
                                    Color.Gray,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Bestätigen",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        if (logoutPopup) {
            if (isRegistered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { logoutPopup = false }
                        .padding(24.dp),

                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF0F1A1A), RoundedCornerShape(20.dp))
                            .padding(40.dp)
                    ) {
                        Text(
                            text = "Möchtest du dich wirklich abmelden?",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Wenn du dich abmeldest, wird die Verknüpfung zu diesem Gerät aufgehoben. Du kannst dein Gerät entweder löschen, oder inaktiv setzen und später neu verknüpfen.",
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(20.dp))


                        Button(
                            onClick = {
                                logout(false)
                            },
                            enabled = !loadingState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
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
                                    text = "Abmelden und als\ninaktiv markieren",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                logout(true)
                            },
                            enabled = !loadingState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
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
                                    text = "Abmelden und löschen",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { logoutPopup = false }
                        .padding(24.dp),

                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF0F1A1A), RoundedCornerShape(20.dp))
                            .padding(40.dp)
                    ) {
                        Text(
                            text = "Möchtest du dich wirklich abmelden?",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Spacer(modifier = Modifier.height(20.dp))


                        Button(
                            onClick = {
                                logout(false)
                            },
                            enabled = !loadingState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
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
                                    text = "Jetzt abmelden",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

        }


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
                        text = registerDeviceProgress,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }


    }

}