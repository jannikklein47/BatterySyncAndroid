package com.jannikklein47.batterysync

import android.app.NotificationChannel
import android.app.NotificationManager
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.jannikklein47.batterysync.ui.theme.BatterySyncTheme

class MainActivity : ComponentActivity() {

    private val batteryReceiver = BatteryStatusReceiver()

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
        unregisterReceiver(batteryReceiver)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "battery_channel",
            "Battery Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

    }

}