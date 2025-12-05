package com.lumen.search.data.engine

import com.lumen.search.domain.models.Author
import com.lumen.search.domain.models.Concept
import com.lumen.search.domain.models.ScholarlyDocument
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

/**
 * Tests for the ResultMerger fusion logic.
 * Validates the trust hierarchy and field selection strategies.
 */
class ResultMergerTest {

    @Test
    fun `title from Crossref wins over OpenAlex`() {
        val openalexDoc = createDocument(
            provider = "openalex",
            title = "Title from OpenAlex"
        )
        val crossrefDoc = createDocument(
            provider = "crossref",
            title = "Title from Crossref"
        )

        val merged = ResultMerger.merge(openalexDoc, crossrefDoc)

        merged.title shouldBe "Title from Crossref"
    }

    @Test
    fun `title from OpenAlex wins over Semantic Scholar`() {
        val ssDoc = createDocument(
            provider = "semanticscholar",
            title = "Title from Semantic Scholar"
        )
        val openalexDoc = createDocument(
            provider = "openalex",
            title = "Title from OpenAlex"
        )

        val merged = ResultMerger.merge(ssDoc, openalexDoc)

        merged.title shouldBe "Title from OpenAlex"
    }

    @Test
    fun `abstract from OpenAlex wins over Crossref`() {
        val crossrefDoc = createDocument(
            provider = "crossref",
            abstract = "Abstract from Crossref"
        )
        val openalexDoc = createDocument(
            provider = "openalex",
            abstract = "Abstract from OpenAlex"
        )

        val merged = ResultMerger.merge(crossrefDoc, openalexDoc)

        merged.abstract shouldBe "Abstract from OpenAlex"
    }

    @Test
    fun `abstract from Semantic Scholar wins over Crossref`() {
        val crossrefDoc = createDocument(
            provider = "crossref",
            abstract = "Abstract from Crossref"
        )
        val ssDoc = createDocument(
            provider = "semanticscholar",
            abstract = "Abstract from Semantic Scholar"
        )

        val merged = ResultMerger.merge(crossrefDoc, ssDoc)

        merged.abstract shouldBe "Abstract from Semantic Scholar"
    }

    @Test
    fun `null abstract is replaced by any available abstract`() {
        val docWithNull = createDocument(
            provider = "openalex",
            abstract = null
        )
        val docWithAbstract = createDocument(
            provider = "arxiv",
            abstract = "Abstract from ArXiv"
        )

        val merged = ResultMerger.merge(docWithNull, docWithAbstract)

        merged.abstract shouldBe "Abstract from ArXiv"
    }

    @Test
    fun `TLDR only taken from Semantic Scholar`() {
        val openalexDoc = createDocument(
            provider = "openalex",
            tldr = null
        )
        val ssDoc = createDocument(
            provider = "semanticscholar",
            tldr = "This is the TLDR summary"
        )

        val merged = ResultMerger.merge(openalexDoc, ssDoc)

        merged.tldr shouldBe "This is the TLDR summary"
    }

    @Test
    fun `TLDR from non-Semantic Scholar provider is ignored`() {
        val openalexDoc = createDocument(
            provider = "openalex",
            tldr = "Fake TLDR"
        )
        val ssDoc = createDocument(
            provider = "semanticscholar",
            tldr = "Real TLDR"
        )

        val merged = ResultMerger.merge(openalexDoc, ssDoc)

        merged.tldr shouldBe "Real TLDR"
    }

    @Test
    fun `publication year from Crossref wins over others`() {
        val openalexDoc = createDocument(
            provider = "openalex",
            publicationYear = 2023
        )
        val crossrefDoc = createDocument(
            provider = "crossref",
            publicationYear = 2024
        )

        val merged = ResultMerger.merge(openalexDoc, crossrefDoc)

        merged.publicationYear shouldBe 2024
    }

    @Test
    fun `null publication year is replaced by available year`() {
        val docWithNull = createDocument(
            provider = "crossref",
            publicationYear = null
        )
        val docWithYear = createDocument(
            provider = "arxiv",
            publicationYear = 2024
        )

        val merged = ResultMerger.merge(docWithNull, docWithYear)

        merged.publicationYear shouldBe 2024
    }

    @Test
    fun `venue prefers non-null value`() {
        val docWithNull = createDocument(
            provider = "openalex",
            venue = null
        )
        val docWithVenue = createDocument(
            provider = "crossref",
            venue = "Nature"
        )

        val merged = ResultMerger.merge(docWithNull, docWithVenue)

        merged.venue shouldBe "Nature"
    }

    @Test
    fun `highest citation count wins`() {
        val doc1 = createDocument(
            provider = "openalex",
            citationCount = 150
        )
        val doc2 = createDocument(
            provider = "semanticscholar",
            citationCount = 200
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.citationCount shouldBe 200
    }

    @Test
    fun `PDF URL prefers first non-null value`() {
        val docWithNull = createDocument(
            provider = "openalex",
            pdfUrl = null
        )
        val docWithPdf = createDocument(
            provider = "crossref",
            pdfUrl = "https://example.com/paper.pdf"
        )

        val merged = ResultMerger.merge(docWithNull, docWithPdf)

        merged.pdfUrl shouldBe "https://example.com/paper.pdf"
    }

    @Test
    fun `concepts from OpenAlex win over Semantic Scholar`() {
        val ssDoc = createDocument(
            provider = "semanticscholar",
            concepts = listOf(Concept(name = "SS Concept", score = 0.8, id = "C1"))
        )
        val openalexDoc = createDocument(
            provider = "openalex",
            concepts = listOf(Concept(name = "OA Concept", score = 0.9, id = "C2"))
        )

        val merged = ResultMerger.merge(ssDoc, openalexDoc)

        merged.concepts shouldHaveSize 1
        merged.concepts[0].name shouldBe "OA Concept"
    }

    @Test
    fun `empty concepts fall back to incoming concepts`() {
        val docEmpty = createDocument(
            provider = "openalex",
            concepts = emptyList()
        )
        val docWithConcepts = createDocument(
            provider = "semanticscholar",
            concepts = listOf(Concept(name = "Fallback Concept", score = 0.7, id = "C1"))
        )

        val merged = ResultMerger.merge(docEmpty, docWithConcepts)

        merged.concepts shouldHaveSize 1
        merged.concepts[0].name shouldBe "Fallback Concept"
    }

    @Test
    fun `references are combined and deduplicated`() {
        val doc1 = createDocument(
            provider = "openalex",
            references = listOf("10.1234/ref1", "10.1234/ref2")
        )
        val doc2 = createDocument(
            provider = "semanticscholar",
            references = listOf("10.1234/ref2", "10.1234/ref3")
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.references shouldHaveSize 3
        merged.references shouldContainExactlyInAnyOrder listOf(
            "10.1234/ref1", "10.1234/ref2", "10.1234/ref3"
        )
    }

    @Test
    fun `citations are combined and deduplicated`() {
        val doc1 = createDocument(
            provider = "openalex",
            citations = listOf("10.1234/cite1")
        )
        val doc2 = createDocument(
            provider = "semanticscholar",
            citations = listOf("10.1234/cite1", "10.1234/cite2")
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.citations shouldHaveSize 2
        merged.citations shouldContainExactlyInAnyOrder listOf(
            "10.1234/cite1", "10.1234/cite2"
        )
    }

    @Test
    fun `rawSourceData sidecars are combined`() {
        val doc1 = createDocument(
            provider = "openalex",
            rawSourceData = mapOf(
                "openalex" to buildJsonObject { put("id", "W123") }
            )
        )
        val doc2 = createDocument(
            provider = "crossref",
            rawSourceData = mapOf(
                "crossref" to buildJsonObject { put("DOI", "10.1234/test") }
            )
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.rawSourceData.keys shouldContainExactlyInAnyOrder listOf("openalex", "crossref")
    }

    @Test
    fun `mergedFromIds tracks merge history`() {
        val doc1 = createDocument(
            provider = "openalex",
            lumenId = "oa:W123"
        )
        val doc2 = createDocument(
            provider = "semanticscholar",
            lumenId = "ss:abc456"
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.mergedFromIds shouldContainExactlyInAnyOrder listOf("ss:abc456")
    }

    @Test
    fun `isFullyHydrated is true if either document is hydrated`() {
        val unhydrated = createDocument(
            provider = "openalex",
            isFullyHydrated = false
        )
        val hydrated = createDocument(
            provider = "semanticscholar",
            isFullyHydrated = true
        )

        val merged = ResultMerger.merge(unhydrated, hydrated)

        merged.isFullyHydrated shouldBe true
    }

    @Test
    fun `highest retrieval confidence wins`() {
        val doc1 = createDocument(
            provider = "openalex",
            retrievalConfidence = 0.8
        )
        val doc2 = createDocument(
            provider = "crossref",
            retrievalConfidence = 1.0
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.retrievalConfidence shouldBe 1.0
    }

    @Test
    fun `authors are merged and deduplicated by name`() {
        val doc1 = createDocument(
            provider = "openalex",
            authors = listOf(
                Author(name = "Jane Doe", orcid = "0000-0001-2345-6789"),
                Author(name = "John Smith", orcid = null)
            )
        )
        val doc2 = createDocument(
            provider = "crossref",
            authors = listOf(
                Author(name = "Jane Doe", orcid = null),  // Duplicate, but missing ORCID
                Author(name = "Bob Wilson", orcid = "0000-0002-3456-7890")
            )
        )

        val merged = ResultMerger.merge(doc1, doc2)

        merged.authors shouldHaveSize 3
        val janeDoe = merged.authors.find { it.name == "Jane Doe" }
        janeDoe?.orcid shouldBe "0000-0001-2345-6789"  // ORCID from first source preserved
    }

    @Test
    fun `author ORCID is enriched from second source when missing`() {
        val doc1 = createDocument(
            provider = "semanticscholar",
            authors = listOf(Author(name = "Jane Doe", orcid = null))
        )
        val doc2 = createDocument(
            provider = "crossref",
            authors = listOf(Author(name = "Jane Doe", orcid = "0000-0001-2345-6789"))
        )

        val merged = ResultMerger.merge(doc1, doc2)

        val janeDoe = merged.authors.find { it.name.equals("Jane Doe", ignoreCase = true) }
        janeDoe?.orcid shouldBe "0000-0001-2345-6789"
    }

    // Helper function to create test documents with custom fields
    private fun createDocument(
        lumenId: String = "test:123",
        provider: String,
        title: String = "Test Document",
        doi: String? = "10.1234/test",
        authors: List<Author> = emptyList(),
        publicationYear: Int? = 2024,
        venue: String? = null,
        citationCount: Int = 0,
        pdfUrl: String? = null,
        abstract: String? = null,
        tldr: String? = null,
        concepts: List<Concept> = emptyList(),
        references: List<String> = emptyList(),
        citations: List<String> = emptyList(),
        rawSourceData: Map<String, kotlinx.serialization.json.JsonObject> = emptyMap(),
        isFullyHydrated: Boolean = false,
        retrievalConfidence: Double = 1.0
    ): ScholarlyDocument {
        return ScholarlyDocument(
            lumenId = lumenId,
            doi = doi,
            sourceProvider = provider,
            title = title,
            authors = authors,
            publicationYear = publicationYear,
            venue = venue,
            citationCount = citationCount,
            pdfUrl = pdfUrl,
            abstract = abstract,
            tldr = tldr,
            concepts = concepts,
            references = references,
            citations = citations,
            rawSourceData = rawSourceData,
            isFullyHydrated = isFullyHydrated,
            retrievalConfidence = retrievalConfidence
        )
    }
}

