package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hisaab_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val SELECTED_SHOP_ID_KEY = longPreferencesKey("selected_shop_id")
        val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
        val FINGERPRINT_ENABLED_KEY = booleanPreferencesKey("fingerprint_enabled")
        val AUTO_LOCK_TIMEOUT_KEY = intPreferencesKey("auto_lock_timeout_seconds")
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
            if (!enabled) {
                preferences[FINGERPRINT_ENABLED_KEY] = false
            }
        }
    }

    val isFingerprintEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FINGERPRINT_ENABLED_KEY] ?: false
    }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FINGERPRINT_ENABLED_KEY] = enabled
        }
    }

    val autoLockTimeoutSeconds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUTO_LOCK_TIMEOUT_KEY] ?: 30
    }

    suspend fun setAutoLockTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_LOCK_TIMEOUT_KEY] = seconds
        }
    }

    val isSecurityLoaded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val pinExists = preferences[CUSTOM_PIN_KEY] != null
        val lockEnabled = preferences[APP_LOCK_ENABLED_KEY] ?: false
        android.util.Log.d("HisaabSecurity", "DataStore security loaded from disk. AppLockEnabled: $lockEnabled, CustomPinExists: $pinExists")
        true
    }

    val customPin: Flow<String?> = context.dataStore.data.map { preferences ->
        val pin = preferences[CUSTOM_PIN_KEY]
        android.util.Log.d("HisaabSecurity", "DataStore customPin emitted. Hash exists: ${pin != null}")
        pin
    }

    suspend fun setCustomPin(plainPin: String?) {
        context.dataStore.edit { preferences ->
            if (!plainPin.isNullOrEmpty()) {
                val newHash = PinHasher.createStoredPinHash(plainPin)
                preferences[CUSTOM_PIN_KEY] = newHash
                android.util.Log.d("HisaabSecurity", "setCustomPin persisted to DataStore disk. PIN length: ${plainPin.length}")
            } else {
                preferences.remove(CUSTOM_PIN_KEY)
                android.util.Log.d("HisaabSecurity", "CUSTOM_PIN_KEY removed from DataStore disk.")
            }
        }
    }

    suspend fun clearAllSecurityAndSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(APP_LOCK_ENABLED_KEY)
            preferences.remove(FINGERPRINT_ENABLED_KEY)
            preferences.remove(CUSTOM_PIN_KEY)
            preferences.remove(AUTO_LOCK_TIMEOUT_KEY)
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
