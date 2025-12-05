package com.lumen.search.adapter

import com.lumen.core.data.providers.SearchProvider as LegacySearchProvider
import com.lumen.core.data.providers.SearchProviderResult as LegacySearchProviderResult
import com.lumen.core.domain.model.Document
import com.lumen.search.api.SearchClient
import com.lumen.search.domain.models.ScholarlyDocument
import com.lumen.search.domain.models.SearchFilters
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

/**
 * Adapter that bridges the new Search Module with existing legacy stages.
 *
 * Implements the old [LegacySearchProvider] interface while delegating
 * to the new [SearchClient] internally.
 *
 * This allows gradual migration of existing stages (SearchExecutionStage,
 * TestAndRefineStage) without breaking changes.
 */
@Deprecated(
    message = "Use SearchClient directly for new code",
    replaceWith = ReplaceWith("SearchClient", "com.lumen.search.api.SearchClient")
)
class LegacySearchAdapter(
    private val searchClient: SearchClient,
    override val providerName: String = "lumen-search"
) : LegacySearchProvider {

    override suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
        filters: Map<String, String>
    ): LegacySearchProviderResult {
        val startTime = Clock.System.now()

        // Convert legacy filters to new SearchFilters
        val searchFilters = convertFilters(filters)

        try {
            // Use the new search client
            val documents = searchClient.search(
                query = query,
                filters = searchFilters,
                maxResults = limit
            ).toList()

            // Convert to legacy Document format
            val legacyDocuments = documents.map { it.toLegacyDocument() }

            val executionTime = Clock.System.now() - startTime

            return LegacySearchProviderResult(
                documents = legacyDocuments,
                totalCount = legacyDocuments.size, // Approximate; new module streams
                hasMore = legacyDocuments.size >= limit,
                executionTimeMs = executionTime.inWholeMilliseconds
            )
        } catch (e: Exception) {
            val executionTime = Clock.System.now() - startTime
            return LegacySearchProviderResult(
                documents = emptyList(),
                totalCount = 0,
                hasMore = false,
                executionTimeMs = executionTime.inWholeMilliseconds
            )
        }
    }

    override suspend fun getDocument(id: String): Document? {
        // The new module doesn't have a direct getDocument, use enrich instead
        return null
    }

    override suspend fun estimateCount(
        query: String,
        filters: Map<String, String>
    ): Int {
        val searchFilters = convertFilters(filters)
        val stats = searchClient.getStats(query, searchFilters)
        return stats.totalEstimatedCount
    }

    override suspend fun isAvailable(): Boolean {
        return true // Assume available; individual providers handle their own availability
    }

    private fun convertFilters(filters: Map<String, String>): SearchFilters {
        return SearchFilters(
            yearStart = filters["from_year"]?.toIntOrNull(),
            yearEnd = filters["to_year"]?.toIntOrNull(),
            hasPdf = filters["has_pdf"]?.toBoolean() ?: false,
            openAccessOnly = filters["open_access"]?.toBoolean() ?: false
        )
    }

    /**
     * Converts a ScholarlyDocument to the legacy Document format.
     */
    private fun ScholarlyDocument.toLegacyDocument(): Document {
        return Document(
            id = lumenId,
            projectId = "", // Will be set by the stage
            title = title,
            abstract = abstract,
            authors = authors.map { it.name },
            year = publicationYear,
            doi = doi,
            url = pdfUrl,
            venue = venue,
            provider = sourceProvider,
            providerId = lumenId,
            citationCount = citationCount,
            metadata = buildMap {
                if (tldr != null) put("tldr", tldr)
                if (concepts.isNotEmpty()) put("concepts", concepts.joinToString(", ") { it.name })
            },
            retrievedAt = Clock.System.now()
        )
    }
}

/**
 * Extension function to convert legacy Document to ScholarlyDocument
 */
fun Document.toScholarlyDocument(): ScholarlyDocument {
    return ScholarlyDocument(
        lumenId = providerId,
        doi = doi,
        sourceProvider = provider,
        title = title,
        authors = authors.map { com.lumen.search.domain.models.Author(name = it) },
        publicationYear = year,
        venue = venue,
        citationCount = citationCount ?: 0,
        pdfUrl = url,
        abstract = abstract,
        isFullyHydrated = abstract != null
    )
}

