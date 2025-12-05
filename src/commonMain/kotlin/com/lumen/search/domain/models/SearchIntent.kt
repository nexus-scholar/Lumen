package com.lumen.search.domain.models

import kotlinx.serialization.Serializable

/**
 * Encapsulates search request parameters.
 * Avoids string parsing in the engine by using structured objects.
 */
@Serializable
data class SearchIntent(
    /** The search query string */
    val query: String,

    /** Optional filters to narrow results */
    val filters: SearchFilters = SearchFilters(),

    /**
     * Determines payload size and behavior:
     * - DISCOVERY: Fast, lite fields, pagination (Phase 1)
     * - ENRICHMENT: Deep fetch for specific IDs (Phase 2)
     */
    val mode: SearchMode = SearchMode.DISCOVERY,

    /**
     * Context hint for AI optimization.
     * e.g., "medical_research" might trigger MeSH term expansion
     */
    val domainContext: String? = null,

    /** Maximum number of results to return per provider */
    val maxResults: Int = 50,

    /** Pagination offset */
    val offset: Int = 0
)

