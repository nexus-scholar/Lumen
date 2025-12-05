package com.lumen.search.domain.ports

/**
 * Assessment of query feasibility for systematic review
 */
enum class QueryFeasibility {
    /** < 50 papers - may be too narrow */
    TOO_NARROW,
    /** 50-500 papers - ideal range */
    FEASIBLE,
    /** 500-2000 papers - manageable but large */
    BORDERLINE,
    /** > 2000 papers - needs refinement */
    TOO_BROAD
}

/**
 * Determines feasibility based on total count
 */
fun assessFeasibility(totalCount: Int): QueryFeasibility = when {
    totalCount < 50 -> QueryFeasibility.TOO_NARROW
    totalCount <= 500 -> QueryFeasibility.FEASIBLE
    totalCount <= 2000 -> QueryFeasibility.BORDERLINE
    else -> QueryFeasibility.TOO_BROAD
}

/**
 * Metrics indicating the viability/signal of a research topic
 */
data class SignalMetrics(
    /** Total number of matching papers ("Is there enough literature?") */
    val totalCount: Int,
    /** Whether publication volume is increasing ("Is this a hot topic?") */
    val isRising: Boolean,
    /** Year-over-year growth rate (positive = growing) */
    val growthRate: Double,
    /** Top concepts/keywords to consider for refinement */
    val topConcepts: List<String>,
    /** Suggested query refinements if count is too high/low */
    val refinementSuggestions: List<String> = emptyList(),
    /** Assessment of the query viability */
    val feasibility: QueryFeasibility
)

/**
 * Specialized interface for the "Idea Generation" module.
 * Decouples exploration of research topics from document retrieval.
 *
 * Allows AI agents to "test the waters" (validate feasibility) without
 * incurring the cost of downloading document lists.
 */
interface ResearchProbe {
    /**
     * Returns "signals" about a topic, not documents.
     * Used to validate PICO questions and research feasibility.
     *
     * @param query The research topic/question to probe
     * @return Metrics indicating topic viability
     */
    suspend fun getSignalStrength(query: String): SignalMetrics

    /**
     * Returns publication trend over time for a query.
     * Useful for identifying emerging or declining research areas.
     *
     * @param query The research topic to analyze
     * @param yearStart Optional start year (default: 10 years ago)
     * @param yearEnd Optional end year (default: current year)
     * @return Map of year to publication count
     */
    suspend fun getTrendLine(
        query: String,
        yearStart: Int? = null,
        yearEnd: Int? = null
    ): Map<Int, Int>

    /**
     * Compares signal strength across multiple queries.
     * Useful for choosing between alternative PICO formulations.
     *
     * @param queries List of queries to compare
     * @return Map of query to its signal metrics
     */
    suspend fun compareQueries(queries: List<String>): Map<String, SignalMetrics>
}

