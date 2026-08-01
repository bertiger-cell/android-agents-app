package com.agents.app

import com.agents.app.createProjectFolderStructure
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProjectFolderStructureTest {

    @Test
    fun createsProjectRootAndSubfolders() {
        val dir = File(System.getProperty("java.io.tmpdir"), "pfs_${System.nanoTime()}")
        try {
            val created = createProjectFolderStructure(dir.absolutePath)
            assertTrue("Projekt-Root muss angelegt werden", created)
            assertTrue(File(dir, "media").isDirectory)
            assertTrue(File(dir, "audio").isDirectory)
            assertTrue(File(dir, "exports").isDirectory)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun existingRootIsAccepted() {
        val dir = File(System.getProperty("java.io.tmpdir"), "pfs2_${System.nanoTime()}").apply { mkdirs() }
        try {
            val created = createProjectFolderStructure(dir.absolutePath)
            assertTrue(created)
            assertTrue(File(dir, "exports").isDirectory)
        } finally {
            dir.deleteRecursively()
        }
    }
}
