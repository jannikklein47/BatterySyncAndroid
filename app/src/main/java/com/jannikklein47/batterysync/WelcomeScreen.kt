package com.jannikklein47.batterysync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WelcomeScreen {

    @Composable
    fun display(errorMessage: String?, loadingState: Boolean, onLogin: (username: String, password: String) -> Unit, onRegister: (username: String, password: String, repeatPassword: String) -> Unit) {
        val green = Color(0xFF7CDEB4)
        val teal = Color(0xFF28B0A5)
        val blue = Color(0xFF3E73B8)

        val gradient = Brush.horizontalGradient(
            colors = listOf(
                blue,  // blue
                teal, // teal
                green, // green
            )
        )

        var username by remember { mutableStateOf("") }
        var repeatPassword by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var doRegister by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Willkommen bei",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light
                )

                GradientText("BatterySync", gradient)

                Spacer(modifier = Modifier.height(50.dp))

                if (!doRegister) {
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
                        Column {

                            Text(
                                text = "Anmelden",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                enabled = !loadingState,
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Nutzername", color = Color.LightGray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = blue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                enabled = !loadingState,
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Passwort", color = Color.LightGray) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = blue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(color= Color.White)) { append("Alternativ kannst du dir ") }
                                    withStyle(style = SpanStyle(color= Color.White, textDecoration = TextDecoration.Underline)) { append("hier") }
                                    withStyle(style = SpanStyle(color= Color.White)) { append(" ein Konto erstellen") }
                                },
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp).clickable {
                                    doRegister = true
                                }
                            )

                            if (errorMessage != null) {
                                Text(
                                    text = "Fehler: " + if (errorMessage == "Forbidden") "Falscher Nutzername oder Passwort." else if (errorMessage == "Not Found") "Dieses Konto existiert nicht." else if (errorMessage == "Failed to connect to batterysync.de/164.30.68.206:3000") "Netzwerkfehler" else errorMessage,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }



                            Button(
                                enabled = !loadingState,
                                onClick = {
                                    onLogin(username, password) },
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
                                            Brush.horizontalGradient(
                                                listOf(green, teal, blue)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loadingState) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.width(20.dp).offset(y = 6.dp)

                                        )
                                    } else {
                                        Text(
                                            text = "Absenden",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }


                                }
                            }
                        }
                    }
                } else {
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
                        Column {

                            Text(
                                text = "Registrieren",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                enabled = !loadingState,
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Nutzername (min. 4 Zeichen)", color = Color.LightGray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = blue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                enabled = !loadingState,
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Passwort (min. 8 Zeichen)", color = Color.LightGray) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = blue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                enabled = !loadingState,
                                value = repeatPassword,
                                onValueChange = { repeatPassword = it },
                                label = { Text("Passwort wiederholen", color = Color.LightGray) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = blue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(color= Color.White)) { append("Alternativ kannst du dich ") }
                                    withStyle(style = SpanStyle(color= Color.White, textDecoration = TextDecoration.Underline)) { append("hier") }
                                    withStyle(style = SpanStyle(color= Color.White)) { append(" anmelden") }
                                },
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp).clickable {
                                    doRegister = false
                                }
                            )

                            if (errorMessage != null) {
                                Text(
                                    text = "Fehler: " + if (errorMessage == "Forbidden") "Falscher Nutzername oder Passwort." else if (errorMessage == "Not Found") "Dieses Konto existiert nicht." else if (errorMessage == "Failed to connect to batterysync.de/164.30.68.206:3000") "Netzwerkfehler" else errorMessage,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }



                            Button(
                                enabled = !loadingState,
                                onClick = {
                                    if (!doRegister) {
                                        onLogin(username, password)
                                    } else {
                                        onRegister(username, password, repeatPassword)
                                    } },
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
                                            Brush.horizontalGradient(
                                                listOf(green, teal, blue)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loadingState) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.width(20.dp).offset(y = 6.dp)

                                        )
                                    } else {
                                        Text(
                                            text = "Absenden",
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
}