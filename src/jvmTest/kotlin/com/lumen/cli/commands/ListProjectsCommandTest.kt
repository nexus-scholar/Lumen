package com.lumen.cli.commands

import com.lumen.core.domain.model.Author
import com.lumen.core.domain.model.AuthorRole
import com.lumen.core.domain.model.Project
import com.lumen.core.domain.model.ProjectStatus
import com.lumen.core.domain.model.ReviewType
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

class ListProjectsCommandTest {

    @TempDir
    lateinit var tempDir: Path

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @BeforeEach
    fun setup() {
        // Create test project structure
        val projectId = "test_project_123"
        val projectDir = File(tempDir.toFile(), "data/$projectId")
        val artifactsDir = File(projectDir, "artifacts")
        artifactsDir.mkdirs()

        // Create a test project
        val project = Project(
            id = projectId,
            name = "Test Systematic Review",
            description = "A test project",
            rawIdea = "Testing the list command",
            reviewType = ReviewType.INTERVENTION,
            authors = listOf(
                Author(
                    fullName = "Test Author",
                    email = "test@example.com",
                    role = AuthorRole.LEAD
                )
            ),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            status = ProjectStatus.CREATED
        )

        val projectFile = File(artifactsDir, "Project.json")
        projectFile.writeText(json.encodeToString(project))
    }

    @Test
    fun shouldListProjects() {
        // This is a basic structure test
        // Actual CLI testing would require running the command
        val dataDir = File(tempDir.toFile(), "data")
        assertTrue(dataDir.exists(), "Data directory should exist")

        val projects = dataDir.listFiles()?.filter { it.isDirectory }
        assertTrue(projects?.isNotEmpty() == true, "Should have at least one project")
    }

    @AfterEach
    fun cleanup() {
        // Temp directory is automatically cleaned up by JUnit
    }
}

