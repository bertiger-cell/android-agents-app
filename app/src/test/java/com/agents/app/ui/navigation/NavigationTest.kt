package com.agents.app.ui.navigation

import com.agents.app.models.ChatSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {

    @Test
    fun projectsWithoutSession_mapsToProjects() {
        assertEquals(
            TopLevelDestination.PROJECTS,
            resolveTopLevelDestination("projects", hasSelectedSession = false)
        )
    }

    @Test
    fun projectDetailWithoutSession_mapsToProjects() {
        assertEquals(
            TopLevelDestination.PROJECTS,
            resolveTopLevelDestination("project/123", hasSelectedSession = false)
        )
    }

    @Test
    fun settingsWithoutSession_mapsToSettings() {
        assertEquals(
            TopLevelDestination.SETTINGS,
            resolveTopLevelDestination("settings", hasSelectedSession = false)
        )
    }

    @Test
    fun settingsWithSession_staysSettingsTab() {
        assertEquals(
            TopLevelDestination.SETTINGS,
            resolveTopLevelDestination("settings", hasSelectedSession = true)
        )
    }

    @Test
    fun anyNonSettingsRouteWithSession_mapsToChat() {
        assertEquals(
            TopLevelDestination.CHAT,
            resolveTopLevelDestination("project/123", hasSelectedSession = true)
        )
        assertEquals(
            TopLevelDestination.CHAT,
            resolveTopLevelDestination(null, hasSelectedSession = true)
        )
    }

    @Test
    fun chatOverlay_hiddenOnSettingsEvenWithSession() {
        val session = ChatSessionEntity(
            id = "session-1",
            projectId = "project-1",
            agentId = "agent-1"
        )

        assertFalse(shouldShowChatOverlay("settings", session))
    }

    @Test
    fun chatOverlay_visibleOnProjectsWithSession() {
        val session = ChatSessionEntity(
            id = "session-1",
            projectId = "project-1",
            agentId = "agent-1"
        )

        assertTrue(shouldShowChatOverlay("projects", session))
    }

    @Test
    fun chatOverlay_visibleOnProjectDetailWithSession() {
        val session = ChatSessionEntity(
            id = "session-1",
            projectId = "project-1",
            agentId = "agent-1"
        )

        assertTrue(shouldShowChatOverlay("project/123", session))
    }
}
