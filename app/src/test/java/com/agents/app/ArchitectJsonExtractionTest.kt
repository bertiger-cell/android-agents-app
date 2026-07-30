package com.agents.app

import org.junit.Test
import org.junit.Assert.*

class ArchitectJsonExtractionTest {

    // Exakt das Regex aus AgentViewModel.kt extractAndParseArchitectJSON
    private val jsonRegex = Regex("""```json\s*(\{[\s\S]*?\})\s*```""")

    @Test
    fun extractJson_fromArchitectMessage() {
        val message = """
            Tolles Interview! Hier ist der Plan:
            
            ```json
            {"discovery_context": {"domain": "Mobile App"}, "phases": [], "rules": [], "suggested_agents": []}
            ```
            
            Viel Erfolg!
        """.trimIndent()

        val match = jsonRegex.find(message)
        assertNotNull("JSON-Codeblock muss gefunden werden", match)
        val json = match!!.groupValues[1]
        assertTrue("JSON muss discovery_context enthalten", json.contains("discovery_context"))
        assertTrue("JSON muss domain enthalten", json.contains("Mobile App"))
    }

    @Test
    fun extractJson_withoutCodeBlock_returnsNull() {
        val message = """
            Hier ist der Plan: {"discovery_context": {"domain": "web"}}
            Aber ohne Codeblock - das wird nicht gematcht.
        """.trimIndent()

        val match = jsonRegex.find(message)
        assertNull("Ohne ```json Codeblock darf kein Match gefunden werden", match)
    }

    @Test
    fun extractJson_multipleCodeBlocks_takesFirst() {
        val message = """
            ```json
            {"discovery_context": {"domain": "Web"}, "phases": [], "rules": [], "suggested_agents": []}
            ```
            
            Hier ein Zwischengedanke...
            
            ```json
            {"phases": [{"name": "Test"}]}
            ```
        """.trimIndent()

        val match = jsonRegex.find(message)
        assertNotNull("Erster Codeblock muss gefunden werden", match)
        val json = match!!.groupValues[1]
        assertTrue("Erster Block muss discovery_context enthalten", json.contains("discovery_context"))
        assertFalse("Nicht den zweiten Block nehmen", json.contains("Test"))
    }

    @Test
    fun extractJson_withLineBreaksInCodeBlock() {
        val message = """
            ```json
            {
                "discovery_context": {
                    "domain": "Web App",
                    "technologies": ["Kotlin", "Jetpack Compose"],
                    "experience_level": "Intermediate",
                    "project_size": "Medium"
                },
                "phases": [
                    {
                        "phase_number": 1,
                        "name": "Setup",
                        "description": "Initial setup",
                        "duration": "1 Woche"
                    }
                ]
            }
            ```
        """.trimIndent()

        val match = jsonRegex.find(message)
        assertNotNull("JSON mit Zeilenumbruechen muss gefunden werden", match)
        val json = match!!.groupValues[1]
        assertTrue("JSON muss technologies enthalten", json.contains("Jetpack Compose"))
        assertTrue("JSON muss phases enthalten", json.contains("\"phase_number\": 1"))
    }

    @Test
    fun extractJson_withExtraTextAfterCodeBlock() {
        val message = """Hier das JSON: ```json{"key": "value"}``` Fertig."""

        val match = jsonRegex.find(message)
        assertNotNull("JSON inline im Text muss gefunden werden", match)
        assertEquals("Inline-JSON korrekt extrahieren", """{"key": "value"}""", match!!.groupValues[1])
    }
}
