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

class ChargingStatusReceiver : BroadcastReceiver() {

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
            val uuid = DataStoreManager(context).getUuid()
            if (!token.isNullOrEmpty() && !uuid.isNullOrEmpty()) {
                sendBatteryStatusToServer(uuid, batteryPct, isCharging, isPluggedIn, token)
            }
        }
    }

    private fun sendBatteryStatusToServer(uuid: String, batteryLevel: Double, chargingStatus: Boolean, isPluggedIn: Boolean, token: String) {
        try {
            val url = URL("https://batterysync.de:3000/battery/secure?uuid=$uuid&battery=$batteryLevel&chargingStatus=$chargingStatus&isPluggedIn=$isPluggedIn")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", token)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Content-Type","application/json")


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