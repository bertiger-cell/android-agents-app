package com.agents.app

import com.agents.app.data.AgentTemplate
import com.agents.app.data.getDefaultAgentTemplates
import com.agents.app.data.mergeTemplateList
import com.agents.app.data.removeTemplateFromList
import com.agents.app.models.AIProvider
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTemplatesTest {

    @Test
    fun defaults_containFiveUniqueTemplates() {
        val defaults = getDefaultAgentTemplates()
        assertEquals(5, defaults.size)
        assertEquals(defaults.size, defaults.map { it.name }.toSet().size)
        assertTrue(defaults.any { it.name == "Code Assistant" })
        assertTrue(defaults.any { it.provider == AIProvider.OLLAMA })
    }

    @Test
    fun merge_replacesTemplateWithSameName() {
        val base = getDefaultAgentTemplates()
        val modified = base[0].copy(systemPrompt = "NEU")
        val merged = mergeTemplateList(base, modified)

        assertEquals(base.size, merged.size)
        assertEquals("NEU", merged.first { it.name == modified.name }.systemPrompt)
    }

    @Test
    fun merge_appendsNewTemplate() {
        val base = getDefaultAgentTemplates()
        val extra = AgentTemplate(
            name = "Mein Template",
            provider = AIProvider.ZEN,
            model = "gpt-4o",
            temperature = 0.5f
        )
        val merged = mergeTemplateList(base, extra)

        assertEquals(base.size + 1, merged.size)
        assertTrue(merged.last().name == "Mein Template")
    }

    @Test
    fun remove_deletesOnlyMatchingName() {
        val base = getDefaultAgentTemplates()
        val remaining = removeTemplateFromList(base, "Translator")

        assertEquals(base.size - 1, remaining.size)
        assertFalse(remaining.any { it.name == "Translator" })
    }

    @Test
    fun gson_roundtrip_preservesTemplate() {
        val template = AgentTemplate(
            name = "Test",
            description = "desc",
            systemPrompt = "prompt",
            provider = AIProvider.OLLAMA,
            model = "llama3",
            temperature = 0.2f
        )
        val json = Gson().toJson(listOf(template))
        val parsed = Gson().fromJson(json, Array<AgentTemplate>::class.java).toList()

        assertEquals(1, parsed.size)
        assertEquals("Test", parsed[0].name)
        assertEquals(AIProvider.OLLAMA, parsed[0].provider)
        assertEquals(0.2f, parsed[0].temperature)
    }
}
