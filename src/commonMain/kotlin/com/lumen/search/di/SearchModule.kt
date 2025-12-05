package com.lumen.search.di

import com.lumen.search.api.ProbeClient
import com.lumen.search.api.SearchClient
import com.lumen.search.data.engine.ResultMerger
import com.lumen.search.data.engine.SearchOrchestrator
import com.lumen.search.data.governance.ResourceGovernor
import com.lumen.search.data.providers.arxiv.ArxivProvider
import com.lumen.search.data.providers.crossref.CrossrefProvider
import com.lumen.search.data.providers.openalex.OpenAlexSearchProvider
import com.lumen.search.data.providers.semanticscholar.SemanticScholarProvider
import com.lumen.search.domain.ports.SearchProvider
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Koin DI module for the Search Module.
 *
 * Requires: HttpClient to be provided by the main application module.
 *
 * Usage:
 * ```kotlin
 * val appModule = module {
 *     single<HttpClient> { ... }
 * }
 * startKoin {
 *     modules(appModule, searchModule)
 * }
 * ```
 */
val searchModule = module {

    // Resource Governor (Rate Limiting)
    single { ResourceGovernor() }

    // Providers
    single<OpenAlexSearchProvider> {
        OpenAlexSearchProvider(
            httpClient = get(),
            email = getOrNull() // Optional email for polite pool
        )
    }

    single<SemanticScholarProvider> {
        SemanticScholarProvider(
            httpClient = get(),
            apiKey = getOrNull() // Optional API key
        )
    }

    single<CrossrefProvider> {
        CrossrefProvider(
            httpClient = get(),
            email = getOrNull()
        )
    }

    single<ArxivProvider> {
        ArxivProvider(
            httpClient = get()
        )
    }

    // Provider List
    single<List<SearchProvider>> {
        listOf(
            get<OpenAlexSearchProvider>(),
            get<SemanticScholarProvider>(),
            get<CrossrefProvider>(),
            get<ArxivProvider>()
        )
    }

    // Orchestrator
    single {
        SearchOrchestrator(
            providers = get(),
            merger = ResultMerger,
            governor = get()
        )
    }

    // Public API
    single { SearchClient(get()) }
    single { ProbeClient(get()) }
}

/**
 * Creates a search module with custom configuration.
 *
 * @param openAlexEmail Email for OpenAlex polite pool
 * @param crossrefEmail Email for Crossref polite pool
 * @param semanticScholarApiKey API key for Semantic Scholar
 */
fun createSearchModule(
    openAlexEmail: String? = null,
    crossrefEmail: String? = null,
    semanticScholarApiKey: String? = null
) = module {

    // Resource Governor
    single { ResourceGovernor() }

    // Providers with configuration
    single<OpenAlexSearchProvider> {
        OpenAlexSearchProvider(
            httpClient = get(),
            email = openAlexEmail
        )
    }

    single<SemanticScholarProvider> {
        SemanticScholarProvider(
            httpClient = get(),
            apiKey = semanticScholarApiKey
        )
    }

    single<CrossrefProvider> {
        CrossrefProvider(
            httpClient = get(),
            email = crossrefEmail
        )
    }

    single<ArxivProvider> {
        ArxivProvider(httpClient = get())
    }

    // Provider List
    single<List<SearchProvider>> {
        listOf(
            get<OpenAlexSearchProvider>(),
            get<SemanticScholarProvider>(),
            get<CrossrefProvider>(),
            get<ArxivProvider>()
        )
    }

    // Orchestrator
    single {
        SearchOrchestrator(
            providers = get(),
            merger = ResultMerger,
            governor = get()
        )
    }

    // Public API
    single { SearchClient(get()) }
    single { ProbeClient(get()) }
}

