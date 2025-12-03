package com.lumen.core.export

import com.lumen.core.domain.model.Document
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonLinesExporterTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun shouldExportEmptyList() {
        val exporter = JsonLinesExporter()
        val result = exporter.export(emptyList())
        assertEquals("", result)
    }

    @Test
    fun shouldExportSingleDocument() {
        val exporter = JsonLinesExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Test Document",
            abstract = "This is a test abstract",
            authors = listOf("Author 1", "Author 2"),
            year = 2024,
            doi = "10.1234/test",
            url = "https://example.com/paper",
            venue = "Test Journal",
            provider = "openalex",
            providerId = "W1234567890",
            citationCount = 10,
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should be valid JSON
        val parsed = json.decodeFromString<Document>(result)
        assertEquals("Test Document", parsed.title)
        assertEquals("Author 1", parsed.authors.first())
    }

    @Test
    fun shouldExportMultipleDocuments() {
        val exporter = JsonLinesExporter()
        val documents = listOf(
            Document(
                id = "test-1",
                projectId = "project-123",
                title = "First Document",
                authors = listOf("Author 1"),
                year = 2024,
                provider = "openalex",
                providerId = "W111",
                retrievedAt = Clock.System.now()
            ),
            Document(
                id = "test-2",
                projectId = "project-123",
                title = "Second Document",
                authors = listOf("Author 2"),
                year = 2023,
                provider = "openalex",
                providerId = "W222",
                retrievedAt = Clock.System.now()
            )
        )

        val result = exporter.export(documents)

        // Should have two lines
        val lines = result.split("\n")
        assertEquals(2, lines.size)

        // Each line should be valid JSON
        val doc1 = json.decodeFromString<Document>(lines[0])
        val doc2 = json.decodeFromString<Document>(lines[1])

        assertEquals("First Document", doc1.title)
        assertEquals("Second Document", doc2.title)
    }

    @Test
    fun shouldExportWithoutPrettyPrint() {
        val exporter = JsonLinesExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Test",
            authors = emptyList(),
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should not contain newlines within the JSON (single line)
        assertTrue(!result.contains("{\n"), "Should not have pretty-printed JSON")
    }
}

