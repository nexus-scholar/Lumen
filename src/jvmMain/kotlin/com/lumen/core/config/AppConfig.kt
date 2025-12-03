package com.lumen.core.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/**
 * Application configuration
 */
@Serializable
data class AppConfig(
    val app: AppInfo = AppInfo(),
    val database: DatabaseConfig = DatabaseConfig(),
    val api: ApiConfig = ApiConfig(),
    val llm: LlmConfig = LlmConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val export: ExportConfig = ExportConfig()
)

@Serializable
data class AppInfo(
    val name: String = "Lumen",
    val version: String = "0.1.0-SNAPSHOT"
)

@Serializable
data class DatabaseConfig(
    val sqlite: SqliteConfig = SqliteConfig(),
    val postgres: PostgresConfig = PostgresConfig()
)

@Serializable
data class SqliteConfig(
    val path: String = "data"
)

@Serializable
data class PostgresConfig(
    val host: String = "localhost",
    val port: Int = 5432,
    val database: String = "lumen",
    val username: String = "lumen_user",
    val password: String = "change_me",
    val maxPoolSize: Int = 10
)

@Serializable
data class ApiConfig(
    val openalex: OpenAlexConfig = OpenAlexConfig(),
    val crossref: CrossrefConfig = CrossrefConfig(),
    val semanticScholar: SemanticScholarConfig = SemanticScholarConfig(),
    val arxiv: ArxivConfig = ArxivConfig()
)

@Serializable
data class OpenAlexConfig(
    val baseUrl: String = "https://api.openalex.org",
    val email: String = ""
)

@Serializable
data class CrossrefConfig(
    val baseUrl: String = "https://api.crossref.org",
    val email: String = ""
)

@Serializable
data class SemanticScholarConfig(
    val baseUrl: String = "https://api.semanticscholar.org",
    val apiKey: String = ""
)

@Serializable
data class ArxivConfig(
    val baseUrl: String = "http://export.arxiv.org/api"
)

@Serializable
data class LlmConfig(
    val provider: String = "openrouter", // Default to OpenRouter (cheaper!)
    val openai: OpenAiConfig = OpenAiConfig(),
    val openrouter: OpenRouterConfig = OpenRouterConfig(),
    val anthropic: AnthropicConfig = AnthropicConfig()
)

@Serializable
data class OpenAiConfig(
    val apiKey: String = "",
    val model: String = "gpt-4",
    val maxTokens: Int = 2000
)

@Serializable
data class OpenRouterConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val model: String = "openai/gpt-3.5-turbo", // Cheaper model
    val maxTokens: Int = 2000
)

@Serializable
data class AnthropicConfig(
    val apiKey: String = "",
    val model: String = "claude-3-opus-20240229",
    val maxTokens: Int = 2000
)

@Serializable
data class LoggingConfig(
    val level: String = "INFO",
    val file: String = "logs/lumen.log"
)

@Serializable
data class ExportConfig(
    val outputDirectory: String = "exports",
    val formats: List<String> = listOf("csv", "bibtex", "ris", "json")
)

/**
 * Configuration loader
 */
object ConfigLoader {
    /**
     * Load configuration from application.conf file and environment variables
     * Environment variables take precedence over file config
     */
    fun load(): AppConfig {
        logger.info { "Loading configuration from environment variables and application.conf" }

        // Try to load from application.conf first
        val fileConfig = loadFromFile()

        // Determine which LLM provider to use
        val hasOpenRouterKey = System.getenv("OPENROUTER_API_KEY") != null
        val hasOpenAiKey = System.getenv("OPENAI_API_KEY") != null

        val provider = when {
            hasOpenRouterKey -> "openrouter"
            hasOpenAiKey -> "openai"
            else -> fileConfig.llm.provider
        }

        // Override file config with environment variables (env vars have priority)
        return fileConfig.copy(
            llm = fileConfig.llm.copy(
                provider = System.getenv("LLM_PROVIDER") ?: provider,
                openai = fileConfig.llm.openai.copy(
                    apiKey = System.getenv("OPENAI_API_KEY")
                        ?: fileConfig.llm.openai.apiKey
                ),
                openrouter = fileConfig.llm.openrouter.copy(
                    apiKey = System.getenv("OPENROUTER_API_KEY")
                        ?: fileConfig.llm.openrouter.apiKey,
                    model = System.getenv("OPENROUTER_MODEL")
                        ?: fileConfig.llm.openrouter.model
                )
            ),
            api = fileConfig.api.copy(
                openalex = fileConfig.api.openalex.copy(
                    email = System.getenv("OPENALEX_EMAIL")
                        ?: fileConfig.api.openalex.email
                )
            )
        )
    }

    /**
     * Load configuration from application.conf file
     * Returns default config if file doesn't exist
     */
    private fun loadFromFile(): AppConfig {
        return try {
            // Try to read application.conf from resources
            val configText = object {}.javaClass.getResourceAsStream("/application.conf")
                ?.bufferedReader()
                ?.use { it.readText() }

            if (configText != null) {
                logger.info { "Found application.conf, parsing configuration..." }
                parseHoconConfig(configText)
            } else {
                logger.warn { "application.conf not found, using defaults" }
                AppConfig()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load application.conf, using defaults: ${e.message}" }
            AppConfig()
        }
    }

    /**
     * Simple HOCON parser for our config structure
     * This is a basic parser - for production use a proper HOCON library
     */
    private fun parseHoconConfig(text: String): AppConfig {
        val config = AppConfig()

        try {
            // Extract OpenRouter API key
            val openRouterKeyMatch = Regex("""openrouter\s*\{[^}]*apiKey\s*=\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
                .find(text)
            val openRouterKey = openRouterKeyMatch?.groupValues?.get(1) ?: ""

            // Extract OpenRouter model
            val openRouterModelMatch = Regex("""openrouter\s*\{[^}]*model\s*=\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
                .find(text)
            val openRouterModel = openRouterModelMatch?.groupValues?.get(1) ?: config.llm.openrouter.model

            // Extract provider
            val providerMatch = Regex("""provider\s*=\s*"([^"]+)"""")
                .find(text)
            val provider = providerMatch?.groupValues?.get(1) ?: config.llm.provider

            // Extract OpenAlex email
            val openAlexEmailMatch = Regex("""openalex\s*\{[^}]*email\s*=\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
                .find(text)
            val openAlexEmail = openAlexEmailMatch?.groupValues?.get(1) ?: ""

            logger.info { "Parsed config - provider: $provider, openRouterKey: ${if (openRouterKey.isNotEmpty()) "***${openRouterKey.takeLast(4)}" else "not set"}" }

            return config.copy(
                llm = config.llm.copy(
                    provider = provider,
                    openrouter = config.llm.openrouter.copy(
                        apiKey = openRouterKey,
                        model = openRouterModel
                    )
                ),
                api = config.api.copy(
                    openalex = config.api.openalex.copy(
                        email = openAlexEmail
                    )
                )
            )
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing HOCON config: ${e.message}" }
            return config
        }
    }
}

