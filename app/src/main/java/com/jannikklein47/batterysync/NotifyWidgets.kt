package com.jannikklein47.batterysync

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

suspend fun notifyWidgets(context: Context) {
    /*
    val manager = AppWidgetManager.getInstance(context)
    val widgetIds = manager.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java))
    for (widgetId in widgetIds) {
        Log.d("BatteryService", "Update Widget $widgetId")
        BatteryWidget.updateWidget(context, manager, widgetId)
    }*/

    val manager = GlanceAppWidgetManager(context)

    // 2. Fetch the unique GlanceIds for EVERY instance of this widget on the home screen
    val glanceIds = manager.getGlanceIds(DeviceWidget::class.java)

    // 3. Loop through them to apply your data updates
    for (glanceId in glanceIds) {
        // Use the glanceId here to update state or trigger redraws
        updateAppWidgetState(context, glanceId) { prefs ->
            val json = DataStoreManager(context).getAllDevices() ?: "{}"
            val localId = DataStoreManager(context).getLocalId()
            val localName = DataStoreManager(context).getDeviceName() ?: ""
            val alwaysFirst = DataStoreManager(context).getWidgetAlwaysFirst()
            val showPercent = DataStoreManager(context).getWidgetShowPercent()
            val darkMode = DataStoreManager(context).getWidgetDarkMode()
            val localBatteryJSON = DataStoreManager(context).getLastLocalBatteryStatus()
            val transparency = DataStoreManager(context).getWidgetTransparency()
            prefs[WidgetKeys.JSON] = json
            prefs[WidgetKeys.LOCAL_ID] = localId.toString()
            prefs[WidgetKeys.LOCAL_NAME] = localName
            prefs[WidgetKeys.ALWAYS_FIRST] = alwaysFirst.toString()
            prefs[WidgetKeys.SHOW_PERCENT] = showPercent.toString()
            prefs[WidgetKeys.DARK_MODE] = darkMode.toString()
            prefs[WidgetKeys.LOCAL_BATTERY_JSON] = localBatteryJSON
            prefs[WidgetKeys.TRANSPARENCY] = transparency.toString()
        }

        DeviceWidget().update(context, glanceId)
    }



    Log.d("BatteryService", "update new widget")
}