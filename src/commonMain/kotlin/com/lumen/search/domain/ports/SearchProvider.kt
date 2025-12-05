package com.lumen.search.domain.ports

import com.lumen.search.domain.models.ScholarlyDocument
import com.lumen.search.domain.models.SearchIntent
import kotlinx.coroutines.flow.Flow

/**
 * Interface for scholarly data providers (OpenAlex, Semantic Scholar, Crossref, ArXiv).
 *
 * Each provider implementation encapsulates API-specific details and maps
 * responses to the unified [ScholarlyDocument] model with Sidecar preservation.
 */
interface SearchProvider {
    /** Unique provider identifier (e.g., "openalex", "semanticscholar") */
    val id: String

    /** Declared capabilities of this provider */
    val capabilities: Set<ProviderCapability>

    /**
     * Phase 1: Streamed Discovery Search
     *
     * Returns results as a Flow for streaming/pagination.
     * Must respect [SearchIntent.mode] (Lite vs Deep payload).
     *
     * @param intent The search parameters
     * @return Flow of provider results (success with documents or error)
     */
    fun search(intent: SearchIntent): Flow<ProviderResult>

    /**
     * Phase 2: Direct Enrichment Fetch
     *
     * Fetches full metadata for a specific document ID.
     * Used for on-demand hydration when user interacts with a document.
     *
     * @param id Provider-specific document ID
     * @return Fully hydrated document or null if not found
     */
    suspend fun fetchDetails(id: String): ScholarlyDocument?

    /**
     * Returns statistics for a query without fetching documents.
     * Used by ResearchProbe for feasibility checks.
     *
     * @param intent The search parameters
     * @return Statistics about matching documents
     */
    suspend fun getStats(intent: SearchIntent): SearchStatistics

    /**
     * Debugging: Returns the actual query that will be sent to the API.
     * Allows AI agents to understand query translation.
     *
     * @param intent The search parameters
     * @return Human-readable explanation of the API query
     */
    fun debugQueryTranslation(intent: SearchIntent): String

    /**
     * Checks if this provider can handle the given intent
     */
    fun supports(intent: SearchIntent): Boolean {
        return capabilities.containsAll(requiredCapabilities(intent))
    }

    /**
     * Determines required capabilities from SearchIntent
     */
    private fun requiredCapabilities(intent: SearchIntent): Set<ProviderCapability> {
        val required = mutableSetOf(ProviderCapability.TEXT_SEARCH)

        if (intent.filters.yearRange != null) required += ProviderCapability.YEAR_FILTER
        if (intent.filters.documentTypes.isNotEmpty()) required += ProviderCapability.TYPE_FILTER
        if (intent.filters.venues.isNotEmpty()) required += ProviderCapability.VENUE_FILTER
        if (intent.filters.concepts.isNotEmpty()) required += ProviderCapability.CONCEPT_FILTER

        return required
    }
}

