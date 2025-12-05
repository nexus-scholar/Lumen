package com.lumen.search.providers.openalex

import com.lumen.search.data.providers.openalex.OpenAlexSearchProvider
import com.lumen.search.domain.models.SearchFilters
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.models.SearchMode
import com.lumen.search.domain.ports.ProviderCapability
import com.lumen.search.domain.ports.ProviderResult
import com.lumen.search.fixtures.OpenAlexFixtures
import com.lumen.search.testutils.MockEngineFactory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OpenAlexSearchProviderTest {

    @Test
    fun `provider has correct id`() {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)

        provider.id shouldBe "openalex"
    }

    @Test
    fun `provider declares expected capabilities`() {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)

        provider.capabilities shouldContain ProviderCapability.TEXT_SEARCH
        provider.capabilities shouldContain ProviderCapability.YEAR_FILTER
        provider.capabilities shouldContain ProviderCapability.ABSTRACTS
        provider.capabilities shouldContain ProviderCapability.CITATIONS
        provider.capabilities shouldContain ProviderCapability.PAGINATION
    }

    @Test
    fun `search returns documents for valid query`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "metformin diabetes")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Success>()

        val success = results[0] as ProviderResult.Success
        success.documents.shouldNotBeEmpty()
        success.totalCount shouldBe 1250
        success.hasMore.shouldBeTrue()
    }

    @Test
    fun `search returns empty results for no matches`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_EMPTY)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "nonexistent query")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        success.documents.size shouldBe 0
        success.totalCount shouldBe 0
        success.hasMore.shouldBeFalse()
    }

    @Test
    fun `documents have correct sourceProvider`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.sourceProvider shouldBe "openalex"
        }
    }

    @Test
    fun `documents have lumenId starting with oa prefix`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.lumenId shouldStartWith "oa:"
        }
    }

    @Test
    fun `search handles 429 rate limit with retry info`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.ERROR_RESPONSE_429,
            searchStatusCode = HttpStatusCode.TooManyRequests,
            responseHeaders = headersOf("Retry-After" to listOf("5"))
        )
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Error>()

        val error = results[0] as ProviderResult.Error
        error.providerId shouldBe "openalex"
        error.canRetry.shouldBeTrue()
        error.retryAfterMs shouldBe 5000L
    }

    @Test
    fun `search handles 500 server error`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.ERROR_RESPONSE_500,
            searchStatusCode = HttpStatusCode.InternalServerError
        )
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Error>()

        val error = results[0] as ProviderResult.Error
        error.canRetry.shouldBeTrue()
    }

    @Test
    fun `search handles 404 not retryable`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = "Not Found",
            searchStatusCode = HttpStatusCode.NotFound
        )
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Error>()

        val error = results[0] as ProviderResult.Error
        error.canRetry.shouldBeFalse()
    }

    @Test
    fun `fetchDetails returns hydrated document`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = OpenAlexFixtures.WORK_COMPLETE
        )
        val provider = OpenAlexSearchProvider(client)

        val document = provider.fetchDetails("W2741809807")

        document shouldNotBe null
        document?.doi shouldBe "10.1038/s41586-019-1666-5"
        document?.title shouldBe "The effect of metformin on type 2 diabetes: A systematic review"
        document?.publicationYear shouldBe 2019
        document?.authors?.shouldHaveAtLeastSize(1)
    }

    @Test
    fun `fetchDetails returns null for non-existent ID`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = "Not Found",
            detailsStatusCode = HttpStatusCode.NotFound
        )
        val provider = OpenAlexSearchProvider(client)

        val document = provider.fetchDetails("W9999999999")

        document shouldBe null
    }

    @Test
    fun `getStats returns year distribution`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS,
            statsResponse = OpenAlexFixtures.STATS_RESPONSE
        )
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "machine learning")

        val stats = provider.getStats(intent)

        stats.totalCount shouldBe 5000
        stats.countByYear.isEmpty().shouldBeFalse()
        stats.countByYear[2024] shouldBe 1200
        stats.countByYear[2023] shouldBe 1500
    }

    @Test
    fun `debugQueryTranslation returns readable explanation`() {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(
            query = "machine learning",
            mode = SearchMode.DISCOVERY,
            filters = SearchFilters(
                yearStart = 2020,
                yearEnd = 2024,
                openAccessOnly = true
            )
        )

        val explanation = provider.debugQueryTranslation(intent)

        explanation shouldNotBe null
        explanation.contains("OpenAlex").shouldBeTrue()
        // Query is URL-encoded in the URL, so check for "machine" which won't be split
        explanation.contains("machine").shouldBeTrue()
    }

    @Test
    fun `supports returns true for basic intent`() {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        provider.supports(intent).shouldBeTrue()
    }

    @Test
    fun `supports returns true for intent with year filter`() {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(
            query = "test",
            filters = SearchFilters(yearStart = 2020, yearEnd = 2024)
        )

        provider.supports(intent).shouldBeTrue()
    }

    @Test
    fun `documents preserve raw JSON in sidecar`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        // Documents should have raw data preserved
        success.documents.forEach { doc ->
            doc.rawSourceData.containsKey("openalex").shouldBeTrue()
        }
    }

    @Test
    fun `handles minimal work response gracefully`() = runTest {
        val minimalResponse = """
        {
            "meta": {"count": 1, "page": 1, "per_page": 25},
            "results": [${OpenAlexFixtures.WORK_MINIMAL}]
        }
        """.trimIndent()

        val client = MockEngineFactory.createForOpenAlex(minimalResponse)
        val provider = OpenAlexSearchProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        success.documents.shouldNotBeEmpty()

        val doc = success.documents[0]
        doc.title shouldBe "Untitled Work"
        doc.doi shouldBe null
        doc.publicationYear shouldBe null
    }

    @Test
    fun `maps authors correctly`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = OpenAlexFixtures.WORK_COMPLETE
        )
        val provider = OpenAlexSearchProvider(client)

        val document = provider.fetchDetails("W2741809807")

        document?.authors?.shouldHaveAtLeastSize(2)
        val firstAuthor = document?.authors?.firstOrNull()
        firstAuthor?.name shouldBe "Jane Doe"
        firstAuthor?.orcid shouldBe "0000-0001-2345-6789"
    }

    @Test
    fun `maps concepts correctly`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = OpenAlexFixtures.WORK_COMPLETE
        )
        val provider = OpenAlexSearchProvider(client)

        val document = provider.fetchDetails("W2741809807")

        document?.concepts?.shouldHaveAtLeastSize(1)
        val concept = document?.concepts?.firstOrNull()
        concept?.name shouldBe "Diabetes mellitus"
        concept?.score shouldBe 0.95
    }

    @Test
    fun `reconstructs abstract from inverted index`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            searchResponse = OpenAlexFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = OpenAlexFixtures.WORK_COMPLETE
        )
        val provider = OpenAlexSearchProvider(client)

        val document = provider.fetchDetails("W2741809807")

        // The abstract should be reconstructed from the inverted index
        document?.abstract shouldNotBe null
        document?.abstract?.contains("Metformin")?.shouldBeTrue()
    }
}

