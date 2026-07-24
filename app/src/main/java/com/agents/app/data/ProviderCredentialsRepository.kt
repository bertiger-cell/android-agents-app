package com.agents.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "provider_credentials")

data class ProviderCredentials(
    val openRouterKey: String = "",
    val zenKey: String = "",
    val ollamaBaseUrl: String = "http://127.0.0.1:11434",
    val ollamaApiKey: String = ""
)

class ProviderCredentialsRepository(private val context: Context) {

    private object Keys {
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val ZEN_API_KEY = stringPreferencesKey("opencode_zen_api_key")
        val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        val OLLAMA_API_KEY = stringPreferencesKey("ollama_api_key")
    }

    val credentials: Flow<ProviderCredentials> = context.dataStore.data.map { prefs ->
        ProviderCredentials(
            openRouterKey = prefs[Keys.OPENROUTER_API_KEY] ?: "",
            zenKey = prefs[Keys.ZEN_API_KEY] ?: "",
            ollamaBaseUrl = prefs[Keys.OLLAMA_BASE_URL] ?: "http://127.0.0.1:11434",
            ollamaApiKey = prefs[Keys.OLLAMA_API_KEY] ?: ""
        )
    }

    suspend fun updateOpenRouterKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENROUTER_API_KEY] = key
        }
    }

    suspend fun updateZenKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ZEN_API_KEY] = key
        }
    }

    suspend fun updateOllamaBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OLLAMA_BASE_URL] = url
        }
    }

    suspend fun updateOllamaApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OLLAMA_API_KEY] = key
        }
    }
}
