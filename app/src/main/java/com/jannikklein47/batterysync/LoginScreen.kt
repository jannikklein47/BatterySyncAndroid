package com.jannikklein47.batterysync

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import java.nio.file.WatchEvent

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    viewModel.loadDeviceName()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("BatterySyncApp", fontSize = 30.sp)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = viewModel.deviceName,
            onValueChange = { viewModel.deviceName = it},
            label = { Text("Gerätename")},
            placeholder = { Text(viewModel.presetDeviceName)},
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { viewModel.saveDeviceName() }, modifier = Modifier.fillMaxWidth()) {
            Text("Name speichern")
        }
        Spacer(modifier = Modifier.height(30.dp))
        TextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("E-Mail") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Passwort") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.login() }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }

        if (viewModel.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(viewModel.errorMessage)
        }
    }
}


