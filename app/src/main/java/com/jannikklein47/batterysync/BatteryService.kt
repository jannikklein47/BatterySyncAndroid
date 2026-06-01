package com.jannikklein47.batterysync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import androidx.core.net.toUri
import androidx.glance.appwidget.state.updateAppWidgetState
import org.json.JSONArray
import org.json.JSONObject

class BatteryService : Service() {
    private lateinit var batteryReceiver: BatteryStatusReceiver
    private lateinit var chargingReceiver: ChargingStatusReceiver
    private var timer: Timer? = null
    private var notificationTimer: Timer? = null
    private var timerStop: Boolean = false
    private var startInfoNotificationSent: Boolean = false

    @OptIn(kotlinx.serialization.InternalSerializationApi::class)
    @Serializable
    data class DevicePrediction(
        val targetName: String,
        val predictedZeroAt: String,
        val content: String,
        val title: String,
        val type: String,
        val url: String
    )

    companion object {
        var isRunning = false
    }

    fun parseJson(jsonString: String): List<DevicePrediction> {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return json.decodeFromString(jsonString)
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("BatteryService", "Service erstellt")

        // Receiver registrieren
        batteryReceiver = BatteryStatusReceiver()
        chargingReceiver = ChargingStatusReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
        registerReceiver(chargingReceiver, filter)

        // Notification für Foreground Service
        val notification = NotificationCompat.Builder(this, "battery_channel")
            .setContentTitle("Battery Sync aktiv")
            .setContentText("Überwacht deinen Akku")
            .build()

        startForeground(1, notification)

        startTimer(10000)
        startNotificationTimer()

        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startTimer(interval: Long) {
        if (!startInfoNotificationSent) {
            //sendNotificationSafe(applicationContext, "BatterySync läuft", "Die App wird dich nun stets mit aktuellen Daten versorgen.", false)
            startInfoNotificationSent = true
        }
        timer = Timer()
        timer?.schedule(object: TimerTask() {

            override fun run() {
                val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

                val isInteractive = pm.isInteractive // true = Bildschirm ist an

                var nextInterval: Long = 10000

                if (isInteractive) {
                    Log.d("BatteryService", "Timer execution long interval")
                    // Bildschirm ist an → Update ist sinnvoll
                    CoroutineScope(Dispatchers.IO).launch {
                        val token = DataStoreManager(applicationContext).getToken()
                        val uuid = DataStoreManager(applicationContext).getUuid()
                        if (!token.isNullOrEmpty() && !uuid.isNullOrEmpty()) {
                            getDevicesInfo(token, uuid)
                        } else if (!token.isNullOrEmpty()) {
                            getDevicesInfo(token)
                        }
                        nextInterval = 10000
                    }
                } else {
                    Log.d("BatteryService", "Timer executing Shorter timer interval set")
                    nextInterval = 10000
                }

                if (!timerStop) {
                    startTimer(nextInterval)
                }
            }
        }, interval)
    }

    fun startNotificationTimer() {
        notificationTimer = Timer()
        notificationTimer?.schedule(object: TimerTask() {

            override fun run() {
                Log.d("BatteryService Notification", "Notification Timer execution")

                CoroutineScope(Dispatchers.IO).launch {
                    val token = DataStoreManager(applicationContext).getToken()
                    val uuid = DataStoreManager(applicationContext).getUuid()
                    if (!token.isNullOrEmpty() && !uuid.isNullOrEmpty()) {
                        getDueNotifications(token,uuid)
                    }
                }

                if (!timerStop) {
                    startNotificationTimer()
                }
            }
        }, 30000)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerStop = true
        timer?.cancel() // Timer stoppen, wenn Service beendet wird
        notificationTimer?.cancel()
        unregisterReceiver(batteryReceiver)
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun getDueNotifications(token: String, uuid: String) {
        Thread {
            try {

                val url = URL("https://batterysync.de:3000/notification/due?uuid=$uuid")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Content-Type","application/json")
                connection.setRequestProperty("Authorization", token)

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("BatteryService Notification GET", "Antwort: $response")

                    var notifications = parseJson(response)

                    for (noti in notifications) {
                        if (noti.type.uppercase() == "CONTENT") {
                            sendNotificationSafe(applicationContext, noti.title, noti.content, true, noti.url)

                        } else {
                            sendNotificationSafe(applicationContext, "Lade dein ${noti.targetName} auf!", "Dein Gerät wird in weniger als 2 Stunden leer sein. Lade es auf, damit es nicht aus geht.", true, noti.url)

                        }
                    }

                } else {
                    Log.e("BatteryService Notification GET", "Serverfehler: $responseCode")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e("BatteryService Notification GET", "Fehler beim Senden: ${e}")
            }
        }.start()
    }

    fun getDevicesInfo(token: String) {
        Thread {
            try {
                val url = URL("https://batterysync.de:3000/battery")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Contenty-Type","application/json")
                connection.setRequestProperty("Authorization", token)

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("BatteryService GET", "Antwort: $response")
                    CoroutineScope(Dispatchers.IO).launch {
                        DataStoreManager(applicationContext).saveAllDevices(response)
                        notifyWidgets(applicationContext)
                    }

                } else {
                    Log.e("BatteryService GET", "Serverfehler: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("BatteryService GET", "Fehler beim Senden: ${e}")
            }
        }.start()
    }

    fun getDevicesInfo(token: String, uuid: String) {
        Thread {
            try {
                val url = URL("https://batterysync.de:3000/battery/secure?uuid=$uuid")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Contenty-Type","application/json")
                connection.setRequestProperty("Authorization", token)

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("BatteryService GET SECURE", "Antwort: $response")
                    CoroutineScope(Dispatchers.IO).launch {
                        val oldData = DataStoreManager(applicationContext).getAllDevices()
                        DataStoreManager(applicationContext).saveAllDevices(response)
                        if (different(oldData ?: "[]", response) || batteryStatusChanged()) {
                            Log.d("BatteryService", "Device Data Changed")
                            notifyWidgets(applicationContext)
                        } else Log.d("BatteryService", "Device Data did not change")
                    }

                } else {
                    Log.e("BatteryService GET SECURE", "Serverfehler: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("BatteryService GET SECURE", "Fehler beim Senden: ${e}")
                CoroutineScope(Dispatchers.IO).launch {
                    if (batteryStatusChanged()) {
                        Log.d("BatteryService", "Local State has changed")
                        notifyWidgets(applicationContext)
                    } else {
                        Log.d("BatteryService", "Local State did not change")
                    }
                }
            }
        }.start()
    }

    fun sendNotification(context: Context, title: String, message: String, loud: Boolean, link: String) {
        var channelId = "notiChannel"
        if (loud) channelId = "notiChannelLoud"

        val channel = NotificationChannel(
            channelId,
            "General Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "App notifications"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        var builder: NotificationCompat.Builder

        if (link.isEmpty()){
            // 2️⃣ Build the notification

            builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(message) // This is what shows when collapsed
                // --- THIS IS THE FIX ---
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(message)) // This shows when expanded
                // -----------------------
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
        } else {
            // 2️⃣ Build the notification
            val browserIntent = Intent(Intent.ACTION_VIEW, link.toUri())

            // 2. Wrap it in a PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                context,
                0, // Request code
                browserIntent,
                // Use IMMUTABLE for security on Android 12+
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(message) // This is what shows when collapsed
                // --- THIS IS THE FIX ---
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(message)) // This shows when expanded
                // -----------------------
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        }

        // 3️⃣ Check permission (Android 13+)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            try {
                Toast.makeText(
                    context,
                    "Notification permission not granted. Please enable it in settings.",
                    Toast.LENGTH_LONG
                ).show()
                return
            } catch (e: Exception) {
                Log.e("BatteryService", "Could not toast notification permission hint")
            }

        }


        // 4️⃣ Send notification
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun sendNotificationSafe(context: Context, title: String, message: String, loud: Boolean, link: String) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            sendNotification(context, title, message, loud, link) // the function we wrote before
        }
    }

    fun different(string1: String, string2: String): Boolean {
        try {
            val json1 = JSONArray(string1)
            val json2 = JSONArray(string2)

            if (json1.length() != json2.length()) return true

            for (i in 0 until json1.length()) {
                val j1 = json1.getJSONObject(i)
                val j2 = json2.getJSONObject(i)

                if (j1.getString("name") != j2.getString("name")) return true
                if ((j1.getDouble("battery" ) * 100f).toInt() !=  (j2.getDouble("battery")*100f).toInt()) return true
                if (j1.getBoolean("chargingStatus") != j2.getBoolean("chargingStatus")) return true
                if (j1.getBoolean("isPluggedIn") != j2.getBoolean("isPluggedIn")) return true
            }

            return false
        } catch (e: Exception) {
            Log.e("BatteryService", "Error occured in different: $e, ${e.printStackTrace()}")
            return true
        }
    }

    suspend fun batteryStatusChanged(): Boolean {

        val lastStatusString = DataStoreManager(applicationContext).getLastLocalBatteryStatus()
        if (lastStatusString == "{}" || lastStatusString.isEmpty()) {
            Log.d("BatteryService", "No local battery state available")
            return false
        }
        try {
            val lastStatus = JSONObject(lastStatusString)

            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                applicationContext.registerReceiver(null, ifilter)
            }

            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = (level / scale.toDouble())

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING

                val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                val isPluggedIn = chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                        chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                        chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

                // Only save the latest state after checking
                DataStoreManager(applicationContext).saveLatestLocalBatteryStatus("{battery:${batteryPct},chargingStatus:${isCharging},isPluggedIn:${isPluggedIn}}")

                if (lastStatus.getDouble("battery") != batteryPct) return true
                if (lastStatus.getBoolean("chargingStatus") != isCharging) return true
                if (lastStatus.getBoolean("isPluggedIn") != isPluggedIn) return true
            }

            return false
        } catch (e: Exception) {
            Log.e("BatteryService", "Error while trying to check if the battery status changed: $e, ${e.printStackTrace()}")
            return false
        }

    }



}