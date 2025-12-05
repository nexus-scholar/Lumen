package com.lumen.search.api

import com.lumen.search.data.engine.AggregatedStatistics
import com.lumen.search.data.engine.SearchOrchestrator
import com.lumen.search.domain.models.ScholarlyDocument
import com.lumen.search.domain.models.SearchFilters
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.models.SearchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * Main public API for the Search Module.
 * Entry point for the rest of the application and AI agents.
 */
class SearchClient(
    private val orchestrator: SearchOrchestrator
) {
    /**
     * Executes a discovery search across all providers.
     * Returns a Flow of deduplicated, merged documents.
     *
     * @param query The search query
     * @param filters Optional filters (year range, document type, etc.)
     * @param maxResults Maximum results per provider (default 50)
     * @return Flow of ScholarlyDocuments
     */
    fun search(
        query: String,
        filters: SearchFilters = SearchFilters(),
        maxResults: Int = 50
    ): Flow<ScholarlyDocument> {
        val intent = SearchIntent(
            query = query,
            filters = filters,
            mode = SearchMode.DISCOVERY,
            maxResults = maxResults
        )
        return orchestrator.executeSearch(intent)
    }

    /**
     * Executes a search and collects all results.
     * Convenience method for non-streaming use cases.
     */
    suspend fun searchAndCollect(
        query: String,
        filters: SearchFilters = SearchFilters(),
        maxResults: Int = 50
    ): List<ScholarlyDocument> {
        return search(query, filters, maxResults).toList()
    }

    /**
     * Searches with a pre-built SearchIntent.
     * Use this when you need full control over search parameters.
     */
    fun searchWithIntent(intent: SearchIntent): Flow<ScholarlyDocument> {
        return orchestrator.executeSearch(intent)
    }

    /**
     * Enriches a document with full details from its source provider.
     * Fetches abstract, references, concepts, and other Phase 2 data.
     *
     * @param document The document to enrich
     * @return Fully hydrated document, or null if enrichment fails
     */
    suspend fun enrich(document: ScholarlyDocument): ScholarlyDocument? {
        if (document.isFullyHydrated) return document
        return orchestrator.enrich(document)
    }

    /**
     * Enriches multiple documents in batch.
     * Documents already hydrated are returned as-is.
     */
    suspend fun enrichBatch(documents: List<ScholarlyDocument>): List<ScholarlyDocument> {
        return documents.map { doc ->
            if (doc.isFullyHydrated) doc else enrich(doc) ?: doc
        }
    }

    /**
     * Gets aggregated statistics for a query across all providers.
     * Useful for understanding the scope of results before fetching.
     */
    suspend fun getStats(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): AggregatedStatistics {
        val intent = SearchIntent(
            query = query,
            filters = filters,
            mode = SearchMode.DISCOVERY
        )
        return orchestrator.getAggregatedStats(intent)
    }
}

