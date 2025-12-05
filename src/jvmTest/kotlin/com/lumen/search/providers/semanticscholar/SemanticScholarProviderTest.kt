package com.lumen.search.providers.semanticscholar

import com.lumen.search.data.providers.semanticscholar.SemanticScholarProvider
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.models.SearchMode
import com.lumen.search.domain.ports.ProviderCapability
import com.lumen.search.domain.ports.ProviderResult
import com.lumen.search.fixtures.SemanticScholarFixtures
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

class SemanticScholarProviderTest {

    @Test
    fun `provider has correct id`() {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)

        provider.id shouldBe "semanticscholar"
    }

    @Test
    fun `provider declares expected capabilities`() {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)

        provider.capabilities shouldContain ProviderCapability.TEXT_SEARCH
        provider.capabilities shouldContain ProviderCapability.ABSTRACTS
        provider.capabilities shouldContain ProviderCapability.TLDR
        provider.capabilities shouldContain ProviderCapability.CITATIONS
    }

    @Test
    fun `search returns documents for valid query`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "metformin diabetes")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Success>()

        val success = results[0] as ProviderResult.Success
        success.documents.shouldNotBeEmpty()
        success.totalCount shouldBe 850
    }

    @Test
    fun `search returns empty results for no matches`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_EMPTY)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "nonexistent query")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        success.documents.size shouldBe 0
        success.totalCount shouldBe 0
    }

    @Test
    fun `documents have correct sourceProvider`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.sourceProvider shouldBe "semanticscholar"
        }
    }

    @Test
    fun `documents have lumenId starting with ss prefix`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.lumenId shouldStartWith "ss:"
        }
    }

    @Test
    fun `search handles 429 rate limit`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(
            searchResponse = SemanticScholarFixtures.ERROR_RESPONSE_429,
            searchStatusCode = HttpStatusCode.TooManyRequests
        )
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Error>()

        val error = results[0] as ProviderResult.Error
        error.providerId shouldBe "semanticscholar"
        error.canRetry.shouldBeTrue()
    }

    @Test
    fun `fetchDetails returns document with TLDR`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(
            searchResponse = SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = SemanticScholarFixtures.PAPER_COMPLETE
        )
        val provider = SemanticScholarProvider(client)

        val document = provider.fetchDetails("abc123def456")

        document shouldNotBe null
        document?.tldr shouldNotBe null
        document?.tldr?.contains("metformin")?.shouldBeTrue()
    }

    @Test
    fun `fetchDetails returns null for non-existent ID`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(
            searchResponse = SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS,
            detailsResponse = "Not Found",
            detailsStatusCode = HttpStatusCode.NotFound
        )
        val provider = SemanticScholarProvider(client)

        val document = provider.fetchDetails("nonexistent")

        document shouldBe null
    }

    @Test
    fun `maps DOI correctly from externalIds`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val docWithDoi = success.documents.find { it.doi != null }
        docWithDoi shouldNotBe null
        docWithDoi?.doi shouldBe "10.1038/s41586-019-1666-5"
    }

    @Test
    fun `preserves raw JSON in sidecar`() = runTest {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.rawSourceData.containsKey("semanticscholar").shouldBeTrue()
        }
    }

    @Test
    fun `handles minimal paper response gracefully`() = runTest {
        val minimalResponse = """
        {
            "total": 1,
            "offset": 0,
            "data": [${SemanticScholarFixtures.PAPER_MINIMAL}]
        }
        """.trimIndent()

        val client = MockEngineFactory.createForSemanticScholar(minimalResponse)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        success.documents.shouldNotBeEmpty()

        val doc = success.documents[0]
        doc.title shouldBe "Untitled Paper"
        doc.abstract shouldBe null
        doc.tldr shouldBe null
    }

    @Test
    fun `supports returns true for basic intent`() {
        val client = MockEngineFactory.createForSemanticScholar(SemanticScholarFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = SemanticScholarProvider(client)
        val intent = SearchIntent(query = "test")

        provider.supports(intent).shouldBeTrue()
    }
}

