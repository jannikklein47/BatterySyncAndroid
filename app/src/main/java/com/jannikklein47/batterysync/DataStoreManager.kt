package com.jannikklein47.batterysync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val NAME_KEY = stringPreferencesKey("name")
        private val DEVICES_KEY = stringPreferencesKey("devices")
        private val UUID_KEY = stringPreferencesKey("uuid")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val WIDGET_SHOW_PERCENT_KEY = stringPreferencesKey("widget_show_percent")
        private val WIDGET_ALWAYS_FIRST_KEY = stringPreferencesKey("widget_always_first")
        private val LOCAL_DEVICE_KEY = stringPreferencesKey("localDevice")
        private val LATEST_BATTERY_STATUS_KEY = stringPreferencesKey("latest_battery_status")
        private val WIDGET_DARK_MODE = stringPreferencesKey("widget_dark_mode")
        private val WIDGET_TRANSPARENCY = stringPreferencesKey("widget_transparency")
    }

    suspend fun resetAll() {
        Log.d("DataStoreManager", "Reset all keys")
        context.dataStore.edit { prefs ->
            val map = prefs.asMap()
            map.forEach { (key, value) ->
                prefs[stringPreferencesKey(key.toString())] = ""
            }
        }
    }

    suspend fun saveToken(token: String) {
        Log.d("DataStoreManager", "Update saved token: $token")
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun saveUUID(uuid: String) {
        Log.d("DataStoreManager", "Save uuid")
        context.dataStore.edit { prefs ->
            prefs[UUID_KEY] = uuid
        }
    }

    suspend fun getToken(): String? {
        val result : String? = context.dataStore.data
            .map { prefs -> prefs[TOKEN_KEY] }
            .first()

        Log.d("DataStoreManager", "Get token from store: $result")

        return context.dataStore.data
            .map { prefs -> prefs[TOKEN_KEY] }
            .first()
    }

    suspend fun getUuid(): String? {
        Log.d("DataStoreManager", "Get uuid from store")

        return context.dataStore.data
            .map { prefs -> prefs[UUID_KEY] }
            .first()
    }

    suspend fun saveDeviceName(name: String) {
        Log.d("DataStoreManager", "Update saved device name: $name")

        context.dataStore.edit { prefs ->
            prefs[NAME_KEY] = name
        }
    }

    suspend fun getDeviceName() : String? {
        val result : String? = context.dataStore.data
            .map { prefs -> prefs[NAME_KEY] }
            .first()

        Log.d("DataStoreManager", "Get name from store: $result")

        return context.dataStore.data
            .map { prefs -> prefs[NAME_KEY] }
            .first()
    }

    suspend fun getAllDevices() : String? {
        val result : String? = context.dataStore.data
            .map { prefs -> prefs[DEVICES_KEY] }
            .first()

        Log.d("DataStoreManager", "Get all devices from store: $result")

        return context.dataStore.data
            .map { prefs -> prefs[DEVICES_KEY] }
            .first()
    }

    suspend fun saveAllDevices(json: String) {
        Log.d("DataStoreManager", "Update all devices: $json")

        context.dataStore.edit { prefs ->
            prefs[DEVICES_KEY] = json
        }
    }

    suspend fun saveLocalId(id: Int) {
        Log.d("DataStoreManager", "Update local device id: $id")

        context.dataStore.edit { prefs ->
            prefs[LOCAL_DEVICE_KEY] = id.toString()
        }
    }

    suspend fun getLocalId() : Int {
        Log.d("DataStoreManager" , "Get local device id from store")

        val result = context.dataStore.data.map { prefs -> prefs[LOCAL_DEVICE_KEY] }.first()

        return try {
            result?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun saveWidgetShowPercent(bool: Boolean) {
        Log.d("DataStoreManager", "Update show percent in widget: $bool")

        context.dataStore.edit { prefs ->
            prefs[WIDGET_SHOW_PERCENT_KEY] = bool.toString()
        }
    }

    suspend fun getWidgetShowPercent() : Boolean {
        var result = context.dataStore.data
            .map { prefs -> prefs[WIDGET_SHOW_PERCENT_KEY] }
            .first()

        if (result.isNullOrEmpty()) {
            saveWidgetShowPercent(true) // If there is no value set, the default should be true
            result = true.toString()
        }

        Log.d("DataStoreManager", "Get widget show percent from store: $result")

        Log.d("DataStoreManager", "Value: ${result}")

        return result == true.toString()
    }

    suspend fun saveWidgetAlwaysFirst(bool: Boolean) {
        Log.d("DataStoreManager", "Update always first in widget: $bool")

        context.dataStore.edit { prefs ->
            prefs[WIDGET_ALWAYS_FIRST_KEY] = bool.toString()
        }
    }

    suspend fun getWidgetAlwaysFirst() : Boolean {
        var result = context.dataStore.data
            .map { prefs -> prefs[WIDGET_ALWAYS_FIRST_KEY] }
            .first()

        if (result.isNullOrEmpty()) {
            saveWidgetAlwaysFirst(true) // If there is no value set, the default should be true
            result = true.toString()
        }

        Log.d("DataStoreManager", "Get widget always first from store: $result")

        Log.d("DataStoreManager", "Value: ${result}")

        return result == true.toString()
    }

    suspend fun saveLatestLocalBatteryStatus(status: String) {
        Log.d("DataStoreManager", "Update latest battery status")

        context.dataStore.edit { prefs ->
            prefs[LATEST_BATTERY_STATUS_KEY] = status
        }
    }

    suspend fun getLastLocalBatteryStatus(): String {
        Log.d("DataStoreManager", "Get last battery status from store")

        return context.dataStore.data.map { prefs -> prefs[LATEST_BATTERY_STATUS_KEY] }.first() ?: "{}"
    }

    suspend fun saveWidgetDarkMode(bool: Boolean?) {
        Log.d("DataStoreManager", "Update dark mode in widget: $bool")

        context.dataStore.edit { prefs ->
            prefs[WIDGET_DARK_MODE] = bool.toString()
        }
    }

    suspend fun getWidgetDarkMode() : Boolean? {
        val result = context.dataStore.data
            .map { prefs -> prefs[WIDGET_DARK_MODE] }
            .first()

        Log.d("DataStoreManager", "Get widget dark mode: $result")

        return if (result === "null" || result.isNullOrEmpty()) null else result.toBoolean()
    }

    suspend fun saveWidgetTransparency(value: Int) {
        Log.d("DataStoreManager", "Update transparency in widget: $value")

        context.dataStore.edit { prefs ->
            prefs[WIDGET_TRANSPARENCY] = value.toString()
        }
    }

    suspend fun getWidgetTransparency() : Int {
        val result = context.dataStore.data
            .map { prefs -> prefs[WIDGET_TRANSPARENCY] }
            .first()

        Log.d("DataStoreManager", "Get widget transparency: $result")

        return try {
            result?.toInt() ?: 94
        } catch (e: Exception) {
            94
        }
    }

    suspend fun saveUserName(name: String) {
        Log.d("DataStoreManager", "Save user name: $name")

        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
        }
    }

    suspend fun getUserName() : String? {
        val result = context.dataStore.data
            .map { prefs -> prefs[USER_NAME] }
            .first()

        Log.d("DataStoreManager", "Get user name: $result")

        return result
    }
}

