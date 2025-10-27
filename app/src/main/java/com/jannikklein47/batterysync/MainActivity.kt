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
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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


        requestRequiredPermissions()


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




}

