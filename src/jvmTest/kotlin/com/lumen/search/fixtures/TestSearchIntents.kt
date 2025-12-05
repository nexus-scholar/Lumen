package com.lumen.search.fixtures

import com.lumen.search.domain.models.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Test search intents for various scenarios.
 */
object TestSearchIntents {

    val SIMPLE = SearchIntent(
        query = "machine learning",
        mode = SearchMode.DISCOVERY,
        maxResults = 25,
        offset = 0,
        filters = SearchFilters()
    )

    val DIABETES_METFORMIN = SearchIntent(
        query = "metformin type 2 diabetes glucose",
        mode = SearchMode.DISCOVERY,
        maxResults = 25,
        offset = 0,
        filters = SearchFilters(
            yearStart = 2018,
            yearEnd = 2024,
            documentTypes = listOf(DocumentType.JOURNAL_ARTICLE),
            openAccessOnly = false,
            hasPdf = false
        )
    )

    val ENRICHMENT_MODE = SearchIntent(
        query = "deep learning neural networks",
        mode = SearchMode.ENRICHMENT,
        maxResults = 10,
        offset = 0,
        filters = SearchFilters()
    )

    val WITH_ALL_FILTERS = SearchIntent(
        query = "cancer treatment immunotherapy",
        mode = SearchMode.DISCOVERY,
        maxResults = 50,
        offset = 0,
        filters = SearchFilters(
            yearStart = 2020,
            yearEnd = 2024,
            documentTypes = listOf(DocumentType.JOURNAL_ARTICLE, DocumentType.REVIEW),
            venues = listOf("Nature", "Science", "Cell"),
            concepts = listOf("Oncology", "Immunotherapy"),
            openAccessOnly = true,
            hasPdf = true
        )
    )

    val PAGINATED = SearchIntent(
        query = "climate change",
        mode = SearchMode.DISCOVERY,
        maxResults = 10,
        offset = 20,
        filters = SearchFilters()
    )

    val EMPTY_QUERY = SearchIntent(
        query = "",
        mode = SearchMode.DISCOVERY,
        maxResults = 25,
        offset = 0,
        filters = SearchFilters()
    )
}

/**
 * Sample scholarly documents for test assertions.
 */
object TestDocuments {

    val COMPLETE_OPENALEX = ScholarlyDocument(
        lumenId = "oa:W2741809807",
        doi = "10.1038/s41586-019-1666-5",
        sourceProvider = "openalex",
        title = "The effect of metformin on type 2 diabetes: A systematic review",
        authors = listOf(
            Author(name = "Jane Doe", orcid = "0000-0001-2345-6789", affiliation = "Harvard University"),
            Author(name = "John Smith", orcid = null, affiliation = "MIT")
        ),
        publicationYear = 2019,
        venue = "Nature",
        citationCount = 150,
        pdfUrl = "https://www.nature.com/articles/s41586-019-1666-5.pdf",
        abstract = "Background: Metformin is the first-line treatment for type 2 diabetes mellitus. This systematic review examines its efficacy and safety profile.",
        tldr = null,
        concepts = listOf(
            Concept(name = "Diabetes mellitus", score = 0.95, id = "C71924100"),
            Concept(name = "Metformin", score = 0.92, id = "C502942594")
        ),
        references = listOf("10.1056/NEJMoa1801550", "10.1016/S0140-6736(17)32152-9"),
        citations = listOf("10.1038/s41591-020-0963-6"),
        rawSourceData = mapOf(
            "openalex" to buildJsonObject {
                put("id", JsonPrimitive("https://openalex.org/W2741809807"))
                put("type", JsonPrimitive("journal-article"))
            }
        ),
        isFullyHydrated = true,
        retrievalConfidence = 1.0,
        mergedFromIds = emptyList()
    )

    val COMPLETE_SEMANTICSCHOLAR = ScholarlyDocument(
        lumenId = "ss:abc123def456",
        doi = "10.1038/s41586-019-1666-5",
        sourceProvider = "semanticscholar",
        title = "The effect of metformin on type 2 diabetes",
        authors = listOf(
            Author(name = "Jane Doe", orcid = null, affiliation = null)
        ),
        publicationYear = 2019,
        venue = "Nature",
        citationCount = 148,
        pdfUrl = null,
        abstract = "Background: Metformin is the first-line treatment...",
        tldr = "This study shows metformin effectively reduces blood glucose levels in T2D patients.",
        concepts = emptyList(),
        references = emptyList(),
        citations = emptyList(),
        rawSourceData = mapOf(
            "semanticscholar" to buildJsonObject {
                put("paperId", JsonPrimitive("abc123def456"))
                put("corpusId", JsonPrimitive(12345678))
            }
        ),
        isFullyHydrated = true,
        retrievalConfidence = 1.0,
        mergedFromIds = emptyList()
    )

    val MINIMAL = ScholarlyDocument(
        lumenId = "oa:W9999999999",
        doi = null,
        sourceProvider = "openalex",
        title = "Untitled Work",
        authors = emptyList(),
        publicationYear = null,
        venue = null,
        citationCount = 0,
        pdfUrl = null,
        abstract = null,
        tldr = null,
        concepts = emptyList(),
        references = emptyList(),
        citations = emptyList(),
        rawSourceData = emptyMap(),
        isFullyHydrated = false,
        retrievalConfidence = 0.5,
        mergedFromIds = emptyList()
    )

    val CROSSREF_DOCUMENT = ScholarlyDocument(
        lumenId = "cr:10.1038/s41586-019-1666-5",
        doi = "10.1038/s41586-019-1666-5",
        sourceProvider = "crossref",
        title = "The Effect of Metformin on Type 2 Diabetes: A Systematic Review",
        authors = listOf(
            Author(name = "Jane Doe", orcid = "0000-0001-2345-6789", affiliation = "Harvard University")
        ),
        publicationYear = 2019,
        venue = "Nature",
        citationCount = 0, // Crossref doesn't provide citation counts
        pdfUrl = "https://www.nature.com/articles/s41586-019-1666-5.pdf",
        abstract = null, // Crossref often lacks abstracts
        rawSourceData = mapOf(
            "crossref" to buildJsonObject {
                put("DOI", JsonPrimitive("10.1038/s41586-019-1666-5"))
                put("type", JsonPrimitive("journal-article"))
            }
        ),
        isFullyHydrated = false
    )

    val ARXIV_DOCUMENT = ScholarlyDocument(
        lumenId = "arxiv:2401.12345",
        doi = null,
        sourceProvider = "arxiv",
        title = "Deep Learning for Medical Image Analysis",
        authors = listOf(
            Author(name = "Alice Johnson", orcid = null, affiliation = "Stanford University")
        ),
        publicationYear = 2024,
        venue = "arXiv",
        citationCount = 0,
        pdfUrl = "https://arxiv.org/pdf/2401.12345.pdf",
        abstract = "We present a novel deep learning approach for medical image analysis...",
        rawSourceData = mapOf(
            "arxiv" to buildJsonObject {
                put("id", JsonPrimitive("2401.12345"))
                put("primary_category", JsonPrimitive("cs.CV"))
            }
        ),
        isFullyHydrated = true
    )
}

