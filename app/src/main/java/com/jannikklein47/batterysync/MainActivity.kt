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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.*


fun request(method: String, url: String, token: String?, onFinish: (statusCode: Int, response: String) -> Unit) {
    Thread {
        try {
            val url = URL("https://batterysync.de:3000$url")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Content-Type", "application/json")
            if (!token.isNullOrEmpty()) connection.setRequestProperty("Authorization", token)

            val responseCode = connection.responseCode
            var response: String = ""
            if (responseCode < 400) response = connection.inputStream.bufferedReader().use { it.readText() }
            else response = connection.errorStream.bufferedReader().use { it.readText() }
            CoroutineScope(Dispatchers.Main).launch {
                onFinish(responseCode, response)
            }
        } catch (e: Exception) {
            Log.d("API Call", "Error with request: $e")
            onFinish(0, "")
        }
    }.start()
}

class MainActivity : ComponentActivity() {

    // Simple data class
    data class Device(val name: String, val requiresOtp: Boolean, val id: Int, val battery: Double, val predictedZeroAt: String, val favorite: Boolean, val isPLuggedIn: Boolean, val chargingStatus: Boolean, val cycles: Int, val healthScore: Int, val healthyPercentCharged: Int)

    var timer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        createNotificationChannel()
        super.onCreate(savedInstanceState)

        var deviceList by mutableStateOf(listOf<MainActivity.Device>())
        val dataStoreManager = DataStoreManager(applicationContext)

        var token by mutableStateOf<String?>(null)
        CoroutineScope(Dispatchers.IO).launch {
            token = dataStoreManager.getToken()
        }
        var deviceListIsRefreshing by mutableStateOf(false)
        var generalLoadingState by mutableStateOf(false)
        var generalErrorMessage by mutableStateOf<String?>(null)
        var displayUsername by mutableStateOf("...")
        var registrationStatus by mutableStateOf(false)
        var selectedReplaceDeviceCanGenerateOtp by mutableStateOf<Boolean?>(null)
        var foregroundServiceIsRunning by mutableStateOf(BatteryService.isRunning)
        var otpInputWrong by mutableStateOf<Boolean?>(null)

        var widgetShowPercent by mutableStateOf(true)
        var widgetAlwaysFirst by mutableStateOf(true)
        var widetDarkMode by mutableStateOf<Boolean?>(null)
        var widgetTransparency by mutableFloatStateOf(0.94f)

        var localId by mutableIntStateOf(0)
        var localDeviceName by mutableStateOf("Dieses Gerät")
        var offline by mutableStateOf(false)

        fun openWebsite() {
            val browserIntent = Intent(Intent.ACTION_VIEW, "https://batterysync.de".toUri());
            startActivity(browserIntent);
        }

        fun openDevice(id: Int) {
            val browserIntent = Intent(Intent.ACTION_VIEW, "https://batterysync.de/devices?id=$id".toUri());
            startActivity(browserIntent);
        }

        fun checkLogin(success: () -> Unit, offline: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                if (token.isNullOrEmpty()) {
                    fail()
                }
                request("GET", "/login/auth", token, onFinish = { code, res ->
                    Log.d("MainActivity", "Login result: $code:$res")
                    CoroutineScope(Dispatchers.IO).launch {
                        if (code == 200) {
                            store.saveUserName(res)
                            displayUsername = res
                            withContext(Dispatchers.Main) {
                                success()
                            }
                        } else if (code == 422) {
                            displayUsername = res
                            withContext(Dispatchers.Main) {
                                fail()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                offline()
                            }
                        }
                    }
                })
            }
        }

        fun parseBatteryHistory(data: String): BatteryHistory {
            val obj = JSONObject(data)
            val dayHistory = obj.getJSONArray("day")
            val weekHistory = obj.getJSONArray("week")

            val history = BatteryHistory(day = emptyList(), week = emptyList())

            for (i in 0..<dayHistory.length()) {
                val element = dayHistory.getJSONArray(i)
                val createdAt = Instant.ofEpochMilli(element.getLong(0))
                val battery = element.getInt(1)
                history.day = history.day.plus(BatteryHistoryEntry(createdAt, battery))
            }

            for (i in 0..<weekHistory.length()) {
                val element = weekHistory.getJSONArray(i)
                val createdAt = Instant.ofEpochMilli(element.getLong(0))
                val battery = element.getInt(1)
                history.week = history.week.plus(BatteryHistoryEntry(createdAt, battery))
            }

            history.day = history.day.reversed()
            history.week = history.week.reversed()

            return history
        }

        fun parseDeviceJSON(data: String) {
            try {
                val deviceListJSON = JSONArray(data)
                deviceList = listOf()


                Log.d("MainActivity", "Data to parse: $data")

                for (i in 0..<deviceListJSON.length()) {
                    try {
                        val element = deviceListJSON.getJSONObject(i)
                        val device = Device(
                            name = element.getString("name"),
                            requiresOtp = element.getBoolean("requiresOtp"),
                            id = element.getInt("id"),
                            battery = element.getDouble("battery"),
                            predictedZeroAt = element.getString("predictedZeroAt"),
                            favorite = element.getBoolean("favorite"),
                            isPLuggedIn = element.getBoolean("isPluggedIn"),
                            chargingStatus = element.getBoolean("chargingStatus"),
                            cycles = element.getInt("cyclesCached"),
                            healthScore = element.getInt("batteryHealthScoreCached"),
                            healthyPercentCharged = element.getInt("percentHealthyChargesCached")
                        )

                        deviceList = deviceList.plus(device)
                    } catch (e: Exception) {

                        Log.e("MainActivity", "Error in parsing: $e")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in parsing $data: $e")
            }
        }

        fun loadOffline() {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                localId = store.getLocalId()
                localDeviceName = store.getDeviceName() ?: "Dieses Gerät"
                displayUsername = store.getUserName() ?: "Nutzer"
                val storedDevices = store.getAllDevices()
                if (!storedDevices.isNullOrEmpty()) parseDeviceJSON(storedDevices)
                else parseDeviceJSON("[]")
            }
        }

        fun checkRegistrationStatus(success: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                val uuid = store.getUuid()
                if (uuid.isNullOrEmpty()) fail()
                else {
                    request("POST", "/device/uuid?uuid=$uuid&build=${Globals().BUILD}", token) { code, res ->
                        if (code == 200) {
                            val json = JSONObject(res)
                            val deviceName = json.getString("name")
                            val id = json.getInt("id")
                            localId = id
                            localDeviceName = deviceName
                            registrationStatus = true
                            CoroutineScope(Dispatchers.IO).launch {
                                store.saveLocalId(id)
                                store.saveDeviceName(deviceName)
                                withContext(Dispatchers.Main) {
                                    success()
                                }
                            }
                        } else {
                            registrationStatus = false
                            fail()
                        }
                    }
                }
            }
        }

        fun loadAllData(onSuccess: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                val uuid = store.getUuid()
                val build = Globals().BUILD
                if (!token.isNullOrEmpty() && !offline) {
                    withContext(Dispatchers.Main) {
                        // Right here, using the "checkRegistrationStatus" function should be better
                        Log.d("MainActivity", "Requesting device info")
                        if (!uuid.isNullOrEmpty()) request("POST", "/device/uuid?uuid=$uuid&build=$build", token,
                            onFinish = { code, res ->
                                Log.d("MainActivity", "Got device info $code:$res")
                                if (code == 200) {
                                    val json = JSONObject(res)
                                    val deviceName = json.getString("name")
                                    val id = json.getInt("id")
                                    localId = id
                                    localDeviceName = deviceName
                                    registrationStatus = true
                                    CoroutineScope(Dispatchers.IO).launch {
                                        store.saveLocalId(id)
                                        store.saveDeviceName(deviceName)
                                    }
                                    request("GET", "/device", token, onFinish = { code, res ->
                                        Log.d("MainActivity", "Request device list")
                                        if (code == 200) {
                                            Log.d("MainActivity", "Got device list")
                                            parseDeviceJSON(res)
                                            Log.d("MainActivity", "Parsed device list")
                                            onSuccess()
                                        } else {
                                            Log.d("MainActivity", "Did not get device list")
                                        }
                                    })
                                } else if (code == 404) {
                                    // This device is not registered!
                                    registrationStatus = false
                                    request("GET", "/device", token, onFinish = { code, res ->
                                        Log.d("MainActivity", "Request device list")
                                        if (code == 200) {
                                            Log.d("MainActivity", "Got device list")
                                            parseDeviceJSON(res)
                                            Log.d("MainActivity", "Parsed device list")
                                            onSuccess()
                                        } else {
                                            Log.d("MainActivity", "Did not get device list")
                                        }
                                    })
                                } else {
                                    // We are offline?
                                    loadOffline()
                                    CoroutineScope(Dispatchers.Main).launch {
                                        onSuccess()
                                    }
                                }
                            }
                        )
                        else {
                            request("GET", "/device", token, onFinish = { code, res ->
                                Log.d("MainActivity", "Request device list")
                                if (code == 200) {
                                    Log.d("MainActivity", "Got device list")
                                    parseDeviceJSON(res)
                                    Log.d("MainActivity", "Parsed device list")
                                    onSuccess()
                                } else {
                                    Log.d("MainActivity", "Did not get device list")
                                }
                            })
                        }
                    }
                } else {
                    loadOffline()
                    CoroutineScope(Dispatchers.Main).launch {
                        onSuccess()
                    }
                }
            }
        }

        fun getDeviceList(success: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("GET", "/device", token, onFinish = { code, res ->
                    if (code == 200) {
                        parseDeviceJSON(res)
                        success()
                    }
                })
            }

        }

        fun login(username: String, password: String, success: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            request("POST", "/login?email=$username&password=$password", null) { code, res ->
                if (code == 200) {
                    CoroutineScope(Dispatchers.IO).launch {
                        store.saveToken(res)
                        withContext(Dispatchers.Main) {
                            success()
                        }
                    }
                }
            }
        }

        fun logout(delete: Boolean, success: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                val uuid = store.getUuid()
                val url = if (delete) "/device/logout/delete?uuid=$uuid" else "/device/logout/inactive?uuid=$uuid"
                request("POST", url, token) { code, res ->
                    Log.d("MainActivity", "Logout response: $code:$res")
                    if (code == 200) {
                        CoroutineScope(Dispatchers.IO).launch {
                            store.resetAll()
                            withContext(Dispatchers.Main) {
                                success()
                            }
                        }
                    }
                }
            }
        }

        fun getUsername(success: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("GET", "/login/auth", token) { code, res ->
                    if (code == 200) {
                        displayUsername = res
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveUserName(res)
                            withContext(Dispatchers.Main) {
                                success()
                            }
                        }
                    }
                }
            }
        }

        fun registerDevice(success: () -> Unit, fail: () -> Unit) {
            val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("POST", "/device/register?system=phone&battery=${0.01 * batLevel.toDouble()}", token) { code, res ->
                    if (code == 200) {
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveUUID(res)
                            withContext(Dispatchers.Main) {
                                success()
                            }
                        }
                    } else fail()
                }
            }
        }

        fun checkOtpCreatable(device: Device, success: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("GET", "/device/otpCreatable?id=${device.id}", token) { code, res ->
                    if (code == 200) {
                        selectedReplaceDeviceCanGenerateOtp = try {
                            JSONObject(res).getBoolean("status")
                        } catch (_: Exception) {
                            null
                        }
                        success()
                    } else {
                        fail()
                    }
                }
            }
        }

        fun sendCodeToDevice(device: Device, success: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("POST", "/device/otp?id=${device.id}", token) { code, _ ->
                    if (code == 200) {
                        success()
                    } else {
                        fail()
                    }
                }
            }
        }

        fun confirmOtp(device: Device, otp: String, success: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("POST", "/device/newUuid?id=${device.id}&otp=$otp", token) { code, res ->
                    if (code == 200) {
                        otpInputWrong = null
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveUUID(res)
                            checkRegistrationStatus(success = {
                                success()
                            }, fail = {
                                fail()
                            })
                        }
                    } else {
                        otpInputWrong = true
                        generalLoadingState = false
                        fail()
                    }
                }
            }
        }

        fun instantLink(device: Device, success: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("POST", "/device/newUuid?id=${device.id}", token) { code, res ->
                    if (code == 200) {
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveUUID(res)
                            checkRegistrationStatus(
                                success = { success() },
                                fail = { fail() }
                            )
                        }
                    } else {
                        generalLoadingState = false
                        fail()
                    }
                }
            }
        }

        fun renameDevice(name: String, callback: (Boolean) -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                val uuid = store.getUuid()
                if (uuid.isNullOrEmpty()) callback(false)
                else {
                    request("PATCH", "/device/name?uuid=$uuid&name=$name", token) { code, res ->
                        if (code == 200) {
                            localDeviceName = res
                            CoroutineScope(Dispatchers.IO).launch {
                                store.saveDeviceName(res)
                                notifyWidgets(applicationContext)
                                getDeviceList({ callback(true) })
                            }
                        }
                    }
                }
            }
        }

        fun registerUser(username: String, password: String, success: () -> Unit, fail: () -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                request("POST", "/login/register?email=$username&password=$password", null) { code, res ->
                    if (code == 200) {
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveToken(res)
                            withContext(Dispatchers.Main) {
                                success()
                            }
                        }
                    } else {
                        fail()
                    }
                }
            }
        }

        fun getDeviceHistory(id: Int, callback: (BatteryHistory?) -> Unit) {
            val store = DataStoreManager(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val token = store.getToken()
                request("GET", "/device/history?id=$id", token) { code, res ->
                    if (code == 200) {
                        callback(parseBatteryHistory(res))
                    } else {
                        callback(null)
                    }
                }
            }
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
            var startDestination = "loading"

            if (!token.isNullOrEmpty()) {
                // The user has logged in once, as this token can only be saved by a
                // successful login call. That means that we must check if that login is valid.
                // The following cases are possible:
                // 1 - The server returns 200, meaning the login is valid
                //        - The user is directed to the home page
                // 2 - The server returns 422, meaning the login is invalid
                //        - The user must login again
                // 3 - The server returns nothing, meaning there is no connection
                //        - The user is directed to the home page, with offline notice.
                //          Some features are disabled.

                Log.d("MainActivity", "Internet: ${isInternetAvailable(applicationContext)}")

                if (isInternetAvailable(applicationContext)) {
                    checkLogin(
                        success = {
                            Log.d("MainActivity", "Logged in")
                            startDestination = "home"
                            loadAllData(onSuccess = {
                                Log.d("MainActivity", "Loaded data")
                                foregroundInterval()
                                navController.navigate("home") {
                                    popUpTo(0)
                                }
                            })
                        },
                        offline = {
                            Log.d("MainActivity", "Offline")
                            startDestination = "home"
                            offline = true
                            loadOffline()
                            foregroundInterval()
                            navController.navigate("home") {
                                popUpTo(0)
                            }
                        },
                        fail = {
                            //
                        }
                    )
                } else {
                    Log.d("MainActivity", "Offline")
                    startDestination = "home"
                    offline = true
                    loadOffline()
                    foregroundInterval()
                }
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,

                // 1. Moving Forward: New screen slides in from right
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    )
                },
                // 2. Moving Forward: Current screen slides out to the left
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    )
                },
                // 3. Hitting Back: Previous screen slides back in from the left
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    )
                },
                // 4. Hitting Back: Current screen slides off the edge to the right
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    )
                }
            ) {
                composable("loading") {
                    LoadingScreen()
                }
                composable("login") {
                    WelcomeScreen().display(
                        generalErrorMessage,
                        generalLoadingState,
                        onLogin = { username, password ->
                            login(username, password) {
                                Log.d("MainActivity", "Navigate to Home")
                                generalErrorMessage = null
                                navController.navigate("home") {
                                    getUsername({
                                        loadAllData {
                                            foregroundInterval()
                                            popUpTo("login") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                        }
                                    })

                                }
                            }
                        },
                        onRegister = { username, password, repeatPassword ->
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

                            registerUser(username, password,
                                success = {
                                    Log.d("MainActivity", "Navigate to Home")
                                    generalErrorMessage = null
                                    loadAllData {
                                        getUsername {
                                            navController.navigate("home") {
                                                popUpTo(0)
                                            }
                                        }

                                    }

                                }, fail = {}
                            )

                        }
                    )
                }

                composable("home") { _ ->
                    DeviceDashboardScreen(deviceList, localId, localDeviceName, foregroundServiceIsRunning, displayUsername, offline,
                        onRegister = { registerDevice(
                            success = {
                                getDeviceList {
                                    checkRegistrationStatus(
                                        success = {}, fail = {}
                                    )
                                }

                            },
                            fail = {}
                        ) },
                        onInherit = {
                            getDeviceList({
                                otpInputWrong = null
                                generalErrorMessage = null
                                navController.navigate("replaceDevice")
                            })

                        },
                        onOpenSettings = {
                            CoroutineScope(Dispatchers.IO).launch {
                                widgetAlwaysFirst = DataStoreManager(applicationContext).getWidgetAlwaysFirst()
                                widgetShowPercent = DataStoreManager(applicationContext).getWidgetShowPercent()
                                widetDarkMode = DataStoreManager(applicationContext).getWidgetDarkMode()
                                widgetTransparency = DataStoreManager(applicationContext).getWidgetTransparency().toFloat() / 100f
                            }
                            navController.navigate("settings")
                        },
                        onStartService = {
                            startForegroundService()
                            foregroundServiceIsRunning = BatteryService.isRunning
                        },
                        fetchBatteryHistory = { deviceId, callback ->
                            getDeviceHistory(deviceId, callback)
                        },
                        refreshAll = { onComplete ->

                            Thread {
                                runBlocking {
                                    delay(500)
                                    loadAllData {
                                        onComplete()
                                    }
                                }
                            }.start()

                        }
                    )
                }

                composable("settings") { _ ->
                    SettingsScreen(initialShowPercent = widgetShowPercent, initialAlwaysFirst = widgetAlwaysFirst, initialDarkMode = widetDarkMode, initialTransparency = widgetTransparency, currentDeviceName = localDeviceName, registrationStatus = registrationStatus, offline = offline,
                        finish = {
                            navController.popBackStack()
                        },
                        saveAndFinish = { showPercent, alwaysFirst, darkMode, transparency ->
                            CoroutineScope(Dispatchers.IO).launch {
                                DataStoreManager(applicationContext).saveWidgetAlwaysFirst(alwaysFirst)
                                DataStoreManager(applicationContext).saveWidgetShowPercent(showPercent)
                                DataStoreManager(applicationContext).saveWidgetDarkMode(darkMode)
                                DataStoreManager(applicationContext).saveWidgetTransparency((transparency * 100f).toInt())
                                notifyWidgets(applicationContext)
                            }
                            navController.popBackStack()
                        },
                        changeDeviceName = { name, callback ->
                            renameDevice(name, callback)
                        },
                        onLogoutOnly = {
                            generalErrorMessage = null

                            if (registrationStatus) logout(false) {
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                    }
                                }
                            else {
                                CoroutineScope(Dispatchers.IO).launch {
                                    DataStoreManager(applicationContext).resetAll()
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                        }
                                    }
                                }
                            }

                        },
                        onLogoutAndDeleteData = {
                            logout(true) {
                                Log.d("MainActiviy", "Navigate to login")
                                generalErrorMessage = null
                                /*
                                CoroutineScope(Dispatchers.IO).launch {
                                    DataStoreManager(applicationContext).saveLocalId(0)
                                    dataStoreManager.saveToken("")
                                    dataStoreManager.saveAllDevices("[]")
                                    dataStoreManager.saveUUID("")
                                    displayUsername = ""
                                    localDeviceName = null
                                    registrationStatus = false
                                    uuid = null
                                    token = null
                                }*/
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }  // verhindert „Zurück zur Loginseite“
                                }
                            }
                        }
                    )
                }

                composable("replaceDevice") { _ ->
                    ReplaceDeviceScreen().DeviceLinkScreen(applicationContext,
                        deviceList,
                        generalLoadingState,
                        deviceListIsRefreshing,
                        selectedReplaceDeviceCanGenerateOtp,
                        otpInputWrong,
                        onRefresh = {
                            deviceListIsRefreshing = true
                            getDeviceList {}
                        }, onSendCode = { device ->
                            sendCodeToDevice(device, {}, {})
                        }, onConfirmInputCode = { code, device ->
                            generalLoadingState = true
                            confirmOtp(device, code,
                                success = {
                                    Log.d("MainActivity", "Input code: $code for device ${device.name}")
                                    navController.navigate("home") {
                                        getUsername {
                                            checkRegistrationStatus(
                                                success = {
                                                    popUpTo(0)
                                                }, fail = {}
                                            )
                                        }

                                    }
                                    generalLoadingState = false
                                },
                                fail = {}
                            )
                        }, onLinkNow = { device ->
                            generalLoadingState = true
                            Log.d("MainActivity", "Request instant link for ${device.name}")
                            instantLink(device,
                                success =  {
                                    Log.d("MainActivity", "Success link for ${device.name}")
                                    navController.navigate("home") {
                                        getUsername {
                                            checkRegistrationStatus(
                                                success = {
                                                    popUpTo(0)
                                                }, fail = {}
                                            )
                                        }

                                    }
                                    generalLoadingState = false
                                }, fail = {}
                            )
                        }, onSelectDevice = { device ->
                            selectedReplaceDeviceCanGenerateOtp = null
                            checkOtpCreatable(device, {}, {})
                        })
                }
            }
        }



        requestRequiredPermissions()
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

