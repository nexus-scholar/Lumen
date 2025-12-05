package com.lumen.search.domain.models

import kotlinx.serialization.Serializable

/**
 * Author information with optional provider-specific ID
 */
@Serializable
data class Author(
    val name: String,
    /** Provider-specific author ID (e.g., OpenAlex author ID) */
    val id: String? = null,
    /** ORCID identifier if available */
    val orcid: String? = null,
    /** Institutional affiliation */
    val affiliation: String? = null
)

