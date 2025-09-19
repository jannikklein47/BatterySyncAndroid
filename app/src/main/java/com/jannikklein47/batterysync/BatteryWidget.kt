package com.jannikklein47.batterysync

import android.appwidget.*
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.*


class BatteryWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun getBatteryDrawableForLevel(level: Double, chargingStatus: Boolean): Int {

            if (chargingStatus) {
                return when {
                    level >= 0.90 -> R.drawable.battery_fill_100_charging
                    level >= 0.80 -> R.drawable.battery_fill_90_charging
                    level >= 0.70 -> R.drawable.battery_fill_80_charging
                    level >= 0.60 -> R.drawable.battery_fill_70_charging
                    level >= 0.50 -> R.drawable.battery_fill_60_charging
                    level >= 0.40 -> R.drawable.battery_fill_50_charging
                    level >= 0.31 -> R.drawable.battery_fill_40_charging
                    level >= 0.20 -> R.drawable.battery_fill_30_charging
                    level >= 0.16 -> R.drawable.battery_fill_20_charging
                    level >= 0.10 -> R.drawable.battery_fill_15_charging
                    level >= 0.05 -> R.drawable.battery_fill_10_charging
                    else        -> R.drawable.battery_fill_empty
                }
            } else {
                return when {
                    level >= 0.90 -> R.drawable.battery_fill_100
                    level >= 0.80 -> R.drawable.battery_fill_90
                    level >= 0.70 -> R.drawable.battery_fill_80
                    level >= 0.60 -> R.drawable.battery_fill_70
                    level >= 0.50 -> R.drawable.battery_fill_60
                    level >= 0.40 -> R.drawable.battery_fill_50
                    level >= 0.31 -> R.drawable.battery_fill_40
                    level >= 0.20 -> R.drawable.battery_fill_30
                    level >= 0.16 -> R.drawable.battery_fill_20
                    level >= 0.10 -> R.drawable.battery_fill_15
                    level >= 0.05 -> R.drawable.battery_fill_10
                    else        -> R.drawable.battery_fill_empty
                }
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val mainView = RemoteViews(context.packageName, R.layout.widget_battery)

            mainView.removeAllViews(R.id.widget_layout)

            CoroutineScope(Dispatchers.IO).launch {

                var deviceInfoString = DataStoreManager(context).getAllDevices()
                var deviceInfoJson : JSONArray
                if (deviceInfoString == null) deviceInfoString = ""
                deviceInfoJson = JSONArray(deviceInfoString)
                var devices : List<Device> = listOf()

                Log.d("BatteryWidget", "Loaded: $deviceInfoJson")
                var i = 0
                while (i < deviceInfoJson.length()) {

                    try {
                        if (deviceInfoJson.getJSONObject(i).getBoolean("isShown")) {
                            devices = devices.plus(
                                Device(deviceInfoJson.getJSONObject(i).getString("name"),
                                    deviceInfoJson.getJSONObject(i).getDouble("battery"),
                                    deviceInfoJson.getJSONObject(i).getBoolean("chargingStatus"),
                                    deviceInfoJson.getJSONObject(i).getBoolean("isPluggedIn"))
                            )
                        }
                    } catch (e: JSONException) {
                        devices = devices.plus(Device("JSON ERROR", 0.0, false, false))
                    }


                    i = i + 1
                }

                Log.d("BatteryWidget", "Processed: $deviceInfoJson")

                if (devices.size < 4) {
                    var i = devices.size
                    while (i < 4) {
                        devices = devices.plus(Device("",-1.0, false, false))
                        i = i + 1
                    }
                }

                // Für jedes Gerät eine Battery-View hinzufügen
                for (device: Device in devices) {
                    val batteryView = RemoteViews(context.packageName, R.layout.battery_entry)

                    // Batterieprozentsatz setzen
                    if (device.battery < 0) {
                        batteryView.setTextViewText(R.id.battery_text, "")
                        val outline = R.drawable.battery_outline_inactive
                        val nub = R.drawable.battery_nub_inactive

                        batteryView.setImageViewResource(R.id.battery_outline, outline)
                        batteryView.setImageViewResource(R.id.battery_nub, nub)
                        batteryView.setImageViewResource(R.id.boltIcon, R.drawable.baseline_bolt_24_gone)
                    } else {
                        batteryView.setTextViewText(R.id.battery_text, (if (device.chargingStatus || device.isPluggedIn) " " else "") +"${(device.battery * 100).toInt()}" + (if (device.chargingStatus || (device.isPluggedIn && device.battery == 1.0)) "" else if (device.isPluggedIn) " " else "  "))
                        batteryView.setImageViewResource(R.id.boltIcon, if (device.chargingStatus) R.drawable.baseline_bolt_24 else if (device.isPluggedIn) R.drawable.plug_svgrepo_com__1_ else R.drawable.baseline_bolt_24_gone)
                    }

                    batteryView.setTextViewText(R.id.battery_device_name, device.name)

                    // Füllstand anpassen
                    val fillDrawable = getBatteryDrawableForLevel(device.battery, device.chargingStatus)
                    batteryView.setImageViewResource(R.id.battery_level, fillDrawable)

                    // Füge die BatteryView zur horizontalen LinearLayout hinzu
                    mainView.addView(R.id.widget_layout, batteryView)
                }

                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, mainView)
            }


        }
    }
}

class Device(val name:String, val battery:Double,val chargingStatus: Boolean, val isPluggedIn:Boolean) {

}
