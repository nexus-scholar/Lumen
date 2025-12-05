package com.lumen.search.domain.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test

class ScholarlyDocumentTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `serialization round-trip preserves all fields`() {
        val original = createCompleteDocument()

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<ScholarlyDocument>(serialized)

        deserialized shouldBe original
    }

    @Test
    fun `default values are applied correctly`() {
        val minimal = ScholarlyDocument(
            lumenId = "test:123",
            doi = null,
            sourceProvider = "test",
            title = "Test Document",
            authors = emptyList(),
            publicationYear = null,
            venue = null,
            citationCount = 0,
            pdfUrl = null
        )

        minimal.abstract shouldBe null
        minimal.tldr shouldBe null
        minimal.concepts.shouldBeEmpty()
        minimal.references.shouldBeEmpty()
        minimal.citations.shouldBeEmpty()
        minimal.rawSourceData.size shouldBe 0
        minimal.isFullyHydrated shouldBe false
        minimal.retrievalConfidence shouldBe 1.0
        minimal.mergedFromIds.shouldBeEmpty()
    }

    @Test
    fun `sidecar preserves raw JSON objects`() {
        val rawData = mapOf(
            "openalex" to buildJsonObject {
                put("id", "https://openalex.org/W123")
                put("type", "journal-article")
                put("custom_field", "custom_value")
            },
            "crossref" to buildJsonObject {
                put("DOI", "10.1234/test")
                put("publisher", "Test Publisher")
            }
        )

        val doc = ScholarlyDocument(
            lumenId = "test:123",
            doi = "10.1234/test",
            sourceProvider = "openalex",
            title = "Test Document",
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 0,
            pdfUrl = null,
            rawSourceData = rawData
        )

        doc.rawSourceData.size shouldBe 2
        doc.rawSourceData["openalex"]?.get("custom_field")?.toString() shouldBe "\"custom_value\""
        doc.rawSourceData["crossref"]?.get("publisher")?.toString() shouldBe "\"Test Publisher\""
    }

    @Test
    fun `hydration state can be toggled`() {
        val doc = ScholarlyDocument(
            lumenId = "test:123",
            doi = null,
            sourceProvider = "test",
            title = "Test",
            authors = emptyList(),
            publicationYear = null,
            venue = null,
            citationCount = 0,
            pdfUrl = null,
            isFullyHydrated = false
        )

        doc.isFullyHydrated shouldBe false

        val hydrated = doc.copy(isFullyHydrated = true)
        hydrated.isFullyHydrated shouldBe true
    }

    @Test
    fun `mergedFromIds tracks merge history`() {
        val original = ScholarlyDocument(
            lumenId = "oa:W123",
            doi = "10.1234/test",
            sourceProvider = "openalex",
            title = "Original",
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 100,
            pdfUrl = null,
            mergedFromIds = emptyList()
        )

        val merged = original.copy(
            mergedFromIds = original.mergedFromIds + "ss:abc123" + "cr:10.1234/test"
        )

        merged.mergedFromIds shouldHaveSize 2
        merged.mergedFromIds[0] shouldBe "ss:abc123"
        merged.mergedFromIds[1] shouldBe "cr:10.1234/test"
    }

    @Test
    fun `retrieval confidence indicates quality`() {
        val highConfidence = ScholarlyDocument(
            lumenId = "test:1",
            doi = "10.1234/exact",
            sourceProvider = "crossref",
            title = "Exact Match",
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 0,
            pdfUrl = null,
            retrievalConfidence = 1.0
        )

        val lowConfidence = ScholarlyDocument(
            lumenId = "test:2",
            doi = null,
            sourceProvider = "openalex",
            title = "Fuzzy Match",
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 0,
            pdfUrl = null,
            retrievalConfidence = 0.75
        )

        highConfidence.retrievalConfidence shouldBe 1.0
        lowConfidence.retrievalConfidence shouldBe 0.75
    }

    @Test
    fun `documents with same DOI can be identified for merging`() {
        val doc1 = createCompleteDocument().copy(lumenId = "oa:W123", sourceProvider = "openalex")
        val doc2 = createCompleteDocument().copy(lumenId = "ss:abc", sourceProvider = "semanticscholar")

        // Both have the same DOI, indicating they represent the same work
        doc1.doi shouldBe doc2.doi
        doc1.lumenId shouldNotBe doc2.lumenId
    }

    @Test
    fun `concepts contain relevance scores`() {
        val concepts = listOf(
            Concept(name = "Machine Learning", score = 0.95, id = "C1"),
            Concept(name = "Neural Networks", score = 0.82, id = "C2"),
            Concept(name = "Computer Science", score = 0.65, id = "C3")
        )

        val doc = ScholarlyDocument(
            lumenId = "test:123",
            doi = null,
            sourceProvider = "openalex",
            title = "ML Paper",
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 0,
            pdfUrl = null,
            concepts = concepts
        )

        doc.concepts shouldHaveSize 3
        doc.concepts[0].score shouldBe 0.95
        doc.concepts.maxByOrNull { it.score }?.name shouldBe "Machine Learning"
    }

    private fun createCompleteDocument(): ScholarlyDocument {
        return ScholarlyDocument(
            lumenId = "oa:W2741809807",
            doi = "10.1038/s41586-019-1666-5",
            sourceProvider = "openalex",
            title = "Test Document Title",
            authors = listOf(
                Author(name = "Jane Doe", orcid = "0000-0001-2345-6789", affiliation = "Harvard"),
                Author(name = "John Smith", orcid = null, affiliation = "MIT")
            ),
            publicationYear = 2024,
            venue = "Nature",
            citationCount = 150,
            pdfUrl = "https://example.com/paper.pdf",
            abstract = "This is the abstract of the document.",
            tldr = "Short summary of the paper.",
            concepts = listOf(
                Concept(name = "Test Concept", score = 0.9, id = "C1")
            ),
            references = listOf("10.1234/ref1", "10.1234/ref2"),
            citations = listOf("10.1234/cite1"),
            rawSourceData = mapOf(
                "openalex" to buildJsonObject { put("id", "W2741809807") }
            ),
            isFullyHydrated = true,
            retrievalConfidence = 1.0,
            mergedFromIds = emptyList()
        )
    }
}

