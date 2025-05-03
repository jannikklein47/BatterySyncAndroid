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

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL


        Log.d("ChargingStatusReceiver", "Current charging status: $isCharging")

        CoroutineScope(Dispatchers.IO).launch {
            val token = DataStoreManager(context).getToken()
            val name = DataStoreManager(context).getDeviceName()
            if (!token.isNullOrEmpty() && !name.isNullOrEmpty()) {
                sendBatteryStatusToServer(isCharging, name, token)
            }
        }
    }

    private fun sendBatteryStatusToServer(isCharging: Boolean, name: String, token: String) {
        try {
            val url = URL("http://164.30.68.206:3000/battery?device=$name&chargingStatus=$isCharging")
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