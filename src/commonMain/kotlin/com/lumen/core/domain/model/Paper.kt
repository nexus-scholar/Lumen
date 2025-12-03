package com.lumen.core.domain.model

/**
 * Represents a research paper in the systematic review
 */
data class Paper(
    val id: String,
    val title: String,
    val abstract: String,
    val authors: List<String>,
    val year: Int?,
    val doi: String?,
    val sources: List<PaperSource>,
    val metadata: Map<String, String> = emptyMap()
)

enum class PaperSource {
    OPENALEX,
    CROSSREF,
    ARXIV,
    SEMANTIC_SCHOLAR,
    PUBMED,
    MANUAL
}

