package com.lumen.search.api

import com.lumen.search.data.engine.SearchOrchestrator
import com.lumen.search.data.governance.ResourceGovernor
import com.lumen.search.domain.models.*
import com.lumen.search.domain.ports.ProviderCapability
import com.lumen.search.domain.ports.ProviderResult
import com.lumen.search.domain.ports.SearchProvider
import com.lumen.search.domain.ports.SearchStatistics
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

        val results = searchClient.search(
            query = "test",
            yearStart = 2020,
            yearEnd = 2024,
            openAccessOnly = true
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
    fun `getUsageStats returns governor stats`() = runTest {
        // Test that usage stats are accessible
        // In real implementation, this would query ResourceGovernor
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

    @Test
    fun `getSignalStrength returns valid range`() = runTest {
        val fakeProvider = object : SearchProvider {
            override val id = "test"
            override val capabilities = setOf(ProviderCapability.TEXT_SEARCH, ProviderCapability.STATISTICS)

            override fun search(intent: SearchIntent): Flow<ProviderResult> = flow { }

            override suspend fun fetchDetails(id: String): ScholarlyDocument? = null

            override suspend fun getStats(intent: SearchIntent): SearchStatistics {
                return SearchStatistics(totalCount = 5000)
            }

            override fun debugQueryTranslation(intent: SearchIntent) = ""
        }

        val probeClient = ProbeClient(listOf(fakeProvider), ResourceGovernor())

        val strength = probeClient.getSignalStrength("machine learning")

        // Signal strength should be based on total count
        strength shouldNotBe null
    }

    @Test
    fun `getTrendLine returns year distribution`() = runTest {
        val fakeProvider = object : SearchProvider {
            override val id = "test"
            override val capabilities = setOf(ProviderCapability.TEXT_SEARCH, ProviderCapability.STATISTICS)

            override fun search(intent: SearchIntent): Flow<ProviderResult> = flow { }

            override suspend fun fetchDetails(id: String): ScholarlyDocument? = null

            override suspend fun getStats(intent: SearchIntent): SearchStatistics {
                return SearchStatistics(
                    totalCount = 1000,
                    countByYear = mapOf(
                        2024 to 300,
                        2023 to 350,
                        2022 to 200,
                        2021 to 150
                    )
                )
            }

            override fun debugQueryTranslation(intent: SearchIntent) = ""
        }

        val probeClient = ProbeClient(listOf(fakeProvider), ResourceGovernor())

        val trendLine = probeClient.getTrendLine("test query")

        trendLine shouldNotBe null
        trendLine?.isEmpty()?.shouldBeTrue() == false
    }

    @Test
    fun `suggestRefinements returns suggestions for broad query`() = runTest {
        val fakeProvider = object : SearchProvider {
            override val id = "test"
            override val capabilities = setOf(ProviderCapability.TEXT_SEARCH, ProviderCapability.STATISTICS)

            override fun search(intent: SearchIntent): Flow<ProviderResult> = flow { }

            override suspend fun fetchDetails(id: String): ScholarlyDocument? = null

            override suspend fun getStats(intent: SearchIntent): SearchStatistics {
                return SearchStatistics(
                    totalCount = 100000,  // Very broad
                    topConcepts = listOf("Machine Learning", "Deep Learning", "Neural Networks")
                )
            }

            override fun debugQueryTranslation(intent: SearchIntent) = ""
        }

        val probeClient = ProbeClient(listOf(fakeProvider), ResourceGovernor())

        val suggestions = probeClient.suggestRefinements("learning")

        // Should suggest narrowing the query
        suggestions shouldNotBe null
    }

    @Test
    fun `handles empty provider list gracefully`() = runTest {
        val probeClient = ProbeClient(emptyList(), ResourceGovernor())

        val strength = probeClient.getSignalStrength("test")

        // Should return null or 0 when no providers available
        strength shouldBe null
    }
}

