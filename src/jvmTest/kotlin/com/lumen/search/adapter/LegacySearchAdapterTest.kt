package com.lumen.search.adapter

import com.lumen.search.api.SearchClient
import com.lumen.search.domain.models.ScholarlyDocument
import com.lumen.search.domain.models.SearchFilters
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LegacySearchAdapterTest {

    private lateinit var searchClient: SearchClient
    private lateinit var adapter: LegacySearchAdapter

    @BeforeEach
    fun setup() {
        searchClient = mockk()
        adapter = LegacySearchAdapter(searchClient)
    }

    @Test
    fun `search converts query to new format`() = runTest {
        val documents = listOf(createScholarlyDocument("doc1"))
        every { searchClient.search(any(), any(), any()) } returns flowOf(*documents.toTypedArray())

        val result = adapter.search(
            query = "machine learning",
            limit = 25,
            offset = 0,
            filters = emptyMap()
        )

        result.documents.shouldHaveSize(1)
    }

    @Test
    fun `search converts documents to legacy format`() = runTest {
        val scholarlyDoc = createScholarlyDocument(
            id = "oa:W123",
            title = "Test Title",
            doi = "10.1234/test"
        )
        every { searchClient.search(any(), any(), any()) } returns flowOf(scholarlyDoc)

        val result = adapter.search(
            query = "test",
            limit = 10,
            offset = 0,
            filters = emptyMap()
        )

        result.documents.shouldHaveSize(1)
        val legacyDoc = result.documents.first()
        legacyDoc.title shouldBe "Test Title"
    }

    @Test
    fun `search handles year filter from legacy format`() = runTest {
        every { searchClient.search(any(), any(), any()) } returns flowOf()

        adapter.search(
            query = "test",
            limit = 10,
            offset = 0,
            filters = mapOf(
                "yearStart" to "2020",
                "yearEnd" to "2024"
            )
        )

        // Should not throw - filters are converted
    }

    @Test
    fun `search handles open access filter`() = runTest {
        every { searchClient.search(any(), any(), any()) } returns flowOf()

        adapter.search(
            query = "test",
            limit = 10,
            offset = 0,
            filters = mapOf("openAccess" to "true")
        )

        // Should not throw
    }

    @Test
    fun `search returns execution time`() = runTest {
        every { searchClient.search(any(), any(), any()) } returns flowOf()

        val result = adapter.search(
            query = "test",
            limit = 10,
            offset = 0,
            filters = emptyMap()
        )

        result.executionTimeMs shouldNotBe null
    }

    @Test
    fun `search handles errors gracefully`() = runTest {
        every { searchClient.search(any(), any(), any()) } throws RuntimeException("API Error")

        val result = adapter.search(
            query = "test",
            limit = 10,
            offset = 0,
            filters = emptyMap()
        )

        result.documents.shouldHaveSize(0)
        result.totalCount shouldBe 0
    }

    @Test
    fun `hasMore is true when results reach limit`() = runTest {
        val documents = (1..10).map { createScholarlyDocument("doc$it") }
        every { searchClient.search(any(), any(), any()) } returns flowOf(*documents.toTypedArray())

        val result = adapter.search(
            query = "test",
            limit = 10,
            offset = 0,
            filters = emptyMap()
        )

        result.hasMore.shouldBeTrue()
    }

    @Test
    fun `getDocument returns null - not supported in new module`() = runTest {
        val result = adapter.getDocument("test-id")

        result shouldBe null
    }

    @Test
    fun `providerName returns configured name`() {
        adapter.providerName shouldBe "lumen-search"
    }

    @Test
    fun `custom providerName can be set`() {
        val customAdapter = LegacySearchAdapter(searchClient, "custom-provider")
        customAdapter.providerName shouldBe "custom-provider"
    }

    private fun createScholarlyDocument(
        id: String,
        title: String = "Test Document",
        doi: String? = null
    ): ScholarlyDocument {
        return ScholarlyDocument(
            lumenId = id,
            doi = doi,
            sourceProvider = "test",
            title = title,
            authors = emptyList(),
            publicationYear = 2024,
            venue = null,
            citationCount = 0,
            pdfUrl = null
        )
    }
}

