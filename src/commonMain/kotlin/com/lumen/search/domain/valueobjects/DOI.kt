package com.lumen.search.domain.valueobjects

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Value object representing a Digital Object Identifier (DOI).
 * Handles normalization and validation of DOI strings.
 */
@Serializable
@JvmInline
value class DOI private constructor(val value: String) {

    companion object {
        private val DOI_REGEX = Regex("^10\\.\\d{4,}/[^\\s]+$")
        private val DOI_URL_PREFIXES = listOf(
            "https://doi.org/",
            "http://doi.org/",
            "https://dx.doi.org/",
            "http://dx.doi.org/",
            "doi:"
        )

        /**
         * Creates a DOI from a string, normalizing URL prefixes.
         * Returns null if the string is not a valid DOI.
         */
        fun fromString(input: String?): DOI? {
            if (input.isNullOrBlank()) return null

            var normalized = input.trim()

            // Remove common URL prefixes
            for (prefix in DOI_URL_PREFIXES) {
                if (normalized.startsWith(prefix, ignoreCase = true)) {
                    normalized = normalized.removePrefix(prefix)
                    break
                }
            }

            // Validate format
            return if (DOI_REGEX.matches(normalized)) {
                DOI(normalized.lowercase())
            } else {
                null
            }
        }

        /**
         * Creates a DOI without validation (for trusted sources)
         */
        fun trusted(value: String): DOI = DOI(value.lowercase())
    }

    /**
     * Returns the full DOI URL
     */
    fun toUrl(): String = "https://doi.org/$value"

    override fun toString(): String = value
}

