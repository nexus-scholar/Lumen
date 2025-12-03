package com.lumen.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.lumen.core.domain.model.Project
import kotlinx.serialization.json.Json
import java.io.File

class ListProjectsCommand : CliktCommand(
    name = "list",
    help = "List all systematic review projects"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override fun run() {
        val dataDir = File("data")
        if (!dataDir.exists() || dataDir.listFiles()?.isEmpty() == true) {
            echo("📋 No projects found.")
            echo("   Create a new project with: lumen new -q \"Your research question\"")
            return
        }

        echo("📋 Systematic Review Projects:")
        echo("")

        val projects = loadProjects()

        if (projects.isEmpty()) {
            echo("   No valid projects found.")
            return
        }

        projects.sortedByDescending { it.updatedAt }.forEach { project ->
            echo("  • ${project.id}")
            echo("    Name: ${project.name}")
            echo("    Type: ${project.reviewType}")
            echo("    Status: ${project.status}")
            echo("    Created: ${project.createdAt}")
            echo("    Authors: ${project.authors.size}")
            echo("")
        }

        echo("Total: ${projects.size} project(s)")
    }

    private fun loadProjects(): List<Project> {
        val dataDir = File("data")
        if (!dataDir.exists()) return emptyList()

        return dataDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { projectDir ->
                try {
                    val projectFile = File(projectDir, "artifacts/Project.json")
                    if (projectFile.exists()) {
                        json.decodeFromString<Project>(projectFile.readText())
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    echo("⚠️  Warning: Could not load project from ${projectDir.name}: ${e.message}", err = true)
                    null
                }
            }
            ?: emptyList()
    }
}

