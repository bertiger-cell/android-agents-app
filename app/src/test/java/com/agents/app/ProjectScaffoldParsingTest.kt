package com.agents.app

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class ProjectScaffoldParsingTest {

    private val gson = Gson()

    @Test
    fun parseFullArchitectJson() {
        val json = """
        {
            "discovery_context": {
                "domain": "Web App",
                "technologies": ["Kotlin", "Jetpack Compose", "Ktor"],
                "experience_level": "Intermediate",
                "project_size": "Medium",
                "concerns": ["Zeitplan", "Budget"]
            },
            "architecture": "MVVM + Clean Architecture",
            "phases": [
                {
                    "phase_number": 1,
                    "name": "Foundation",
                    "description": "Setup Projekt-Struktur und Build-Konfiguration",
                    "duration": "1 Woche",
                    "focus": "Infrastructure"
                },
                {
                    "phase_number": 2,
                    "name": "Core Features",
                    "description": "Implementiere Kern-Funktionalitaet",
                    "duration": "3 Wochen",
                    "focus": "Features"
                }
            ],
            "rules": [
                "Teste allen Code mit min. 70% Coverage",
                "Nutze Material 3 Design",
                "Keine Magic Numbers"
            ],
            "suggested_agents": [
                {
                    "name": "Backend Developer",
                    "description": "Ktor API Entwicklung",
                    "system_prompt": "Du bist ein Ktor-Experte...",
                    "provider": "openrouter",
                    "model": "gpt-4o",
                    "temperature": 0.7
                }
            ],
            "diary_entry": "Projekt erfolgreich geplant. Zwei Phasen definiert."
        }
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val parsed = gson.fromJson(json, Map::class.java) as Map<String, Any>

        assertEquals("Web App", (parsed["discovery_context"] as Map<*, *>)["domain"])
        assertEquals(3, ((parsed["discovery_context"] as Map<*, *>)["technologies"] as List<*>).size)
        assertEquals(2, (parsed["phases"] as List<*>).size)
        assertEquals(3, (parsed["rules"] as List<*>).size)
        assertEquals(1, (parsed["suggested_agents"] as List<*>).size)
        assertTrue((parsed["diary_entry"] as String).startsWith("Projekt"))
    }

    @Test
    fun parseMinimalArchitectJson() {
        // Pflichtfelder ohne optionale Werte
        val json = """
        {
            "discovery_context": {
                "domain": "CLI Tool",
                "technologies": [],
                "experience_level": "",
                "project_size": "Small"
            },
            "architecture": "Single File",
            "phases": [],
            "rules": [],
            "suggested_agents": [],
            "diary_entry": ""
        }
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val parsed = gson.fromJson(json, Map::class.java) as Map<String, Any>

        assertEquals("CLI Tool", (parsed["discovery_context"] as Map<*, *>)["domain"])
        assertEquals(0, (parsed["phases"] as List<*>).size)
        assertEquals(0, (parsed["suggested_agents"] as List<*>).size)
    }

    @Test
    fun validateAgentSpecFields() {
        val json = """
        {
            "name": "Test Agent",
            "description": "Test Description",
            "system_prompt": "You are a test agent",
            "provider": "openrouter",
            "model": "gpt-4o-mini",
            "temperature": 0.3
        }
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val parsed = gson.fromJson(json, Map::class.java) as Map<String, Any>

        assertEquals("Test Agent", parsed["name"])
        assertEquals("openrouter", parsed["provider"])
        assertEquals("gpt-4o-mini", parsed["model"])
        // Gson parses 0.3 as Double
        assertEquals(0.3, (parsed["temperature"] as Double), 0.001)
    }

    @Test
    fun parsePhaseWithAllFields() {
        val json = """
        {
            "phase_number": 1,
            "name": "Setup",
            "description": "Initial project setup with Gradle and dependencies",
            "duration": "2 Wochen",
            "focus": "Infrastructure"
        }
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val parsed = gson.fromJson(json, Map::class.java) as Map<String, Any>

        assertEquals(1.0, (parsed["phase_number"] as Double), 0.001)
        assertEquals("Setup", parsed["name"])
        assertEquals("2 Wochen", parsed["duration"])
        assertEquals("Infrastructure", parsed["focus"])
    }
}
