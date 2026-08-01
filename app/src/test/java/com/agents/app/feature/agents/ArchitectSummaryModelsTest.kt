package com.agents.app.feature.agents

import com.agents.app.models.AgentSpec
import org.junit.Test
import org.junit.Assert.*

class ArchitectSummaryModelsTest {

    @Test
    fun agentSpecWithEditedPrompt_fromAgentSpec() {
        val spec = AgentSpec(
            name = "Backend Dev",
            description = "Ktor API development",
            systemPrompt = "You are a Ktor expert",
            provider = "openrouter",
            model = "gpt-4o",
            temperature = 0.7f
        )

        val edited = AgentSpecWithEditedPrompt(spec)

        assertEquals(spec.name, edited.name)
        assertEquals(spec.description, edited.description)
        assertEquals(spec.systemPrompt, edited.systemPrompt)
        assertEquals(spec.provider, edited.provider)
        assertEquals(spec.model, edited.model)
        assertEquals(spec.temperature, edited.temperature, 0.001f)
    }

    @Test
    fun agentSpecWithEditedPrompt_customSystemPrompt() {
        val spec = AgentSpec(
            name = "Test",
            description = "Test agent",
            systemPrompt = "Original prompt",
            provider = "openrouter",
            model = "gpt-4o",
            temperature = 0.7f
        )

        val edited = AgentSpecWithEditedPrompt(
            name = spec.name,
            description = spec.description,
            systemPrompt = "Modified custom prompt",
            provider = spec.provider,
            model = spec.model,
            temperature = spec.temperature
        )

        assertEquals("Modified custom prompt", edited.systemPrompt)
        assertEquals(spec.name, edited.name)
    }

    @Test
    fun agentSpecWithEditedPrompt_defaultValues() {
        val edited = AgentSpecWithEditedPrompt(
            name = "Agent",
            description = "Desc",
            systemPrompt = "Prompt"
        )

        assertEquals("openrouter", edited.provider)
        assertEquals("gpt-4o", edited.model)
        assertEquals(0.7f, edited.temperature)
    }

    @Test
    fun agentSpecWithEditedPrompt_copyFromListToAgentSpec() {
        val originals = listOf(
            AgentSpec("A", "Desc A", "Prompt A"),
            AgentSpec("B", "Desc B", "Prompt B")
        )

        val editedList = originals.map { AgentSpecWithEditedPrompt(it) }
        val backToSpecs = editedList.map { edited ->
            AgentSpec(
                name = edited.name,
                description = edited.description,
                systemPrompt = edited.systemPrompt,
                provider = edited.provider,
                model = edited.model,
                temperature = edited.temperature
            )
        }

        assertEquals(2, backToSpecs.size)
        assertEquals("A", backToSpecs[0].name)
        assertEquals("Prompt B", backToSpecs[1].systemPrompt)
    }
}
