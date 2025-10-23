package com.jannikklein47.batterysync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jannikklein47.batterysync.ui.theme.BatterySyncTheme
import android.Manifest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        createNotificationChannel()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatterySyncTheme {
                val loginViewModel = remember {
                    LoginViewModel(applicationContext)
                }

                LoginScreen(viewModel = loginViewModel)
            }
        }

        //val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        //registerReceiver(batteryReceiver, filter);
    }

    override fun onDestroy() {
        super.onDestroy()
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

}

