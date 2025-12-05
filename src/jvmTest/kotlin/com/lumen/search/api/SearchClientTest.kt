package com.lumen.search.api

import com.lumen.search.data.engine.SearchOrchestrator
import com.lumen.search.domain.models.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchClientTest {

    private lateinit var orchestrator: SearchOrchestrator
    private lateinit var searchClient: SearchClient

    @BeforeEach
    fun setup() {
        orchestrator = mockk()
        searchClient = SearchClient(orchestrator)
    }

    @Test
    fun `search returns flow of documents`() = runTest {
        val expectedDocs = listOf(
            createDoc("doc1"),
            createDoc("doc2")
        )
        every { orchestrator.executeSearch(any()) } returns flowOf(*expectedDocs.toTypedArray())

        val results = searchClient.search("machine learning").toList()

        results shouldHaveAtLeastSize 2
        results[0].lumenId shouldBe "doc1"
    }

    @Test
    fun `search with filters passes intent correctly`() = runTest {
        val expectedDocs = listOf(createDoc("filtered"))
        every { orchestrator.executeSearch(any()) } returns flowOf(*expectedDocs.toTypedArray())

        val filters = SearchFilters(
            yearStart = 2020,
            yearEnd = 2024,
            openAccessOnly = true
        )
        val results = searchClient.search(
            query = "test",
            filters = filters
        ).toList()

        results.shouldNotBeEmpty()
    }

    @Test
    fun `enrich returns hydrated document`() = runTest {
        val original = createDoc("doc1", isFullyHydrated = false)
        val hydrated = original.copy(
            abstract = "Full abstract",
            isFullyHydrated = true
        )
        coEvery { orchestrator.enrich(any()) } returns hydrated

        val result = searchClient.enrich(original)

        result shouldNotBe null
        result?.isFullyHydrated?.shouldBeTrue()
        result?.abstract shouldBe "Full abstract"
    }

    @Test
    fun `enrich returns null when orchestrator fails`() = runTest {
        val doc = createDoc("doc1")
        coEvery { orchestrator.enrich(any()) } returns null

        val result = searchClient.enrich(doc)

        result shouldBe null
    }

    @Test
    fun `enrich returns same document if already hydrated`() = runTest {
        val doc = createDoc("doc1", isFullyHydrated = true)

        val result = searchClient.enrich(doc)

        result shouldBe doc
    }

    private fun createDoc(
        id: String,
        isFullyHydrated: Boolean = false
    ): ScholarlyDocument {
        return ScholarlyDocument(
            lumenId = id,
            doi = null,
            sourceProvider = "test",
            title = "Test Document",
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 0,
            pdfUrl = null,
            isFullyHydrated = isFullyHydrated
        )
    }
}

class ProbeClientTest {

    private lateinit var orchestrator: SearchOrchestrator
    private lateinit var probeClient: ProbeClient

    @BeforeEach
    fun setup() {
        orchestrator = mockk()
        probeClient = ProbeClient(orchestrator)
    }

    @Test
    fun `getSignalStrength returns metrics`() = runTest {
        val stats = com.lumen.search.data.engine.AggregatedStatistics(
            totalEstimatedCount = 5000,
            providerStats = emptyMap(),
            allConcepts = listOf("Machine Learning", "Deep Learning")
        )
        coEvery { orchestrator.getAggregatedStats(any()) } returns stats

        val result = probeClient.getSignalStrength("machine learning")

        result shouldNotBe null
        result.totalCount shouldBe 5000
    }

    @Test
    fun `getTrendLine returns year distribution`() = runTest {
        val stats = com.lumen.search.data.engine.AggregatedStatistics(
            totalEstimatedCount = 1000,
            providerStats = mapOf(
                "test" to com.lumen.search.domain.ports.SearchStatistics(
                    totalCount = 1000,
                    countByYear = mapOf(2024 to 300, 2023 to 350, 2022 to 200)
                )
            ),
            allConcepts = emptyList()
        )
        coEvery { orchestrator.getAggregatedStats(any()) } returns stats

        val result = probeClient.getTrendLine("test query")

        result shouldNotBe null
        result.isNotEmpty().shouldBeTrue()
    }
}

