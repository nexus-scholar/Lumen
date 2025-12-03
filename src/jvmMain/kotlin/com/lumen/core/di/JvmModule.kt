package com.lumen.core.di

import com.lumen.core.config.AppConfig
import com.lumen.core.config.ConfigLoader
import com.lumen.core.data.persistence.DatabaseManager
import com.lumen.core.data.persistence.DeduplicationPersistence
import com.lumen.core.data.persistence.DocumentStore
import com.lumen.core.data.persistence.FileArtifactStore
import com.lumen.core.data.providers.openalex.OpenAlexProvider
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.stages.*
import com.lumen.core.services.llm.LlmService
import com.lumen.core.services.llm.OpenAiLlmService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Dependency injection module for JVM-specific services
 */
val jvmModule = module {
    // Configuration
    single<AppConfig> { ConfigLoader.load() }

    // HTTP Client
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    // Persistence
    single<DatabaseManager> {
        DatabaseManager(get<AppConfig>().database.sqlite.path)
    }
    single<ArtifactStore> {
        FileArtifactStore(
            baseDataPath = get<AppConfig>().database.sqlite.path,
            databaseManager = get()
        )
    }
    single {
        DocumentStore(get<DatabaseManager>())
    }
    single {
        DeduplicationPersistence(get<DatabaseManager>())
    }

    // LLM Service
    single<LlmService> {
        val config = get<AppConfig>()
        val httpClient = get<HttpClient>()

        // Determine which provider to use
        val provider = config.llm.provider

        when (provider.lowercase()) {
            "openrouter" -> {
                val apiKey = config.llm.openrouter.apiKey

                if (apiKey.isBlank()) {
                    throw IllegalStateException(
                        "OpenRouter API key not configured. " +
                        "Set OPENROUTER_API_KEY environment variable or add to application.conf\n" +
                        "Get a free API key at: https://openrouter.ai/keys"
                    )
                }

                // OpenRouter uses OpenAI-compatible API
                OpenAiLlmService(
                    apiKey = apiKey,
                    model = config.llm.openrouter.model,
                    maxTokens = config.llm.openrouter.maxTokens,
                    baseUrl = config.llm.openrouter.baseUrl,
                    httpClient = httpClient
                )
            }
            "openai" -> {
                val apiKey = config.llm.openai.apiKey

                if (apiKey.isBlank()) {
                    throw IllegalStateException(
                        "OpenAI API key not configured. " +
                        "Set OPENAI_API_KEY environment variable or add to application.conf\n" +
                        "Or use OpenRouter instead (cheaper): Set OPENROUTER_API_KEY"
                    )
                }

                OpenAiLlmService(
                    apiKey = apiKey,
                    model = config.llm.openai.model,
                    maxTokens = config.llm.openai.maxTokens,
                    httpClient = httpClient
                )
            }
            else -> throw IllegalStateException(
                "Unknown LLM provider: $provider. Supported: openai, openrouter"
            )
        }
    }

    // Search Providers
    single<OpenAlexProvider> {
        val config = get<AppConfig>()
        OpenAlexProvider(
            httpClient = get(),
            email = config.api.openalex.email.takeIf { it.isNotBlank() }
        )
    }

    // Pipeline Stages
    factory {
        ProjectSetupStage(
            artifactStore = get()
        )
    }
    factory {
        PicoExtractionStage(
            llmService = get(),
            artifactStore = get()
        )
    }
    factory {
        ConceptExpansionStage(
            llmService = get(),
            artifactStore = get()
        )
    }
    factory {
        QueryGenerationStage(
            artifactStore = get()
        )
    }
    factory {
        TestAndRefineStage(
            searchProviders = mapOf("openalex" to get<OpenAlexProvider>()),
            llmService = get(),
            artifactStore = get()
        )
    }
    factory {
        val documentStore = get<DocumentStore>()
        SearchExecutionStage(
            searchProviders = mapOf("openalex" to get<OpenAlexProvider>()),
            saveDocuments = { projectId, documents ->
                documentStore.saveDocuments(projectId, documents)
            },
            artifactStore = get()
        )
    }
    factory {
        val dedupPersistence = get<DeduplicationPersistence>()
        DeduplicationStage(
            artifactStore = get(),
            saveGroups = { projectId, groups ->
                dedupPersistence.saveGroups(projectId, groups)
            }
        )
    }
}

