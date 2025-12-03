package com.lumen.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.choice
import com.lumen.core.data.persistence.DatabaseManager
import com.lumen.core.domain.model.Document
import com.lumen.core.export.BibTeXExporter
import com.lumen.core.export.CsvExporter
import com.lumen.core.export.JsonLinesExporter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

class ExportCommand : CliktCommand(
    name = "export",
    help = "Export project results to various formats"
), KoinComponent {
    private val projectId: String by option(
        "--project", "-p",
        help = "Project ID to export"
    ).required()

    private val format: String by option(
        "--format", "-f",
        help = "Export format (jsonl, csv, bibtex, ris)"
    ).choice("jsonl", "csv", "bibtex", "ris").default("jsonl")

    private val output: String? by option(
        "--output", "-o",
        help = "Output file path (default: data/<project>/export/documents.<format>)"
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun run() = runBlocking {
        echo("📤 Exporting project $projectId to $format...")

        // Verify project exists
        val projectDir = File("data/$projectId")
        if (!projectDir.exists()) {
            echo("❌ Error: Project $projectId not found", err = true)
            echo("   Run 'lumen list' to see available projects")
            return@runBlocking
        }

        // Load documents from database
        val documents = loadDocuments(projectId)

        if (documents.isEmpty()) {
            echo("⚠️  No documents found in project $projectId")
            echo("   The project may not have completed the search execution stage")
            return@runBlocking
        }

        echo("   Found ${documents.size} documents")

        // Export based on format
        val content = when (format) {
            "jsonl" -> exportJsonLines(documents)
            "csv" -> exportCsv(documents)
            "bibtex" -> exportBibTeX(documents)
            "ris" -> {
                echo("❌ RIS export not yet implemented", err = true)
                return@runBlocking
            }
            else -> {
                echo("❌ Unknown format: $format", err = true)
                return@runBlocking
            }
        }

        // Determine output path
        val outputPath = output ?: "data/$projectId/export/documents.$format"
        val outputFile = File(outputPath)

        // Create export directory if needed
        outputFile.parentFile?.mkdirs()

        // Write to file
        outputFile.writeText(content)

        echo("✅ Exported ${documents.size} documents to: $outputPath")
        echo("   File size: ${outputFile.length() / 1024} KB")
    }

    private fun loadDocuments(projectId: String): List<Document> {
        val databaseManager: DatabaseManager = get()
        val documents = mutableListOf<Document>()

        try {
            val connection = databaseManager.getConnection(projectId)
            connection.use { conn ->
                val statement = conn.createStatement()
                val resultSet = statement.executeQuery(
                    "SELECT json_data FROM documents WHERE project_id = '$projectId'"
                )

                while (resultSet.next()) {
                    val jsonData = resultSet.getString("json_data")
                    try {
                        val document = json.decodeFromString<Document>(jsonData)
                        documents.add(document)
                    } catch (e: Exception) {
                        echo("⚠️  Warning: Could not parse document: ${e.message}", err = true)
                    }
                }
            }
        } catch (e: Exception) {
            echo("❌ Error loading documents: ${e.message}", err = true)
        }

        return documents
    }

    private fun exportJsonLines(documents: List<Document>): String {
        val exporter = JsonLinesExporter()
        return exporter.export(documents)
    }

    private fun exportCsv(documents: List<Document>): String {
        val exporter = CsvExporter()
        return exporter.export(documents)
    }

    private fun exportBibTeX(documents: List<Document>): String {
        val exporter = BibTeXExporter()
        return exporter.export(documents)
    }
}

