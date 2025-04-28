package com.jannikklein47.batterysync

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

class LoginViewModel(context: Context) : ViewModel() {
    var deviceName by mutableStateOf("")
    var presetDeviceName = ""
    var presetEmail = ""
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var savedContext = context

    private val dataStoreManager = DataStoreManager(savedContext)

    fun login() {
        Thread {
            try {
                val url = URL("http://192.168.0.119:3000/login?email=$email&password=$password")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Contenty-Type","application/json")


                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("LoginViewModel", "Antwort: $response")
                    viewModelScope.launch {
                        dataStoreManager.saveToken(token = response)
                    }
                } else {
                    Log.e("LoginViewModel", "Serverfehler: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("BatterySender", "Fehler beim Senden: ${e}")
            }
        }.start()


            /*
            if (email == "test" && password == "test") {
                //val fakeToken = "\$2b\$11\$2UFkElxKzuk1L9RdCqIBL.7rjflF6bX/BHjqS3ZUvNgBk1Lb76ouu"
                val fakeToken = "\$2b\$11\$2UFkElxKzuk1L9RdCqIBL.7rjflF6bX/BHjqS3ZUvNgBk1Lb76ouu"
                dataStoreManager.saveToken(fakeToken)
                errorMessage = "Login erfolgreich!"
            } else {
                errorMessage = "Login fehlgeschlagen."
            }

             */

    }

    fun loadDeviceName() {
        viewModelScope.launch {
            val tempDeviceName = dataStoreManager.getDeviceName()

            if (tempDeviceName != null) {
                Log.d("LoginViewModel", "Loaded own device name: $tempDeviceName")
                presetDeviceName = tempDeviceName
            } else {
                Log.d("LoginViewModel", "Could not load own device name")
            }
        }
    }

    fun saveDeviceName() {
        viewModelScope.launch {
            dataStoreManager.saveDeviceName(deviceName)
        }
    }

    fun restartService() {
        val serviceIntent = Intent(savedContext, BatteryService::class.java)
        savedContext.startForegroundService(serviceIntent)
    }
}
