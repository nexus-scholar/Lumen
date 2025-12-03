package com.lumen.core.export

import com.lumen.core.domain.model.Document
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class BibTeXExporterTest {

    @Test
    fun shouldExportEmptyList() {
        val exporter = BibTeXExporter()
        val result = exporter.export(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun shouldExportSingleDocument() {
        val exporter = BibTeXExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Machine Learning for Crop Disease Detection",
            abstract = "This study investigates the use of machine learning.",
            authors = listOf("Smith, John", "Doe, Jane"),
            year = 2024,
            doi = "10.1234/test.2024.001",
            url = "https://example.com/paper",
            venue = "Nature Agriculture",
            provider = "openalex",
            providerId = "W1234567890",
            citationCount = 42,
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should contain BibTeX entry
        assertTrue(result.contains("@article{"))
        assertTrue(result.contains("title = {Machine Learning for Crop Disease Detection}"))
        assertTrue(result.contains("author = {Smith, John and Doe, Jane}"))
        assertTrue(result.contains("year = {2024}"))
        assertTrue(result.contains("doi = {10.1234/test.2024.001}"))
        assertTrue(result.contains("journal = {Nature Agriculture}"))
    }

    @Test
    fun shouldEscapeBibTeXSpecialCharacters() {
        val exporter = BibTeXExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Title with & special % characters # and _ underscores",
            authors = listOf("Author Name"),
            year = 2024,
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should escape special characters
        assertTrue(result.contains("\\&"))
        assertTrue(result.contains("\\%"))
        assertTrue(result.contains("\\#"))
        assertTrue(result.contains("\\_"))
    }

    @Test
    fun shouldGenerateUniqueCitationKeys() {
        val exporter = BibTeXExporter()
        val doc1 = Document(
            id = "1",
            projectId = "project-123",
            title = "First Paper About Machine Learning",
            authors = listOf("Smith, John"),
            year = 2024,
            provider = "openalex",
            providerId = "W1",
            retrievedAt = Clock.System.now()
        )
        val doc2 = Document(
            id = "2",
            projectId = "project-123",
            title = "Second Paper About Deep Learning",
            authors = listOf("Doe, Jane"),
            year = 2023,
            provider = "openalex",
            providerId = "W2",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(doc1, doc2))

        // Should have different citation keys
        assertTrue(result.contains("Smith_2024"))
        assertTrue(result.contains("Doe_2023"))
    }

    @Test
    fun shouldHandleConferenceVenue() {
        val exporter = BibTeXExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Conference Paper",
            authors = listOf("Author Name"),
            year = 2024,
            venue = "International Conference on Machine Learning",
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        // Should be inproceedings for conference
        assertTrue(result.contains("@inproceedings{"))
        assertTrue(result.contains("booktitle = {"))
    }

    @Test
    fun shouldIncludeAbstract() {
        val exporter = BibTeXExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Test Paper",
            abstract = "This is a test abstract with important information.",
            authors = listOf("Author"),
            year = 2024,
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        assertTrue(result.contains("abstract = {This is a test abstract"))
    }

    @Test
    fun shouldIncludeURL() {
        val exporter = BibTeXExporter()
        val document = Document(
            id = "test-1",
            projectId = "project-123",
            title = "Test Paper",
            authors = listOf("Author"),
            year = 2024,
            url = "https://example.com/paper/123",
            provider = "openalex",
            providerId = "W123",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(document))

        assertTrue(result.contains("url = {https://example.com/paper/123}"))
    }

    @Test
    fun shouldSeparateEntriesWithBlankLine() {
        val exporter = BibTeXExporter()
        val doc1 = Document(
            id = "1",
            projectId = "project-123",
            title = "First",
            authors = listOf("Author"),
            provider = "openalex",
            providerId = "W1",
            retrievedAt = Clock.System.now()
        )
        val doc2 = Document(
            id = "2",
            projectId = "project-123",
            title = "Second",
            authors = listOf("Author"),
            provider = "openalex",
            providerId = "W2",
            retrievedAt = Clock.System.now()
        )

        val result = exporter.export(listOf(doc1, doc2))

        // Should have two entries separated by blank line
        val entries = result.split("\n\n")
        assertTrue(entries.size >= 2)
    }
}

