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
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import kotlinx.serialization.*
import kotlinx.serialization.json.*

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
    )

    companion object {
        var isRunning = false
    }

    fun parseJson(json: String): List<DevicePrediction> {
        return Json.decodeFromString(json)
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

        // Timer starten (alle 30 Sekunden)
        startTimer()
        startNotificationTimer()

        isRunning = true
    }

    fun startTimer() {
        if (!startInfoNotificationSent) {
            //sendNotificationSafe(applicationContext, "BatterySync läuft", "Die App wird dich nun stets mit aktuellen Daten versorgen.", false)
            startInfoNotificationSent = true
        }
        timer = Timer()
        timer?.schedule(object: TimerTask() {

            override fun run() {
                Log.d("BatteryService", "Timer execution")
                val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

                val isInteractive = pm.isInteractive // true = Bildschirm ist an

                if (isInteractive) {
                    // Bildschirm ist an → Update ist sinnvoll
                    CoroutineScope(Dispatchers.IO).launch {
                        val token = DataStoreManager(applicationContext).getToken()
                        val uuid = DataStoreManager(applicationContext).getUuid()
                        if (!token.isNullOrEmpty() && !uuid.isNullOrEmpty()) {
                            getDevicesInfo(token, uuid)
                        } else if (!token.isNullOrEmpty()) {
                            getDevicesInfo(token)
                        }
                    }
                }

                if (!timerStop) {
                    startTimer()
                }
            }
        }, 10000)
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
                            sendNotificationSafe(applicationContext, noti.title, noti.content, true)

                        } else {
                            sendNotificationSafe(applicationContext, "Lade dein ${noti.targetName} auf!", "Dein Gerät wird in weniger als 2 Stunden leer sein. Lade es auf, damit es nicht aus geht.", true)

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
                        DataStoreManager(applicationContext).saveAllDevices(response)
                        notifyWidgets(applicationContext)
                    }

                } else {
                    Log.e("BatteryService GET SECURE", "Serverfehler: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("BatteryService GET SECURE", "Fehler beim Senden: ${e}")
            }
        }.start()
    }


    fun notifyWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java))
        for (widgetId in widgetIds) {
            Log.d("BatteryService", "Update Widget $widgetId")
            BatteryWidget.updateWidget(context, manager, widgetId)
        }
    }

    fun sendNotification(context: Context, title: String, message: String, loud: Boolean) {
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


        // 2️⃣ Build the notification
        val intent = Intent(context, context::class.java) // Opens the same activity
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

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

    fun sendNotificationSafe(context: Context, title: String, message: String, loud: Boolean) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            sendNotification(context, title, message, loud) // the function we wrote before
        }
    }



}