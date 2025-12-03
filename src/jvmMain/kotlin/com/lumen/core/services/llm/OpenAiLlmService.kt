package com.lumen.core.services.llm

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

/**
 * OpenAI/OpenRouter API request models
 */
@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

@Serializable
private data class Message(
    val role: String,
    val content: String
)

@Serializable
private data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
private data class Choice(
    val message: Message,
    val finish_reason: String? = null
)

@Serializable
private data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * OpenAI implementation of LLM service using direct HTTP requests
 * Also supports OpenAI-compatible APIs like OpenRouter
 */
class OpenAiLlmService(
    private val apiKey: String,
    private val model: String = "gpt-4",
    private val maxTokens: Int = 2000,
    private val maxRetries: Int = 3,
    private val baseUrl: String? = null,
    private val httpClient: HttpClient
) : LlmService {

    override val modelName: String = model

    private val endpoint = baseUrl ?: "https://api.openai.com/v1"
    private val isOpenRouter = baseUrl?.contains("openrouter") == true

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    init {
        logger.info { "OpenAI LLM Service initialized - endpoint: $endpoint, model: $model" }
    }

    override suspend fun <T> generateStructured(
        prompt: String,
        schema: KSerializer<T>,
        temperature: Double
    ): T {
        logger.info { "Generating structured output with model: $model" }

        val systemMessage = """
            You are a research methodology expert assistant.
            You MUST respond with ONLY valid JSON matching the requested schema.
            Do not include any explanatory text before or after the JSON.
        """.trimIndent()

        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemMessage),
                Message(role = "user", content = prompt)
            ),
            temperature = temperature,
            max_tokens = maxTokens
        )

        val response = callChatCompletion(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("No response from LLM")

        logger.debug { "LLM response: $content" }

        return try {
            json.decodeFromString(schema, content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse LLM response as JSON" }
            throw LlmException("Failed to parse LLM response: ${e.message}", e)
        }
    }

    override suspend fun generateSynonyms(term: String, context: String?): List<String> {
        logger.info { "Generating synonyms for term: $term" }

        val prompt = buildString {
            append("Generate a list of synonyms and related terms for: \"$term\"\n\n")
            if (context != null) {
                append("Context: $context\n\n")
            }
            append("""
                Return ONLY a JSON array of strings. Example: ["term1", "term2", "term3"]
                Include:
                - Direct synonyms
                - Related medical/scientific terms
                - Common abbreviations
                - Alternative spellings

                Limit to 10-15 most relevant terms.
            """.trimIndent())
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                Message(role = "user", content = prompt)
            ),
            temperature = 0.3,
            max_tokens = 500
        )

        val response = callChatCompletion(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("No response from LLM")

        return try {
            json.decodeFromString<List<String>>(content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse synonyms response" }
            emptyList()
        }
    }

    override suspend fun refineQuery(
        query: String,
        analysis: String,
        iteration: Int
    ): QueryRefinement {
        logger.info { "Refining query (iteration $iteration)" }

        val prompt = """
            You are helping refine a systematic review search query.

            Original Query:
            $query

            Analysis of Test Results:
            $analysis

            This is iteration $iteration of refinement.

            Please suggest improvements to the query and return a JSON object with this structure:
            {
              "refinedQuery": "the improved query",
              "explanation": "explanation of what was changed and why",
              "changes": ["change 1", "change 2", "change 3"]
            }
        """.trimIndent()

        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                Message(role = "user", content = prompt)
            ),
            temperature = 0.3,
            max_tokens = 1000
        )

        val response = callChatCompletion(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("No response from LLM")

        return try {
            json.decodeFromString<QueryRefinement>(content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse query refinement response" }
            QueryRefinement(
                refinedQuery = query,
                explanation = "Could not parse LLM response",
                changes = emptyList()
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        // For custom endpoints (like OpenRouter), we assume they're available
        // Actual availability will be determined on first API call
        if (baseUrl != null) {
            logger.info { "Skipping availability check for custom endpoint: $endpoint" }
            return true
        }

        // For OpenAI, we could add a health check here if needed
        return true
    }

    /**
     * Make HTTP POST request to chat completions endpoint with retry logic
     */
    private suspend fun callChatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val response: HttpResponse = httpClient.post("$endpoint/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $apiKey")

                    // Add OpenRouter-specific headers
                    if (isOpenRouter) {
                        header("HTTP-Referer", "https://github.com/lumen-systematic-review")
                        header("X-Title", "Lumen Systematic Review Tool")
                    }

                    setBody(json.encodeToString(ChatCompletionRequest.serializer(), request))
                }

                val responseBody = response.bodyAsText()
                logger.debug { "API Response: $responseBody" }

                return json.decodeFromString(ChatCompletionResponse.serializer(), responseBody)

            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delayMs = (100 * 2.0.pow(attempt)).toLong()
                    logger.warn { "API call failed (attempt ${attempt + 1}/$maxRetries), retrying in ${delayMs}ms: ${e.message}" }
                    delay(delayMs)
                } else {
                    logger.error(e) { "API call failed after $maxRetries attempts" }
                }
            }
        }

        throw LlmException(
            "LLM API call failed after $maxRetries retries: ${lastException?.message}",
            lastException
        )
    }
}

/**
 * LLM service exception
 */
class LlmException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

