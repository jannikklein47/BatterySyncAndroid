package com.jannikklein47.batterysync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class BatteryStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = (level / scale.toDouble())

        Log.d("BatteryReceiver", "Akkustand: $batteryPct")

        CoroutineScope(Dispatchers.IO).launch {
            val token = DataStoreManager(context).getToken()
            val name = DataStoreManager(context).getDeviceName()
            if (!token.isNullOrEmpty() && !name.isNullOrEmpty()) {
                sendBatteryStatusToServer(batteryPct, name, token)
            }
        }
    }

    private fun sendBatteryStatusToServer(batteryLevel: Double, name: String, token: String) {
        try {
            val url = URL("http://10.0.2.2:3000/battery?device=$name&battery=$batteryLevel")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", token)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Contenty-Type","application/json")


            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("BatterySender", "Antwort: $response")
            } else {
                Log.e("BatterySender", "Serverfehler: $responseCode")
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e("BatterySender", "Fehler beim Senden: ${e.localizedMessage}")
        }
    }
}
