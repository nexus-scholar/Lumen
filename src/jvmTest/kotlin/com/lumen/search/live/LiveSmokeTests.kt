package com.lumen.search.live

import com.lumen.search.data.providers.openalex.OpenAlexSearchProvider
import com.lumen.search.data.providers.semanticscholar.SemanticScholarProvider
import com.lumen.search.data.providers.crossref.CrossrefProvider
import com.lumen.search.data.providers.arxiv.ArxivProvider
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.ports.ProviderResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.util.concurrent.TimeUnit

/**
 * Live smoke tests that hit real APIs.
 *
 * These tests are DISABLED by default and only run when:
 * 1. The LUMEN_LIVE_TESTS environment variable is set to "true"
 * 2. You run with: ./gradlew jvmTest -PincludeTags=live
 *
 * Use sparingly to avoid rate limiting!
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "LUMEN_LIVE_TESTS", matches = "true")
class LiveSmokeTests {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `OpenAlex API responds with expected schema`() = runBlocking {
        val provider = OpenAlexSearchProvider(httpClient)
        val intent = SearchIntent(query = "machine learning", maxResults = 1)

        val result = provider.search(intent).first()

        result.shouldBeInstanceOf<ProviderResult.Success>()
        val success = result as ProviderResult.Success

        // Validate response structure
        success.documents.shouldNotBeEmpty()
        success.totalCount shouldNotBe 0

        val doc = success.documents.first()
        doc.sourceProvider shouldBe "openalex"
        doc.title.isNotBlank().shouldBeTrue()
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `Semantic Scholar API responds with expected schema`() = runBlocking {
        val provider = SemanticScholarProvider(httpClient)
        val intent = SearchIntent(query = "deep learning", maxResults = 1)

        val result = provider.search(intent).first()

        result.shouldBeInstanceOf<ProviderResult.Success>()
        val success = result as ProviderResult.Success

        success.documents.shouldNotBeEmpty()

        val doc = success.documents.first()
        doc.sourceProvider shouldBe "semanticscholar"
        doc.title.isNotBlank().shouldBeTrue()
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `Crossref API responds with expected schema`() = runBlocking {
        val provider = CrossrefProvider(httpClient)
        val intent = SearchIntent(query = "climate change", maxResults = 1)

        val result = provider.search(intent).first()

        result.shouldBeInstanceOf<ProviderResult.Success>()
        val success = result as ProviderResult.Success

        success.documents.shouldNotBeEmpty()

        val doc = success.documents.first()
        doc.sourceProvider shouldBe "crossref"
        doc.doi shouldNotBe null  // Crossref always has DOI
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `ArXiv API responds with expected schema`() = runBlocking {
        val provider = ArxivProvider(httpClient)
        val intent = SearchIntent(query = "neural network", maxResults = 1)

        val result = provider.search(intent).first()

        result.shouldBeInstanceOf<ProviderResult.Success>()
        val success = result as ProviderResult.Success

        success.documents.shouldNotBeEmpty()

        val doc = success.documents.first()
        doc.sourceProvider shouldBe "arxiv"
        doc.pdfUrl shouldNotBe null  // ArXiv always has PDF
    }

    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `OpenAlex fetchDetails returns full document`() = runBlocking {
        val provider = OpenAlexSearchProvider(httpClient)

        // First, get a document ID from search
        val searchIntent = SearchIntent(query = "CRISPR", maxResults = 1)
        val searchResult = provider.search(searchIntent).first() as ProviderResult.Success
        val lumenId = searchResult.documents.first().lumenId

        // Then fetch full details
        val fullDoc = provider.fetchDetails(lumenId.removePrefix("oa:"))

        fullDoc shouldNotBe null
        fullDoc?.abstract shouldNotBe null
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `OpenAlex getStats returns year distribution`() = runBlocking {
        val provider = OpenAlexSearchProvider(httpClient)
        val intent = SearchIntent(query = "diabetes treatment")

        val stats = provider.getStats(intent)

        stats.totalCount shouldNotBe 0
        // May or may not have year distribution depending on query
    }
}

