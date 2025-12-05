package com.lumen.search.providers.crossref

import com.lumen.search.data.providers.crossref.CrossrefProvider
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.ports.ProviderCapability
import com.lumen.search.domain.ports.ProviderResult
import com.lumen.search.fixtures.CrossrefFixtures
import com.lumen.search.testutils.MockEngineFactory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CrossrefProviderTest {

    @Test
    fun `provider has correct id`() {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)

        provider.id shouldBe "crossref"
    }

    @Test
    fun `provider declares expected capabilities`() {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)

        provider.capabilities shouldContain ProviderCapability.TEXT_SEARCH
        provider.capabilities shouldContain ProviderCapability.YEAR_FILTER
        provider.capabilities shouldContain ProviderCapability.TYPE_FILTER
        // Crossref is authoritative for dates and titles
    }

    @Test
    fun `search returns documents for valid query`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "metformin diabetes")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Success>()

        val success = results[0] as ProviderResult.Success
        success.documents.shouldNotBeEmpty()
        success.totalCount shouldBe 5200
    }

    @Test
    fun `search returns empty results for no matches`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_EMPTY)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "nonexistent query")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        success.documents.size shouldBe 0
        success.totalCount shouldBe 0
    }

    @Test
    fun `documents have correct sourceProvider`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.sourceProvider shouldBe "crossref"
        }
    }

    @Test
    fun `documents have lumenId starting with cr prefix`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.lumenId shouldStartWith "cr:"
        }
    }

    @Test
    fun `search handles 503 service unavailable`() = runTest {
        val client = MockEngineFactory.createForCrossref(
            searchResponse = CrossrefFixtures.ERROR_RESPONSE_503,
            searchStatusCode = HttpStatusCode.ServiceUnavailable
        )
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Error>()

        val error = results[0] as ProviderResult.Error
        error.providerId shouldBe "crossref"
        error.canRetry.shouldBeTrue()
    }

    @Test
    fun `fetchDetails returns document by DOI`() = runTest {
        val client = MockEngineFactory.createForCrossref(
            searchResponse = CrossrefFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = CrossrefFixtures.WORK_COMPLETE
        )
        val provider = CrossrefProvider(client)

        val document = provider.fetchDetails("10.1038/s41586-019-1666-5")

        document shouldNotBe null
        document?.doi shouldBe "10.1038/s41586-019-1666-5"
        document?.publicationYear shouldBe 2019
    }

    @Test
    fun `fetchDetails returns null for non-existent DOI`() = runTest {
        val client = MockEngineFactory.createForCrossref(
            searchResponse = CrossrefFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = CrossrefFixtures.ERROR_RESPONSE_404,
            detailsStatusCode = HttpStatusCode.NotFound
        )
        val provider = CrossrefProvider(client)

        val document = provider.fetchDetails("10.9999/nonexistent")

        document shouldBe null
    }

    @Test
    fun `maps author ORCID correctly`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        val authorWithOrcid = doc.authors.find { it.orcid != null }
        authorWithOrcid shouldNotBe null
        authorWithOrcid?.orcid shouldBe "0000-0001-2345-6789"
    }

    @Test
    fun `preserves raw JSON in sidecar`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.rawSourceData.containsKey("crossref").shouldBeTrue()
        }
    }

    @Test
    fun `handles minimal work response gracefully`() = runTest {
        val minimalResponse = """
        {
            "status": "ok",
            "message-type": "work-list",
            "message": {
                "total-results": 1,
                "items-per-page": 25,
                "items": [${CrossrefFixtures.WORK_MINIMAL.substringAfter("\"message\": ").substringBeforeLast("}")}]
            }
        }
        """.trimIndent()

        val client = MockEngineFactory.createForCrossref(minimalResponse)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        // Should not crash even with minimal data
    }

    @Test
    fun `constructs author name from given and family`() = runTest {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        val author = doc.authors.first()
        // Should combine given + family into full name
        author.name.contains("Jane").shouldBeTrue()
        author.name.contains("Doe").shouldBeTrue()
    }

    @Test
    fun `supports returns true for basic intent`() {
        val client = MockEngineFactory.createForCrossref(CrossrefFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = CrossrefProvider(client)
        val intent = SearchIntent(query = "test")

        provider.supports(intent).shouldBeTrue()
    }
}

