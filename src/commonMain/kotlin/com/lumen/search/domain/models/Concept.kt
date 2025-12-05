package com.lumen.search.domain.models

import kotlinx.serialization.Serializable

/**
 * Topic/concept tag with relevance score
 */
@Serializable
data class Concept(
    val name: String,
    /** Relevance score from 0.0 to 1.0 */
    val score: Double,
    /** Provider-specific concept ID */
    val id: String? = null
)

