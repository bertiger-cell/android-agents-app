package com.agents.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agents.app.models.DEFAULT_ARCHITECT_PROMPT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.architectDataStore by preferencesDataStore(name = "architect_config")

private val ARCHITECT_SYSTEM_PROMPT = stringPreferencesKey("architect_system_prompt")

class ArchitectConfigRepository(private val context: Context) {

    val architectSystemPrompt: Flow<String> = context.architectDataStore.data
        .map { prefs ->
            prefs[ARCHITECT_SYSTEM_PROMPT] ?: DEFAULT_ARCHITECT_PROMPT
        }

    suspend fun updateArchitectSystemPrompt(newPrompt: String) {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_SYSTEM_PROMPT] = newPrompt
        }
    }

    suspend fun resetArchitectSystemPrompt() {
        context.architectDataStore.edit { prefs ->
            prefs[ARCHITECT_SYSTEM_PROMPT] = DEFAULT_ARCHITECT_PROMPT
        }
    }
}
