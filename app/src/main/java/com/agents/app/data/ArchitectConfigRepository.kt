package com.agents.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agents.app.models.DEFAULT_ARCHITECT_PROMPT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.architectDataStore by preferencesDataStore(name = "architect_config")

private val ARCHITECT_SYSTEM_PROMPT = stringPreferencesKey("architect_system_prompt")
private val ARCHITECT_ENABLED = booleanPreferencesKey("architect_enabled")
private val ARCHITECT_PROVIDER = stringPreferencesKey("architect_provider")
private val ARCHITECT_MODEL = stringPreferencesKey("architect_model")

class ArchitectConfigRepository(private val context: Context) {

    val architectSystemPrompt: Flow<String> = context.architectDataStore.data
        .map { prefs ->
            prefs[ARCHITECT_SYSTEM_PROMPT] ?: DEFAULT_ARCHITECT_PROMPT
        }

    val isArchitectEnabled: Flow<Boolean> = context.architectDataStore.data
        .map { prefs ->
            prefs[ARCHITECT_ENABLED] ?: false
        }

    val architectProvider: Flow<String> = context.architectDataStore.data
        .map { prefs ->
            prefs[ARCHITECT_PROVIDER] ?: "openrouter"
        }

    val architectModel: Flow<String> = context.architectDataStore.data
        .map { prefs ->
            prefs[ARCHITECT_MODEL] ?: "gpt-4o"
        }

    suspend fun updateArchitectSystemPrompt(newPrompt: String) {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_SYSTEM_PROMPT] = newPrompt
        }
    }

    suspend fun setArchitectEnabled(enabled: Boolean) {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_ENABLED] = enabled
        }
    }

    suspend fun updateArchitectProvider(provider: String) {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_PROVIDER] = provider
        }
    }

    suspend fun updateArchitectModel(model: String) {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_MODEL] = model
        }
    }

    suspend fun resetArchitectSystemPrompt() {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_SYSTEM_PROMPT] = DEFAULT_ARCHITECT_PROMPT
        }
    }
}
