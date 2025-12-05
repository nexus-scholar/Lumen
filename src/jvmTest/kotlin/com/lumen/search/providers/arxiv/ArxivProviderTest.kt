package com.lumen.search.providers.arxiv

import com.lumen.search.data.providers.arxiv.ArxivProvider
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.ports.ProviderCapability
import com.lumen.search.domain.ports.ProviderResult
import com.lumen.search.fixtures.ArxivFixtures
import com.lumen.search.testutils.MockEngineFactory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ArxivProviderTest {

    @Test
    fun `provider has correct id`() {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)

        provider.id shouldBe "arxiv"
    }

    @Test
    fun `provider declares expected capabilities`() {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)

        provider.capabilities shouldContain ProviderCapability.TEXT_SEARCH
        provider.capabilities shouldContain ProviderCapability.ABSTRACTS
        provider.capabilities shouldContain ProviderCapability.PDF_URLS
    }

    @Test
    fun `search returns documents for valid query`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "machine learning")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Success>()

        val success = results[0] as ProviderResult.Success
        success.documents.shouldNotBeEmpty()
        success.totalCount shouldBe 125000
    }

    @Test
    fun `search returns empty results for no matches`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_EMPTY)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "nonexistent query")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        success.documents.size shouldBe 0
        success.totalCount shouldBe 0
    }

    @Test
    fun `documents have correct sourceProvider`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.sourceProvider shouldBe "arxiv"
        }
    }

    @Test
    fun `documents have lumenId starting with arxiv prefix`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.lumenId shouldStartWith "arxiv:"
        }
    }

    @Test
    fun `search handles 503 rate limit`() = runTest {
        val client = MockEngineFactory.createForArxiv(
            response = ArxivFixtures.RATE_LIMIT_RESPONSE,
            statusCode = HttpStatusCode.ServiceUnavailable
        )
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        results[0].shouldBeInstanceOf<ProviderResult.Error>()

        val error = results[0] as ProviderResult.Error
        error.providerId shouldBe "arxiv"
        error.canRetry.shouldBeTrue()
    }

    @Test
    fun `parses XML Atom feed correctly`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "deep learning")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        doc.title shouldContain "Deep Learning"
        doc.abstract shouldNotBe null
        doc.pdfUrl shouldNotBe null
    }

    @Test
    fun `extracts arXiv ID correctly`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        // ArXiv ID should be like 2401.12345
        doc.lumenId shouldContain "2401"
    }

    @Test
    fun `extracts PDF URL from link elements`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        doc.pdfUrl shouldContain "arxiv.org/pdf"
    }

    @Test
    fun `parses multiple authors`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        doc.authors.size shouldBe 2
        doc.authors.any { it.name.contains("Alice") }.shouldBeTrue()
        doc.authors.any { it.name.contains("Bob") }.shouldBeTrue()
    }

    @Test
    fun `extracts publication year from date`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        val doc = success.documents.first()
        doc.publicationYear shouldBe 2024
    }

    @Test
    fun `preserves raw XML data in sidecar`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.rawSourceData.containsKey("arxiv").shouldBeTrue()
        }
    }

    @Test
    fun `handles minimal entry gracefully`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.ENTRY_MINIMAL)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()

        results.shouldNotBeEmpty()
        val success = results[0] as ProviderResult.Success
        // May have 0 or 1 documents depending on parsing
        // Should not crash with minimal data
    }

    @Test
    fun `sets venue to arXiv`() = runTest {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        val results = provider.search(intent).toList()
        val success = results[0] as ProviderResult.Success

        success.documents.forEach { doc ->
            doc.venue shouldBe "arXiv"
        }
    }

    @Test
    fun `supports returns true for basic intent`() {
        val client = MockEngineFactory.createForArxiv(ArxivFixtures.SEARCH_RESPONSE_SUCCESS)
        val provider = ArxivProvider(client)
        val intent = SearchIntent(query = "test")

        provider.supports(intent).shouldBeTrue()
    }
}

