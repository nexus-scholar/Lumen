package com.lumen.search.domain.ports

/**
 * Statistics about search results (without fetching documents)
 */
data class SearchStatistics(
    val totalCount: Int,
    val countByYear: Map<Int, Int> = emptyMap(),
    val topConcepts: List<String> = emptyList(),
    val estimatedExecutionTimeMs: Long = 0
)

