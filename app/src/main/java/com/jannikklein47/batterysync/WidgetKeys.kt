package com.jannikklein47.batterysync

import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetKeys {
    val JSON = stringPreferencesKey("widget_data_class_json")
    val LOCAL_ID = stringPreferencesKey("widget_local_device_id")
    val LOCAL_NAME = stringPreferencesKey("widget_local_device_name")
    val ALWAYS_FIRST = stringPreferencesKey("widget_always_first")
    val SHOW_PERCENT = stringPreferencesKey("widget_show_percent")
    val DARK_MODE = stringPreferencesKey("widget_dark_mode")
    val LOCAL_BATTERY_JSON = stringPreferencesKey("widget_local_battery_json")
    val TRANSPARENCY = stringPreferencesKey("widget_transparency")
}