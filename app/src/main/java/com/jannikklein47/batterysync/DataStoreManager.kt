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
}

