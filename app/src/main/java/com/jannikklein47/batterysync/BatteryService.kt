package com.jannikklein47.batterysync

import android.app.Service
import android.content.*
import android.os.IBinder
import androidx.*
import androidx.core.app.NotificationCompat

class BatteryService : Service() {
    private lateinit var batteryReceiver: BatteryStatusReceiver

    override fun onCreate() {
        super.onCreate()

        // Receiver registrieren
        batteryReceiver = BatteryStatusReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        // Notification für Foreground Service
        val notification = NotificationCompat.Builder(this, "battery_channel")
            .setContentTitle("Battery Sync aktiv")
            .setContentText("Überwacht deinen Akku")
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
