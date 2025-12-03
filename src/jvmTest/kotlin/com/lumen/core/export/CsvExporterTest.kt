package com.lumen.core.export

import com.lumen.core.domain.model.Document
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvExporterTest {

    @Test
    fun shouldExportEmptyList() {
        val exporter = CsvExporter()
        val result = exporter.export(emptyList())

        // Should only have header row
        val lines = result.trim().split("\n")
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("ID"))
        assertTrue(lines[0].contains("Title"))
    }

    @Test
    fun shouldExportSingleDocument() {
        val exporter = CsvExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Test Document",
            abstract = "This is a test abstract with some content",
            authors = listOf("Author One", "Author Two"),
            year = 2024,
            doi = "10.1234/test",
            url = "https://example.com/paper",
            venue = "Test Journal",
            provider = "openalex",
            providerId = "W1234567890",
            citationCount = 42,
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should have header + 1 data row
        val lines = result.trim().split("\n")
        assertEquals(2, lines.size)

        // Check data row contains expected values
        val dataRow = lines[1]
        assertTrue(dataRow.contains("test-1"))
        assertTrue(dataRow.contains("Test Document"))
        assertTrue(dataRow.contains("Author One; Author Two"))
        assertTrue(dataRow.contains("2024"))
        assertTrue(dataRow.contains("10.1234/test"))
    }

    @Test
    fun shouldExportMultipleDocuments() {
        val exporter = CsvExporter()
        val documents = listOf(
            Document(
                id = "test-1",
                projectId = "project-123",
                title = "First Document",
                authors = listOf("Author A"),
                year = 2024,
                provider = "openalex",
                providerId = "W111",
                retrievedAt = Clock.System.now()
            ),
            Document(
                id = "test-2",
                projectId = "project-123",
                title = "Second Document",
                authors = listOf("Author B", "Author C"),
                year = 2023,
                provider = "openalex",
                providerId = "W222",
                retrievedAt = Clock.System.now()
            ),
            Document(
                id = "test-3",
                projectId = "project-123",
                title = "Third Document",
                authors = emptyList(),
                year = 2022,
                provider = "crossref",
                providerId = "C333",
                retrievedAt = Clock.System.now()
            )
        )

        val result = exporter.export(documents)

        // Should have header + 3 data rows
        val lines = result.trim().split("\n")
        assertEquals(4, lines.size)

        // Verify all documents are present
        assertTrue(result.contains("First Document"))
        assertTrue(result.contains("Second Document"))
        assertTrue(result.contains("Third Document"))
    }

    @Test
    fun shouldHandleSpecialCharacters() {
        val exporter = CsvExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Title with \"quotes\" and, commas",
            abstract = "Abstract with\nnewlines and special chars: àéîôü",
            authors = listOf("O'Brien, John", "Smith, Mary"),
            year = 2024,
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should properly escape quotes and commas
        assertTrue(result.contains("Title with \"\"quotes\"\" and, commas") ||
                   result.contains("\"Title with \"\"quotes\"\" and, commas\""))

        // CSV with newlines in fields will have the field quoted and span multiple lines
        // So we just verify the content is present, not the line count
        assertTrue(result.contains("Abstract with"))
        assertTrue(result.contains("newlines and special chars"))
        assertTrue(result.contains("O'Brien, John; Smith, Mary") ||
                   result.contains("\"O'Brien, John; Smith, Mary\""))
    }

    @Test
    fun shouldHandleNullFields() {
        val exporter = CsvExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Minimal Document",
            authors = emptyList(),
            year = null, // null year
            doi = null, // null DOI
            url = null, // null URL
            venue = null, // null venue
            abstract = null, // null abstract
            citationCount = null, // null citations
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should handle nulls gracefully (empty fields)
        val lines = result.trim().split("\n")
        assertEquals(2, lines.size)

        // Should contain the title
        assertTrue(result.contains("Minimal Document"))
    }

    @Test
    fun shouldIncludeAllColumns() {
        val exporter = CsvExporter()
        val result = exporter.export(emptyList())

        // Verify all expected columns are in header
        val header = result.trim().split("\n")[0]
        val expectedColumns = listOf(
            "ID", "Title", "Authors", "Year", "DOI",
            "URL", "Venue", "Provider", "Citation Count", "Abstract"
        )

        expectedColumns.forEach { column ->
            assertTrue(header.contains(column), "Header should contain column: $column")
        }
    }

    @Test
    fun shouldTruncateLongAbstracts() {
        val exporter = CsvExporter()
        val longAbstract = "A".repeat(1000) // 1000 character abstract

        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Document with Long Abstract",
            abstract = longAbstract,
            authors = emptyList(),
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Abstract should be truncated to 500 chars
        // The CSV might have quotes around it, so check for presence
        val lines = result.split("\n")
        val dataRow = lines[1]

        // Should not contain the full 1000 A's
        assertTrue(!dataRow.contains("A".repeat(600)), "Abstract should be truncated")
    }
}

