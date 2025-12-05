package com.lumen.search.domain.valueobjects

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Value object representing a year range for filtering publications.
 */
@Serializable
data class YearRange(
    val start: Int,
    val end: Int
) {
    init {
        require(start <= end) { "Start year ($start) must be <= end year ($end)" }
        require(start >= 1900) { "Start year must be >= 1900" }
        require(end <= 2100) { "End year must be <= 2100" }
    }

    companion object {
        /**
         * Creates a range for the last N years from current year
         */
        fun lastYears(n: Int): YearRange {
            require(n > 0) { "Number of years must be positive" }
            val currentYear = Clock.System.now()
                .toLocalDateTime(TimeZone.UTC).year
            return YearRange(currentYear - n + 1, currentYear)
        }

        /**
         * Creates a single year range
         */
        fun singleYear(year: Int): YearRange = YearRange(year, year)
    }

    /**
     * Checks if a year falls within this range
     */
    operator fun contains(year: Int): Boolean = year in start..end

    /**
     * Converts to IntRange for Kotlin range operations
     */
    fun toIntRange(): IntRange = start..end

    override fun toString(): String = if (start == end) "$start" else "$start-$end"
}

