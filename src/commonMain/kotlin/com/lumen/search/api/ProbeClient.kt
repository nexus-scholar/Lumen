package com.lumen.search.api

import com.lumen.search.data.engine.SearchOrchestrator
import com.lumen.search.domain.models.SearchFilters
import com.lumen.search.domain.models.SearchIntent
import com.lumen.search.domain.models.SearchMode
import com.lumen.search.domain.ports.QueryFeasibility
import com.lumen.search.domain.ports.ResearchProbe
import com.lumen.search.domain.ports.SignalMetrics
import com.lumen.search.domain.ports.assessFeasibility
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Specialized API for the "Idea Generation" module.
 * Implements ResearchProbe interface for AI-driven feasibility validation.
 *
 * Allows AI agents to "test the waters" (get counts/trends) without
 * incurring the cost of downloading full document lists.
 */
class ProbeClient(
    private val orchestrator: SearchOrchestrator
) : ResearchProbe {

    /**
     * Gets signal strength metrics for a research topic.
     * Used to validate PICO questions and research feasibility.
     */
    override suspend fun getSignalStrength(query: String): SignalMetrics {
        logger.info { "Probing signal strength for: $query" }

        val intent = SearchIntent(
            query = query,
            mode = SearchMode.DISCOVERY,
            maxResults = 1 // We only need metadata
        )

        val stats = orchestrator.getAggregatedStats(intent)
        val trendLine = analyzeTrend(stats.providerStats.values.flatMap { it.countByYear.entries })

        val feasibility = assessFeasibility(stats.totalEstimatedCount)

        val suggestions = generateRefinementSuggestions(
            query = query,
            totalCount = stats.totalEstimatedCount,
            feasibility = feasibility,
            concepts = stats.allConcepts
        )

        return SignalMetrics(
            totalCount = stats.totalEstimatedCount,
            isRising = trendLine.isRising,
            growthRate = trendLine.growthRate,
            topConcepts = stats.allConcepts.take(10),
            refinementSuggestions = suggestions,
            feasibility = feasibility
        )
    }

    /**
     * Gets publication trend over time for a query.
     */
    override suspend fun getTrendLine(
        query: String,
        yearStart: Int?,
        yearEnd: Int?
    ): Map<Int, Int> {
        val filters = if (yearStart != null && yearEnd != null) {
            SearchFilters(yearStart = yearStart, yearEnd = yearEnd)
        } else {
            SearchFilters()
        }

        val intent = SearchIntent(
            query = query,
            filters = filters,
            mode = SearchMode.DISCOVERY,
            maxResults = 1
        )

        val stats = orchestrator.getAggregatedStats(intent)

        // Merge year counts from all providers
        return stats.providerStats.values
            .flatMap { it.countByYear.entries }
            .groupBy { it.key }
            .mapValues { (_, entries) -> entries.maxOf { it.value } }
            .toSortedMap()
    }

    /**
     * Compares signal strength across multiple queries.
     */
    override suspend fun compareQueries(queries: List<String>): Map<String, SignalMetrics> {
        return queries.associateWith { query ->
            getSignalStrength(query)
        }
    }

    /**
     * Suggests query refinements based on initial probe results.
     */
    suspend fun suggestRefinements(query: String): List<RefinementSuggestion> {
        val metrics = getSignalStrength(query)

        return when (metrics.feasibility) {
            QueryFeasibility.TOO_BROAD -> {
                // Suggest narrowing with concepts
                metrics.topConcepts.take(5).map { concept ->
                    RefinementSuggestion(
                        originalQuery = query,
                        refinedQuery = "$query AND $concept",
                        reason = "Add concept filter: $concept",
                        estimatedImpact = "Narrows results"
                    )
                }
            }
            QueryFeasibility.TOO_NARROW -> {
                // Suggest broadening
                listOf(
                    RefinementSuggestion(
                        originalQuery = query,
                        refinedQuery = query.split(" ").dropLast(1).joinToString(" "),
                        reason = "Remove restrictive terms",
                        estimatedImpact = "Broadens results"
                    )
                )
            }
            else -> emptyList()
        }
    }

    private fun analyzeTrend(yearCounts: List<Map.Entry<Int, Int>>): TrendAnalysis {
        if (yearCounts.isEmpty()) {
            return TrendAnalysis(isRising = false, growthRate = 0.0)
        }

        val sorted = yearCounts.sortedBy { it.key }
        if (sorted.size < 2) {
            return TrendAnalysis(isRising = false, growthRate = 0.0)
        }

        // Compare last 2 years vs previous 2 years
        val recentYears = sorted.takeLast(2)
        val previousYears = sorted.dropLast(2).takeLast(2)

        val recentAvg = recentYears.map { it.value }.average()
        val previousAvg = if (previousYears.isNotEmpty()) {
            previousYears.map { it.value }.average()
        } else {
            recentAvg
        }

        val growthRate = if (previousAvg > 0) {
            (recentAvg - previousAvg) / previousAvg
        } else {
            0.0
        }

        return TrendAnalysis(
            isRising = growthRate > 0.05, // 5% growth threshold
            growthRate = growthRate
        )
    }

    private fun generateRefinementSuggestions(
        query: String,
        totalCount: Int,
        feasibility: QueryFeasibility,
        concepts: List<String>
    ): List<String> {
        return when (feasibility) {
            QueryFeasibility.TOO_BROAD -> {
                val suggestions = mutableListOf<String>()
                suggestions.add("Consider adding year filters to narrow scope")
                concepts.take(3).forEach { concept ->
                    suggestions.add("Try adding: '$concept' to narrow results")
                }
                suggestions
            }
            QueryFeasibility.TOO_NARROW -> {
                listOf(
                    "Consider using broader terms",
                    "Remove specific filters",
                    "Try related concepts from other fields"
                )
            }
            QueryFeasibility.BORDERLINE -> {
                listOf("Results are on the high end - consider if all are relevant")
            }
            QueryFeasibility.FEASIBLE -> emptyList()
        }
    }

    private data class TrendAnalysis(
        val isRising: Boolean,
        val growthRate: Double
    )
}

