package com.agents.app.models

import org.junit.Test
import org.junit.Assert.*

class AgentModelsTest {

    // ===== AgentSpec Tests =====

    @Test
    fun agentSpec_defaultValues() {
        val spec = AgentSpec(
            name = "TestAgent",
            description = "A test agent",
            systemPrompt = "You are helpful"
        )

        assertEquals("TestAgent", spec.name)
        assertEquals("A test agent", spec.description)
        assertEquals("openrouter", spec.provider)
        assertEquals("gpt-4o", spec.model)
        assertEquals(0.7f, spec.temperature)
    }

    @Test
    fun agentSpec_customValues() {
        val spec = AgentSpec(
            name = "Custom Agent",
            description = "Custom description",
            systemPrompt = "You are custom",
            provider = "ollama",
            model = "llama3",
            temperature = 0.1f
        )

        assertEquals("ollama", spec.provider)
        assertEquals("llama3", spec.model)
        assertEquals(0.1f, spec.temperature)
    }

    @Test
    fun agentSpec_copyWithModifications() {
        val original = AgentSpec(
            name = "Original",
            description = "Original description",
            systemPrompt = "Original prompt",
            provider = "openrouter",
            model = "gpt-4o",
            temperature = 0.7f
        )

        val modified = original.copy(systemPrompt = "Modified prompt", temperature = 0.5f)

        assertEquals("Original", modified.name) // unchanged
        assertEquals("Modified prompt", modified.systemPrompt) // changed
        assertEquals(0.5f, modified.temperature) // changed
        assertEquals(original.description, modified.description) // unchanged
    }

    // ===== ProjectScaffold Tests =====

    @Test
    fun projectScaffold_construction() {
        val discovery = ProjectDiscovery(
            projectId = "test-id",
            domain = "Mobile App",
            technologies = listOf("Kotlin", "Compose"),
            experienceLevel = "Advanced",
            projectSize = "Large",
            concerns = listOf("Performance", "Testing"),
            keyInsights = "Use MVVM"
        )

        val phase = ScaffoldPhase(
            phaseNumber = 1,
            name = "Setup",
            description = "Initial setup",
            duration = "1 Woche",
            focus = "Foundation"
        )

        val agent = AgentSpec(
            name = "Dev",
            description = "Developer agent",
            systemPrompt = "You are a developer"
        )

        val scaffold = ProjectScaffold(
            discoveryContext = discovery,
            architecture = "MVVM",
            phases = listOf(phase),
            rules = listOf("Rule 1"),
            suggestedAgents = listOf(agent),
            diaryEntry = "Started project"
        )

        assertEquals("Mobile App", scaffold.discoveryContext.domain)
        assertEquals("MVVM", scaffold.architecture)
        assertEquals(1, scaffold.phases.size)
        assertEquals(1, scaffold.rules.size)
        assertEquals(1, scaffold.suggestedAgents.size)
        assertEquals("Started project", scaffold.diaryEntry)
    }

    @Test
    fun projectScaffold_copyModifyAgents() {
        val discovery = ProjectDiscovery(projectId = "test")
        val scaffold = ProjectScaffold(
            discoveryContext = discovery,
            architecture = "Test",
            phases = emptyList(),
            rules = emptyList(),
            suggestedAgents = listOf(
                AgentSpec("A", "", "Prompt A"),
                AgentSpec("B", "", "Prompt B")
            ),
            diaryEntry = ""
        )

        val updatedAgents = scaffold.suggestedAgents.map {
            it.copy(systemPrompt = "Modified: ${it.systemPrompt}")
        }
        val modified = scaffold.copy(suggestedAgents = updatedAgents)

        assertEquals(2, modified.suggestedAgents.size)
        assertEquals("Modified: Prompt A", modified.suggestedAgents[0].systemPrompt)
        assertEquals("Modified: Prompt B", modified.suggestedAgents[1].systemPrompt)
        assertEquals(scaffold.architecture, modified.architecture) // unchanged
        assertEquals(scaffold.discoveryContext, modified.discoveryContext) // unchanged
    }

    // ===== ScaffoldPhase Tests =====

    @Test
    fun scaffoldPhase_ordering() {
        val phases = listOf(
            ScaffoldPhase(2, "Phase 2", "", "2d", "B"),
            ScaffoldPhase(1, "Phase 1", "", "1d", "A"),
            ScaffoldPhase(3, "Phase 3", "", "3d", "C")
        )

        val sorted = phases.sortedBy { it.phaseNumber }

        assertEquals(1, sorted[0].phaseNumber)
        assertEquals("Phase 1", sorted[0].name)
        assertEquals(2, sorted[1].phaseNumber)
        assertEquals(3, sorted[2].phaseNumber)
    }

    // ===== ProjectDiscovery Tests =====

    @Test
    fun projectDiscovery_defaults() {
        val discovery = ProjectDiscovery(projectId = "test")

        assertEquals("test", discovery.projectId)
        assertTrue(discovery.domain.isEmpty())
        assertTrue(discovery.technologies.isEmpty())
        assertTrue(discovery.concerns.isEmpty())
    }

    @Test
    fun projectDiscovery_fullConstruction() {
        val discovery = ProjectDiscovery(
            projectId = "p1",
            domain = "Web",
            technologies = listOf("Kotlin", "JS"),
            experienceLevel = "Beginner",
            projectSize = "Small",
            concerns = listOf("Time"),
            links = listOf("https://example.com"),
            keyInsights = "Keep it simple"
        )

        assertEquals("Web", discovery.domain)
        assertEquals(listOf("Kotlin", "JS"), discovery.technologies)
        assertEquals("Beginner", discovery.experienceLevel)
        assertEquals(listOf("Time"), discovery.concerns)
        assertEquals(listOf("https://example.com"), discovery.links)
        assertEquals("Keep it simple", discovery.keyInsights)
    }
}
