package com.lumen.search.di

import com.lumen.search.api.ProbeClient
import com.lumen.search.api.SearchClient
import com.lumen.search.data.engine.SearchOrchestrator
import com.lumen.search.data.governance.ResourceGovernor
import com.lumen.search.domain.ports.SearchProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchModuleTest : KoinComponent {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `search module provides SearchClient`() {
        startKoin {
            modules(searchModule)
        }

        val searchClient: SearchClient by inject()

        searchClient shouldNotBe null
        searchClient.shouldBeInstanceOf<SearchClient>()
    }

    @Test
    fun `search module provides ProbeClient`() {
        startKoin {
            modules(searchModule)
        }

        val probeClient: ProbeClient by inject()

        probeClient shouldNotBe null
        probeClient.shouldBeInstanceOf<ProbeClient>()
    }

    @Test
    fun `search module provides SearchOrchestrator`() {
        startKoin {
            modules(searchModule)
        }

        val orchestrator: SearchOrchestrator by inject()

        orchestrator shouldNotBe null
    }

    @Test
    fun `search module provides ResourceGovernor`() {
        startKoin {
            modules(searchModule)
        }

        val governor: ResourceGovernor by inject()

        governor shouldNotBe null
    }

    @Test
    fun `search module provides shared HttpClient`() {
        startKoin {
            modules(searchModule)
        }

        val client: HttpClient by inject()

        client shouldNotBe null
    }

    @Test
    fun `search module provides list of SearchProviders`() {
        startKoin {
            modules(searchModule)
        }

        val providers: List<SearchProvider> by inject()

        providers shouldNotBe null
        providers shouldHaveAtLeastSize 1  // At least OpenAlex
    }

    @Test
    fun `HttpClient is singleton across injections`() {
        startKoin {
            modules(searchModule)
        }

        val client1: HttpClient by inject()
        val client2: HttpClient by inject()

        // Should be the same instance (connection pooling)
        client1 shouldBe client2
    }

    @Test
    fun `ResourceGovernor is singleton`() {
        startKoin {
            modules(searchModule)
        }

        val gov1: ResourceGovernor by inject()
        val gov2: ResourceGovernor by inject()

        gov1 shouldBe gov2
    }

    @Test
    fun `all providers are properly injected`() {
        startKoin {
            modules(searchModule)
        }

        val providers: List<SearchProvider> by inject()

        // Check that expected providers are present
        val providerIds = providers.map { it.id }
        providerIds.contains("openalex") shouldBe true
    }
}

