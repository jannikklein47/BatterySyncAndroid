package com.jannikklein47.batterysync

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask

class BatteryService : Service() {
    private lateinit var batteryReceiver: BatteryStatusReceiver
    private lateinit var chargingReceiver: ChargingStatusReceiver
    private var timer: Timer? = null
    private var timerStop: Boolean = false


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
    }

    fun startTimer() {
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
                        if (token != null) {
                            getDevicesInfo(token)
                        }
                    }
                } else Log.d("BatteryService", "Timer skipped execution")

                if (!timerStop) {
                    startTimer()
                }
            }
        }, 10000)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerStop = true
        timer?.cancel() // Timer stoppen, wenn Service beendet wird
        unregisterReceiver(batteryReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun getDevicesInfo(token: String) {
        Thread {
            try {
                val url = URL("http://192.168.0.119:3000/battery")

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

    fun notifyWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java))
        for (widgetId in widgetIds) {
            Log.d("BatteryService", "Update Widget $widgetId")
            BatteryWidget.updateWidget(context, manager, widgetId)
        }
    }

}