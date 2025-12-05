package com.lumen.search.api

/**
 * Suggestion for refining a search query
 */
data class RefinementSuggestion(
    val originalQuery: String,
    val refinedQuery: String,
    val reason: String,
    val estimatedImpact: String
)

