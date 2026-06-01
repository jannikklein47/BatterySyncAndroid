package com.jannikklein47.batterysync

import android.R
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
import androidx.compose.ui.res.painterResource
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

@Composable
fun SettingsScreen(initialShowPercent: Boolean, initialAlwaysFirst: Boolean, initialDarkMode: Boolean?, initialTransparency: Float, currentDeviceName: String?, registrationStatus: Boolean, offline: Boolean, finish: () -> Unit, saveAndFinish: (showPercent: Boolean, alwaysFirst: Boolean, darkMode: Boolean?, transparency: Float) -> Unit, changeDeviceName: (name: String, callback: (Boolean) -> Unit) -> Unit, onLogoutAndDeleteData: () -> Unit, onLogoutOnly: () -> Unit,) {
    val BackgroundDark = Color(0xFF0F1318)
    var showPercent by remember { mutableStateOf(initialShowPercent) }
    var alwaysFirst by remember { mutableStateOf(initialAlwaysFirst) }
    var darkMode by remember { mutableStateOf(initialDarkMode) }
    var transparency by remember { mutableFloatStateOf(initialTransparency) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        finish()
                    }
                ) {
                    Icon(
                        painter = painterResource(id= com.jannikklein47.batterysync.R.drawable.arrow_back),
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Einstellungen",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!currentDeviceName.isNullOrEmpty() && registrationStatus)
                ChangeDeviceNameSetting(
                    currentDeviceName,
                    offline,
                    changeDeviceName,
                    Modifier
                        .padding(bottom = 32.dp)
                )

            Text(text = "Widget-Einstellungen", color = Color.Gray)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPercent = !showPercent }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = showPercent,
                    onCheckedChange = { showPercent = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3B71CA), // Material Blue
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "%-Symbol nach Akkustand", color = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { alwaysFirst = !alwaysFirst }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = alwaysFirst,
                    onCheckedChange = { alwaysFirst = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3B71CA), // Material Blue
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Dieses Gerät immer als erstes zeigen", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "Widget-Design", color = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                TextButton(onClick = {darkMode = null}) {
                    Text("System", color = if (darkMode == null) Color.White else Color.Gray)
                }
                TextButton(onClick = {darkMode = false}) {
                    Text(text = "Hell", color = if (darkMode == false) Color.White else Color.Gray)
                }
                TextButton(onClick = {darkMode = true}) {
                    Text(text = "Dunkel", color = if (darkMode == true) Color.White else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "Widget-Transparenz: ${(transparency * 100).toInt()}%", color = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Slider(
                    value = transparency,
                    onValueChange = { value ->
                        transparency = value
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF3B71CA),
                        activeTrackColor = Color(0xFF3B71CA)
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TextButton(onClick = {transparency = 0.94f}) {
                    Text("Zurücksetzen", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Hinweis: Es kann eine Weile dauern, bis das Widget sich an die neuen Einstellungen anpasst, um Energie zu sparen.",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Konto-Einstellungen", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

            LogoutSetting(registrationStatus, offline, onLogoutAndDeleteData, onLogoutOnly)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // "Cancel" feel: Just finish without saving
                TextButton(onClick = { finish() }) {
                    Text("Zurück", color = Color(0xFF3B71CA))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B71CA)
                    ),
                    onClick = { saveAndFinish(showPercent, alwaysFirst, darkMode, transparency) }) {
                    Text("Speichern")
                }
            }

            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
            )

        }
    }
}

@Composable
fun ChangeDeviceNameSetting(
    currentDeviceName: String,
    offline: Boolean,
    onUpdateNameApiCall: (newName: String, callback: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var newNameText by remember { mutableStateOf(currentDeviceName) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    // Wenn der Dialog neu geöffnet wird, setzen wir den Text auf den aktuellen Namen zurück
    LaunchedEffect(showDialog) {
        if (showDialog) {
            newNameText = currentDeviceName
            isError = false
            isLoading = false
        }
    }

    Column(modifier = modifier) {
        Text(text = "Dieses Gerät heißt: $currentDeviceName", color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E222B), disabledContainerColor = Color(0x881E222B)), // Passend zum Dark Theme
            enabled = !offline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Gerätename ändern", color = if (offline) Color.Gray else Color.White)
        }
        if (offline) Text(text = "Zum Ändern des Gerätenamens ist eine Internetverbindung erforderlich.", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))

        // Das Popup-Menü (Dialog)
        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    // Verhindert das Schließen durch Tippen außerhalb, während das API lädt
                    if (!isLoading) showDialog = false
                },
                containerColor = Color(0xFF1E222B), // Dunkler Hintergrund
                title = {
                    Text(text = "Gerätename ändern", color = Color.White, fontSize = 20.sp)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newNameText,
                            onValueChange = {
                                if (!isLoading) {
                                    newNameText = it
                                    isError = false // Fehler zurücksetzen bei Neueingabe
                                }
                            },
                            label = { Text("Neuer Name") },
                            enabled = !isLoading,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B71CA),
                                unfocusedBorderColor = Color(0xFF8A939E),
                                focusedLabelColor = Color(0xFF3B71CA),
                                unfocusedLabelColor = Color(0xFF8A939E)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Fehlermeldung anzeigen, wenn das Backend "false" zurückgibt
                        if (isError) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Fehler beim Aktualisieren. Bitte erneut versuchen.",
                                color = Color(0xFFDC3545),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isLoading && newNameText.isNotBlank(),
                        onClick = {
                            isLoading = true
                            isError = false

                            // API-Aufruf starten und Callback übergeben
                            onUpdateNameApiCall(newNameText) { success ->
                                isLoading = false
                                if (success) {
                                    showDialog = false // Bedingung erfüllt: Schließt nur bei true
                                } else {
                                    isError = true // Bleibt offen und zeigt Fehler
                                }
                            }
                        }
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF3B71CA)
                            )
                        } else {
                            Text("Speichern", color = Color(0xFF3B71CA))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isLoading,
                        onClick = { showDialog = false }
                    ) {
                        Text("Abbrechen", color = Color(0xFF8A939E))
                    }
                }
            )
        }
    }
}

@Composable
fun LogoutSetting(
    registrationStatus: Boolean,
    offline: Boolean,
    onLogoutAndDeleteData: () -> Unit,
    onLogoutOnly: () -> Unit, // Wird aufgerufen, wenn registrationStatus = false ist
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val destructiveRed = Color(0xFFDC3545)
    val cardBackground = Color(0xFF1E222B)
    val textGray = Color(0xFF8A939E)

    Column(modifier = modifier) {
        // Der Logout-Button in den Einstellungen (Rot gefärbt als destruktive Aktion)
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = destructiveRed.copy(alpha = 0.15f), disabledContainerColor = destructiveRed.copy(alpha = 0.05f)),
            enabled = !offline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Abmelden",
                color = if (offline) Color.Gray else destructiveRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (offline) Text(text = "Zum Abmelden ist eine Internetverbindung erforderlich.", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // Das Logout-Popup
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = cardBackground,
                title = {
                    Text(
                        text = "Konto abmelden",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Dynamischer Hinweistext je nach Registrierungsstatus
                        Text(
                            text = if (registrationStatus) {
                                "Dieses Gerät ist am Server registriert. Möchtest du dein Gerät beim Abmelden löschen oder es als Inaktiv markieren?"
                            } else {
                                "Möchtest du dich wirklich abmelden?"
                            },
                            color = textGray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (registrationStatus) {
                            // --- BEDINGUNG: registrationStatus == true (3 Optionen) ---

                            // Option 1: Abmelden & Löschen (Kritische Aktion)
                            Button(
                                onClick = {
                                    showDialog = false
                                    onLogoutAndDeleteData()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = destructiveRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abmelden & Gerät löschen", color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Option 2: Abmelden & Behalten
                            Button(
                                onClick = {
                                    showDialog = false
                                    onLogoutOnly()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C313A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abmelden & Gerät inaktiv setzen", color = Color.White)
                            }
                        } else {
                            // --- BEDINGUNG: registrationStatus == false (2 Optionen) ---

                            // Option 1: Einfaches Abmelden
                            Button(
                                onClick = {
                                    showDialog = false
                                    onLogoutOnly()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = destructiveRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abmelden", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 3 / Cancel: Abbrechen (In beiden Fällen identisch)
                        OutlinedButton(
                            onClick = { showDialog = false },
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2C313A))
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abbrechen", color = textGray)
                        }
                    }
                },
                // Da wir das Layout komplett in 'text' via Column regeln,
                // lassen wir die Standard-Buttons leer, um doppelte Reihen zu vermeiden.
                confirmButton = {},
                dismissButton = {}
            )
        }
    }
}