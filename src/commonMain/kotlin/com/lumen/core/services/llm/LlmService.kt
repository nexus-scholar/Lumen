package com.lumen.core.services.llm

import kotlinx.serialization.KSerializer

/**
 * Service for LLM-powered operations
 */
interface LlmService {
    /**
     * The name/identifier of the LLM model being used
     */
    val modelName: String

    /**
     * Generate structured output from LLM
     * @param prompt The prompt to send to the LLM
     * @param schema The serializer for the expected output type
     * @param temperature Sampling temperature (0.0-2.0)
     * @return The parsed structured output
     */
    suspend fun <T> generateStructured(
        prompt: String,
        schema: KSerializer<T>,
        temperature: Double = 0.2
    ): T

    /**
     * Generate synonyms for a term
     * @param term The term to expand
     * @param context Additional context for generation
     * @return List of synonyms
     */
    suspend fun generateSynonyms(
        term: String,
        context: String? = null
    ): List<String>

    /**
     * Refine a search query based on test results
     * @param query The original query
     * @param analysis Analysis of test search results
     * @param iteration Current iteration number
     * @return Refined query and explanation
     */
    suspend fun refineQuery(
        query: String,
        analysis: String,
        iteration: Int
    ): QueryRefinement

    /**
     * Check if LLM service is available
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Result of query refinement
 */
data class QueryRefinement(
    val refinedQuery: String,
    val explanation: String,
    val changes: List<String>
)

