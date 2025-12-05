package com.lumen.search.domain.models

import kotlinx.serialization.Serializable

/**
 * Search mode determining retrieval depth
 */
@Serializable
enum class SearchMode {
    /** Fast scanning - Title, Year, Authors, DOI, Venue only */
    DISCOVERY,
    /** Deep fetch - Abstracts, References, Concepts, full metadata */
    ENRICHMENT
}

