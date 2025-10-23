package com.jannikklein47.batterysync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
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

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        val isPluggedIn = chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

        Log.d("BatteryReceiver", "Akkustand: $batteryPct")

        CoroutineScope(Dispatchers.IO).launch {
            val token = DataStoreManager(context).getToken()
            val name = DataStoreManager(context).getDeviceName()
            if (!token.isNullOrEmpty() && !name.isNullOrEmpty()) {
                sendBatteryStatusToServer(batteryPct, isCharging, isPluggedIn, name, token)
            }
        }
    }

    private fun sendBatteryStatusToServer(batteryLevel: Double, chargingStatus: Boolean, isPluggedIn: Boolean, name: String, token: String) {
        try {
            val url = URL("https://batterysync.chickenkiller.com:3000/battery?device=$name&battery=$batteryLevel&chargingStatus=$chargingStatus&isPluggedIn=$isPluggedIn")
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
