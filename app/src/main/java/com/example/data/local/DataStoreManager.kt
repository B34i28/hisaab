package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_ledger_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val SELECTED_SHOP_ID_KEY = longPreferencesKey("selected_shop_id")
        val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
        val CUSTOM_PIN_KEY = stringPreferencesKey("custom_pin")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
    }

    val selectedShopId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_SHOP_ID_KEY]
    }

    suspend fun setSelectedShopId(shopId: Long?) {
        context.dataStore.edit { preferences ->
            if (shopId != null) {
                preferences[SELECTED_SHOP_ID_KEY] = shopId
            } else {
                preferences.remove(SELECTED_SHOP_ID_KEY)
            }
        }
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[APP_LOCK_ENABLED_KEY] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED_KEY] = enabled
        }
    }

    val customPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_PIN_KEY]
    }

    suspend fun setCustomPin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin != null) {
                preferences[CUSTOM_PIN_KEY] = pin
            } else {
                preferences.remove(CUSTOM_PIN_KEY)
            }
        }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE_KEY] ?: "EN"
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = languageCode
        }
    }
}
