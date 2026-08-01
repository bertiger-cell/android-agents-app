package com.agents.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agents.app.models.AIProvider
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AgentTemplate(
    val name: String,
    val description: String = "",
    val systemPrompt: String = "You are a helpful AI assistant.",
    val provider: AIProvider = AIProvider.OPENROUTER,
    val model: String = "gpt-4o",
    val temperature: Float = 0.7f
)

fun getDefaultAgentTemplates(): List<AgentTemplate> = listOf(
    AgentTemplate(
        name = "Code Assistant",
        description = "Hilft beim Programmieren, Debugging und Code-Review",
        systemPrompt = "Du bist ein erfahrener Software-Entwickler. Hilf beim Programmieren, erklaere Code, finde Bugs und schlage Verbesserungen vor. Antworte praezise und gib konkrete Code-Beispiele.",
        provider = AIProvider.OPENROUTER,
        model = "openai/gpt-4o",
        temperature = 0.3f
    ),
    AgentTemplate(
        name = "Creative Writer",
        description = "Kreatives Schreiben, Geschichten, Artikel",
        systemPrompt = "Du bist ein kreativer Autor. Schreibe ansprechende Texte, Geschichten oder Artikel. Sei bildhaft, inspirierend und passe den Stil an den Kontext an.",
        provider = AIProvider.OPENROUTER,
        model = "openai/gpt-4o",
        temperature = 0.9f
    ),
    AgentTemplate(
        name = "Research Mode",
        description = "Recherchiert Themen und fasst Ergebnisse zusammen",
        systemPrompt = "Du bist ein fleissiger Researcher. Recherchiere Themen gruendlich, fasse Zusammen, zitiere Quellen und strukturiere die Ergebnisse uebersichtlich.",
        provider = AIProvider.OPENROUTER,
        model = "openai/gpt-4o",
        temperature = 0.5f
    ),
    AgentTemplate(
        name = "Translator",
        description = "Uebersetzt Texte zwischen Sprachen",
        systemPrompt = "Du bist ein professioneller Uebersetzer. Uebersetze Texte genau und fluessig zwischen den gewuenschten Sprachen. Behalte den Ton und die Bedeutung bei.",
        provider = AIProvider.OPENROUTER,
        model = "openai/gpt-4o",
        temperature = 0.3f
    ),
    AgentTemplate(
        name = "Local Ollama General",
        description = "Allzweck-Assistent via Ollama (lokal)",
        systemPrompt = "You are a helpful AI assistant.",
        provider = AIProvider.OLLAMA,
        model = "",
        temperature = 0.7f
    )
)

fun mergeTemplateList(current: List<AgentTemplate>, template: AgentTemplate): List<AgentTemplate> =
    current.filterNot { it.name == template.name } + template

fun removeTemplateFromList(current: List<AgentTemplate>, name: String): List<AgentTemplate> =
    current.filterNot { it.name == name }

private val Context.templatesDataStore by preferencesDataStore(name = "agent_templates")
private val TEMPLATES_JSON = stringPreferencesKey("templates_json")
private val templatesGson: Gson = Gson()

class AgentTemplatesRepository(private val context: Context) {

    val templates: Flow<List<AgentTemplate>> = context.templatesDataStore.data
        .map { prefs ->
            val json = prefs[TEMPLATES_JSON]
            if (json.isNullOrBlank()) {
                getDefaultAgentTemplates()
            } else {
                runCatching {
                    templatesGson.fromJson(json, Array<AgentTemplate>::class.java).toList()
                }.getOrElse { getDefaultAgentTemplates() }
            }
        }

    suspend fun saveTemplate(template: AgentTemplate) {
        val merged = mergeTemplateList(templates.first(), template)
        context.templatesDataStore.edit { prefs ->
            prefs[TEMPLATES_JSON] = templatesGson.toJson(merged)
        }
    }

    suspend fun deleteTemplate(name: String) {
        val remaining = removeTemplateFromList(templates.first(), name)
        context.templatesDataStore.edit { prefs ->
            prefs[TEMPLATES_JSON] = templatesGson.toJson(remaining)
        }
    }
}
