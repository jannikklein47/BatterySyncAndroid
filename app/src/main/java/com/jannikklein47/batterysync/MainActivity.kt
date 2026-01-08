package com.jannikklein47.batterysync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity() {

    // Simple data class
    data class Device(val name: String, val requiresOtp: Boolean, val id: Int, val battery: Double)

    var timer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        createNotificationChannel()
        super.onCreate(savedInstanceState)

        var deviceList by mutableStateOf(listOf<MainActivity.Device>())
        val dataStoreManager = DataStoreManager(applicationContext)

        var token by mutableStateOf<String?>(null)
        var uuid by mutableStateOf<String?>(null)
        CoroutineScope(Dispatchers.IO).launch {
            token = dataStoreManager.getToken()
            uuid = dataStoreManager.getUuid()
        }
        var deviceListIsRefreshing by mutableStateOf(false)
        var generalLoadingState by mutableStateOf(false)
        var generalErrorMessage by mutableStateOf<String?>(null)
        var displayUsername by mutableStateOf("...")
        var ownDisplayName by mutableStateOf<String?>(null)
        var registrationStatus by mutableStateOf(false)
        var selectedReplaceDeviceCanGenerateOtp by mutableStateOf<Boolean?>(null)
        var foregroundServiceIsRunning by mutableStateOf(BatteryService.isRunning)
        var otpInputWrong by mutableStateOf<Boolean?>(null)

        fun openWebsite() {
            val browserIntent = Intent(Intent.ACTION_VIEW, "https://batterysync.de".toUri());
            startActivity(browserIntent);
        }

        fun getDeviceList() {
            Thread {
                try {

                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d("MainActivity", "Get devices")
                        try {
                            val token = dataStoreManager.getToken()
                            if (!token.isNullOrEmpty()) {
                                val url = URL("https://batterysync.de:3000/device")
                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "GET"
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                connection.setRequestProperty("Content-Type","application/json")
                                connection.setRequestProperty("Authorization", token)

                                val responseCode = connection.responseCode

                                if (responseCode == 200) {
                                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                                    Log.d("MainActivity", response)
                                    val deviceListJSON = JSONArray(response)

                                    deviceList = listOf()
                                    var i = 0
                                    while (i < deviceListJSON.length()) {

                                        try {
                                            deviceList = deviceList.plus(
                                                Device(
                                                    deviceListJSON.getJSONObject(i).getString("name"),
                                                    deviceListJSON.getJSONObject(i).getBoolean("requiresOtp"),
                                                    deviceListJSON.getJSONObject(i).getInt("id"),
                                                    deviceListJSON.getJSONObject(i).getDouble("battery")
                                                    )
                                            )

                                        } catch (e: JSONException) {
                                            deviceList = deviceList.plus(Device("JSON ERROR", true, -1, 0.0))
                                        }


                                        i = i + 1
                                    }

                                    deviceListIsRefreshing = false

                                    Log.d("MainActivity", "Loaded devices")

                                } else {
                                    deviceListIsRefreshing = false
                                    Log.d("MainActivity", "Error response")
                                }
                            } else {
                                deviceListIsRefreshing = false
                                Log.d("MainActivity", "not logged in")
                                throw Error("Not logged in")
                            }
                        } catch (e: Exception) {
                            deviceListIsRefreshing = false
                            Log.e("ReplaceDevicesScreen", e.toString())
                        }
                    }

                } catch (e: Exception) {
                    deviceListIsRefreshing = false
                    Log.e("MainActivity", e.toString())
                }
            }.start()
        }
        
        fun login(username: String, password: String, onLoginSuccess:() -> Unit) {

            Thread {
                CoroutineScope(Dispatchers.IO).launch {

                    generalLoadingState = true

                    try {
                        val url = URL("https://batterysync.de:3000/login?email=$username&password=$password")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")

                        val responseCode = connection.responseCode

                        if (responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }

                            // Save token in background
                            dataStoreManager.saveToken(token = response)

                            // Switch back to UI thread
                            withContext(Dispatchers.Main) {
                                generalLoadingState = false
                                onLoginSuccess()
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                generalLoadingState = false
                                generalErrorMessage = connection.responseMessage
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            generalLoadingState = false
                            generalErrorMessage = e.message
                        }
                        Log.e("MainActivity", e.printStackTrace().toString())
                    }
                }
            }.start()
            
        }

        fun logout(delete: Boolean, onSuccess: () -> Unit) {

            Thread {
                CoroutineScope(Dispatchers.IO).launch {

                    token = dataStoreManager.getToken()
                    uuid = dataStoreManager.getUuid()

                    generalLoadingState = true

                    try {
                        val url = if (delete) URL("https://batterysync.de:3000/device/logout/delete?uuid=$uuid") else URL("https://batterysync.de:3000/device/logout/inactive?uuid=$uuid")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")
                        connection.setRequestProperty("Authorization",token)

                        val responseCode = connection.responseCode

                        if (responseCode == 200) {
                            // Switch back to UI thread
                            withContext(Dispatchers.Main) {
                                generalLoadingState = false
                                onSuccess()
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                generalLoadingState = false
                                generalErrorMessage = connection.responseMessage
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            generalLoadingState = false
                            generalErrorMessage = e.message
                        }
                        Log.e("MainActivity", e.printStackTrace().toString())
                    }
                }
            }.start()
        }

        fun getUsername() {
            Thread {
                try {
                    generalLoadingState = true
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                    if (!token.isNullOrEmpty()) {
                        val url = URL("https://batterysync.de:3000/login/auth")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")
                        connection.setRequestProperty("Authorization", token)

                        val responseCode = connection.responseCode

                        if (responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            displayUsername = response
                            generalLoadingState = false

                        } else {
                            generalLoadingState = false
                            generalErrorMessage = connection.responseMessage
                        }
                    } else {
                        generalLoadingState = false
                    }
                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()


        }
        fun getUsername(onSuccess: () -> Unit) {
            Thread {
                try {
                    generalLoadingState = true
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                    if (!token.isNullOrEmpty()) {
                        val url = URL("https://batterysync.de:3000/login/auth")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")
                        connection.setRequestProperty("Authorization", token)

                        val responseCode = connection.responseCode

                        if (responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            displayUsername = response
                            generalLoadingState = false
                            onSuccess()

                        } else {
                            generalLoadingState = false
                            generalErrorMessage = connection.responseMessage
                        }
                    } else {
                        generalLoadingState = false
                    }
                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()


        }

        fun checkRegistrationStatus() {
            Thread {
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                        uuid = dataStoreManager.getUuid()
                    }
                    if (!uuid.isNullOrEmpty()) {
                        val url = URL("https://batterysync.de:3000/device/uuid?uuid=$uuid")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")
                        connection.setRequestProperty("Authorization", token)

                        val secondResponseCode = connection.responseCode

                        if (secondResponseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            try {
                                ownDisplayName = JSONObject(response).getString("name")
                            } catch (e: Exception) {
                                ownDisplayName = "_"
                            }

                            registrationStatus = true
                            generalLoadingState = false
                        }
                    } else {
                        registrationStatus = false
                        generalLoadingState = false
                    }
                } catch (e: Exception) {
                    generalLoadingState = false
                    registrationStatus = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        // Duplicate to checkRegistrationStatus ???
        fun getDisplayName(uuid: String) {
            val secondUrl = URL("https://batterysync.de:3000/device/uuid?uuid=$uuid")
            val secondConnection = secondUrl.openConnection() as HttpURLConnection
            secondConnection.requestMethod = "POST"
            secondConnection.connectTimeout = 5000
            secondConnection.readTimeout = 5000
            secondConnection.setRequestProperty("Content-Type","application/json")
            secondConnection.setRequestProperty("Authorization", token)

            secondConnection.connect()

            val secondResponseCode = secondConnection.responseCode
            if (secondResponseCode == 200) {
                val secondResponse = secondConnection.inputStream.bufferedReader().use { it.readText() }

                var name = ""
                try {
                    name = JSONObject(secondResponse).getString("name")
                } catch (e: Exception) {
                    name = "_"
                }

                ownDisplayName = name
                registrationStatus = true
                generalLoadingState = false

                Log.d("MainActivity", "Matched identifiers: $ownDisplayName")


            } else {
                Log.d("MainActivity", "Failed to match identifiers")
                generalLoadingState = false
            }
        }

        fun registerDevice() {

            Thread {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        generalLoadingState = true

                        val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
                        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)


                        val url = URL("https://batterysync.de:3000/device/register?system=phone&battery=${0.01 * batLevel.toDouble()}")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")
                        connection.setRequestProperty("Authorization", token)

                        val responseCode = connection.responseCode

                        if (responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }

                            Log.d("MainActivity", "Got assigned UUID: $response")

                            // Save token in background
                            dataStoreManager.saveUUID(response)

                            uuid = response

                            getDisplayName(response)
                        } else {
                            generalLoadingState = false
                        }
                    } catch (e: Exception) {
                        generalLoadingState = false
                        registrationStatus = false
                        dataStoreManager.saveUUID("")
                        Log.e("MainActivity", e.printStackTrace().toString())
                    }

                }
            }.start()

        }

        fun checkOtpCreatable(device: Device) {
            Thread {
                generalLoadingState = true
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                    val url = URL("https://batterysync.de:3000/device/otpCreatable?id=${device.id}")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Authorization", token)

                    val secondResponseCode = connection.responseCode

                    if (secondResponseCode == 200) {
                        generalLoadingState = false
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        selectedReplaceDeviceCanGenerateOtp = try {
                            JSONObject(response).getBoolean("status")
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        generalLoadingState = false
                    }
                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        fun sendCodeToDevice(device: Device) {
            Log.d("MainActivity", "Send code")
            Thread {
                generalLoadingState = true
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                        val url = URL("https://batterysync.de:3000/device/otp?id=${device.id}")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Content-Type","application/json")
                        connection.setRequestProperty("Authorization", token)

                        val secondResponseCode = connection.responseCode

                        if (secondResponseCode == 200) {
                            generalLoadingState = false
                        } else {
                            generalLoadingState = false
                        }
                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        fun confirmOtp(device: Device, otp: String, success: () -> Unit) {
            Thread {
                otpInputWrong = null
                generalLoadingState = true
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                    val url = URL("https://batterysync.de:3000/device/newUuid?id=${device.id}&otp=$otp")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Content-Type","application/json")
                    connection.setRequestProperty("Authorization", token)

                    val secondResponseCode = connection.responseCode

                    if (secondResponseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        Log.d("MainActivity", "Got new uuid: $response")

                        otpInputWrong = null

                        CoroutineScope(Dispatchers.IO).launch {
                            dataStoreManager.saveUUID(response)
                            getDisplayName(response)
                            uuid = response
                            withContext(Dispatchers.Main) {
                                success()
                            }

                        }
                    } else {
                        generalLoadingState = false
                        otpInputWrong = true
                    }

                } catch (e: Exception) {
                    generalLoadingState = false
                    otpInputWrong = true
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        fun instantLink(device: Device, success: () -> Unit) {
            Thread {
                generalLoadingState = true
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                    val url = URL("https://batterysync.de:3000/device/newUuid?id=${device.id}")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Content-Type","application/json")
                    connection.setRequestProperty("Authorization", token)

                    val secondResponseCode = connection.responseCode

                    if (secondResponseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        Log.d("MainActivity", "Got new uuid: $response")

                        CoroutineScope(Dispatchers.IO).launch {
                            dataStoreManager.saveUUID(response)
                            getDisplayName(response)
                            uuid = response
                            withContext(Dispatchers.Main) {
                                success()
                            }

                        }
                    } else {
                        generalLoadingState = false
                    }

                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        fun renameDevice(name: String, success: () -> Unit) {
            Log.d("MainActivity", "Rename device to $name")
            Thread {
                generalLoadingState = true
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                        uuid = dataStoreManager.getUuid()
                    }
                    val url = URL("https://batterysync.de:3000/device/name?uuid=$uuid&name=$name")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "PATCH"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Content-Type","application/json")
                    connection.setRequestProperty("Authorization", token)

                    val secondResponseCode = connection.responseCode

                    if (secondResponseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        ownDisplayName = response
                        success()

                        generalLoadingState = false
                    } else {
                        generalLoadingState = false
                    }
                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        fun registerUser(username: String, password: String, onSuccess: () -> Unit) {
            Log.d("MainActivity", "Register user $username")
            Thread {
                generalLoadingState = true
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        token = dataStoreManager.getToken()
                    }
                    val url = URL("https://batterysync.de:3000/login/register?email=$username&password=$password")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Content-Type","application/json")
                    connection.setRequestProperty("Authorization", token)

                    val secondResponseCode = connection.responseCode

                    if (secondResponseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        CoroutineScope(Dispatchers.IO).launch {
                            // Save token in background
                            dataStoreManager.saveToken(token = response)

                            // Switch back to UI thread
                            withContext(Dispatchers.Main) {
                                generalLoadingState = false
                                onSuccess()
                            }
                        }


                        generalLoadingState = false
                    } else {
                        generalLoadingState = false
                    }
                } catch (e: Exception) {
                    generalLoadingState = false
                    Log.e("MainActivity", e.printStackTrace().toString())
                }
            }.start()
        }

        fun foregroundInterval() {
            timer.schedule(object: TimerTask() {
                override fun run() {
                    CoroutineScope(Dispatchers.IO).launch {

                        foregroundServiceIsRunning = BatteryService.isRunning
                        Log.d("MainActivity", "Check foreground service: $foregroundServiceIsRunning")
                        foregroundInterval()
                    }

                }
            }, 3000)

        }

        setContent {
            val navController = rememberNavController()
            var startDestination = "login"

            if (!token.isNullOrEmpty()) {

                getUsername(onSuccess = {
                    startDestination = "home"
                    checkRegistrationStatus()
                    getDeviceList()
                    foregroundInterval()
                })

            }

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable("login") {
                    WelcomeScreen().display(
                        generalErrorMessage,
                        generalLoadingState,
                        onLogin = { username, password ->
                            login(username, password) {
                                Log.d("MainActivity", "Navigate to Home")
                                generalErrorMessage = null
                                navController.navigate("home") {
                                    getUsername()
                                    checkRegistrationStatus()
                                    getDeviceList()
                                    foregroundInterval()
                                    popUpTo("login") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                }
                            }
                        }, onRegister = { username, password, repeatPassword ->
                            generalLoadingState = true
                            if (password.length < 8) {
                                generalErrorMessage = "Passwort muss mindestens 8 Zeichen lang sein."
                                generalLoadingState = false
                            }
                            if (password != repeatPassword) {
                                generalLoadingState = false
                                generalErrorMessage = "Passwörter stimmen nicht überein."
                            }
                            if (username.length < 4) {
                                generalLoadingState = false
                                generalErrorMessage = "Nutzername muss mindestens 4 Zeichen lang sein"
                            }

                            registerUser(username, password) {
                                Log.d("MainActivity", "Navigate to Home")
                                generalErrorMessage = null
                                navController.navigate("home") {
                                    getUsername()
                                    checkRegistrationStatus()
                                    popUpTo("login") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                }
                            }

                        }
                    )
                }

                composable("home") { backStackEntry ->
                    HomeScreen().display(displayUsername, registrationStatus, ownDisplayName, uuid, generalLoadingState, generalErrorMessage, deviceList, deviceListIsRefreshing, foregroundServiceIsRunning, logout = { delete ->
                        if (registrationStatus) {
                            logout(delete) {
                                Log.d("MainActiviy", "Navigate to login")
                                generalErrorMessage = null
                                CoroutineScope(Dispatchers.IO).launch {
                                    dataStoreManager.saveToken("")
                                    dataStoreManager.saveAllDevices("[]")
                                    dataStoreManager.saveUUID("")
                                    displayUsername = ""
                                    ownDisplayName = null
                                    registrationStatus = false
                                    uuid = null
                                    token = null
                                }
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                }
                            }
                        } else {
                            Log.d("MainActiviy", "Navigate to login")
                            generalErrorMessage = null
                            CoroutineScope(Dispatchers.IO).launch {
                                dataStoreManager.saveToken("")
                                dataStoreManager.saveAllDevices("[]")
                                dataStoreManager.saveUUID("")
                                displayUsername = ""
                                ownDisplayName = null
                                registrationStatus = false
                                uuid = null
                                token = null
                            }
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                            }
                        }


                    }, onOpenReplaceOld = {
                        Log.d("MainActivity", "Navigate to replaceDevice")
                        getDeviceList()
                        otpInputWrong = null
                        generalErrorMessage = null
                        navController.navigate("replaceDevice")
                    }, registerDevice = {
                        registerDevice()
                    }, renameDevice = { name, success ->
                        renameDevice(name, success)
                    }, onRefresh = {
                        deviceListIsRefreshing = true
                        getUsername()
                        checkRegistrationStatus()
                        getDeviceList()
                    }, startForegroundService = {
                        startForegroundService()
                        foregroundServiceIsRunning = BatteryService.isRunning
                    }, openWebsite = {
                        openWebsite()
                    })
                }

                composable("replaceDevice") { backStackEntry ->
                    ReplaceDeviceScreen().DeviceLinkScreen(applicationContext,
                        deviceList,
                        generalLoadingState,
                        deviceListIsRefreshing,
                        selectedReplaceDeviceCanGenerateOtp,
                        otpInputWrong,
                        onRefresh = {
                            deviceListIsRefreshing = true
                            getDeviceList()
                        }, onSendCode = { device ->
                            sendCodeToDevice(device)
                        }, onConfirmInputCode = { code, device ->
                            generalLoadingState = true
                            confirmOtp(device, code) {
                                Log.d("MainActivity", "Input code: $code for device ${device.name}")
                                navController.navigate("home") {
                                    getUsername()
                                    checkRegistrationStatus()
                                    popUpTo("replaceDevice") { inclusive = true }  // verhindert zurück
                                }
                                generalLoadingState = false
                            }

                        }, onLinkNow = { device ->
                            generalLoadingState = true
                            Log.d("MainActivity", "Request instant link for ${device.name}")
                            instantLink(device) {
                                Log.d("MainActivity", "Success link for ${device.name}")
                                navController.navigate("home") {
                                    getUsername()
                                    checkRegistrationStatus()
                                    popUpTo("replaceDevice") { inclusive = true }  // verhindert zurück
                                }
                                generalLoadingState = false
                            }
                        }, onSelectDevice = { device ->
                            selectedReplaceDeviceCanGenerateOtp = null
                            checkOtpCreatable(device)
                        })
                }
            }
        }



        requestRequiredPermissions()


        //val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        //registerReceiver(batteryReceiver, filter);
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "battery_channel",
            "Battery Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notiChannel = NotificationChannel(
            "not_channel",
            "Notification Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(notiChannel)

    }

    private val requiredPermissions = buildList {
        if (Build.VERSION.SDK_INT >= 33)
            add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestRequiredPermissions() {
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(), 42)
        } else {
            Log.d("Permissions","All Permissions are granted.")
        }
    }

    // handle result (for API <33)
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Log.d("Permissions", "All granted!")
        } else {
            Log.d("Permissions", "Not granted")
        }
    }

    fun startForegroundService() {
        val serviceIntent = Intent(applicationContext, BatteryService::class.java)
        applicationContext.startForegroundService(serviceIntent)
    }




}

