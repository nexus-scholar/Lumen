package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Concept expansion for search query building
 */
@Serializable
data class ConceptExpansion(
    val populationBlock: ConceptBlock,
    val interventionBlock: ConceptBlock,
    val outcomeBlock: ConceptBlock,
    val comparisonBlock: ConceptBlock? = null,
    val approved: Boolean = false,
    val expandedAt: Instant? = null
)

@Serializable
data class ConceptBlock(
    val coreTerm: String,
    val synonyms: List<String> = emptyList(),
    val relatedTerms: List<String> = emptyList(),
    val meshTerms: List<String> = emptyList(),
    val exclusionTerms: List<String> = emptyList()
)

