package com.lumen.search.domain.models

import kotlinx.serialization.Serializable

/**
 * Optional filters for search refinement
 */
@Serializable
data class SearchFilters(
    /** Filter by publication year range (start to end inclusive) */
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
    /** Only include results with PDF available */
    val hasPdf: Boolean = false,
    /** Filter by document type */
    val documentTypes: List<DocumentType> = emptyList(),
    /** Filter by venue/journal names */
    val venues: List<String> = emptyList(),
    /** Filter by concepts/topics */
    val concepts: List<String> = emptyList(),
    /** Only open access documents */
    val openAccessOnly: Boolean = false
) {
    /** Convenience property for year range */
    val yearRange: IntRange?
        get() = if (yearStart != null && yearEnd != null) yearStart..yearEnd else null
}

