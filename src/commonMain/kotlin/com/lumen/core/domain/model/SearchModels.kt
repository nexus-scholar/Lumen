package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a research document/paper
 */
@Serializable
data class Document(
    val id: String,
    val projectId: String,
    val title: String,
    val abstract: String? = null,
    val authors: List<String> = emptyList(),
    val year: Int? = null,
    val doi: String? = null,
    val url: String? = null,
    val venue: String? = null,
    val provider: String,
    val providerId: String,
    val citationCount: Int? = null,
    val metadata: Map<String, String> = emptyMap(),
    val retrievedAt: Instant
)

/**
 * Search results from multiple databases
 */
@Serializable
data class SearchResults(
    val searchId: String,
    val projectId: String,
    val executedAt: Instant,
    val queryPlan: DatabaseQueryPlan,
    val results: Map<String, DatabaseSearchResult>,
    val totalDocuments: Int,
    val executionLog: List<ExecutionLogEntry> = emptyList()
)

@Serializable
data class DatabaseSearchResult(
    val database: String,
    val query: String,
    val totalCount: Int,
    val documents: List<Document>,
    val executionTimeMs: Long,
    val error: String? = null
)

@Serializable
data class ExecutionLogEntry(
    val timestamp: Instant,
    val database: String,
    val action: String,
    val details: String
)

/**
 * Test search result for query refinement
 */
@Serializable
data class TestSearchResult(
    val testRuns: List<TestRun>,
    val currentAnalysis: SearchAnalysis,
    val refinementSuggestions: List<RefinementSuggestion> = emptyList(),
    val approved: Boolean = false
)

@Serializable
data class TestRun(
    val runId: String,
    val iteration: Int,
    val timestamp: Instant,
    val queryPlan: DatabaseQueryPlan,
    val results: Map<String, DatabaseTestResult>,
    val analysis: SearchAnalysis
)

@Serializable
data class DatabaseTestResult(
    val database: String,
    val estimatedTotal: Int,
    val samplePapers: List<Document>,
    val yearDistribution: Map<Int, Int> = emptyMap(),
    val topVenues: List<String> = emptyList()
)

@Serializable
data class SearchAnalysis(
    val broadness: QueryBroadness,
    val totalAcrossDatabases: Int,
    val precisionEstimate: Double? = null,
    val recommendations: List<String> = emptyList()
)

@Serializable
enum class QueryBroadness {
    TOO_NARROW,   // < 100 total
    APPROPRIATE,  // 100-5000
    TOO_BROAD     // > 5000
}

@Serializable
data class RefinementSuggestion(
    val type: RefinementType,
    val description: String,
    val suggestedChange: String,
    val affectedDatabases: List<String>,
    val rationale: String
)

@Serializable
enum class RefinementType {
    ADD_EXCLUSION_TERM,
    NARROW_DATE_RANGE,
    ADD_FIELD_RESTRICTION,
    BROADEN_SYNONYMS,
    RELAX_FILTERS,
    ADJUST_OPERATORS
}

