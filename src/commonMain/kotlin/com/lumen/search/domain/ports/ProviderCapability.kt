package com.lumen.search.domain.ports

/**
 * Capabilities that a provider can declare
 */
enum class ProviderCapability {
    /** Can search by free text query */
    TEXT_SEARCH,
    /** Can filter by year range */
    YEAR_FILTER,
    /** Can filter by document type */
    TYPE_FILTER,
    /** Can filter by venue/journal */
    VENUE_FILTER,
    /** Can filter by concepts/topics */
    CONCEPT_FILTER,
    /** Can provide abstracts */
    ABSTRACTS,
    /** Can provide references list */
    REFERENCES,
    /** Can provide citations list */
    CITATIONS,
    /** Can provide AI-generated summaries (TLDR) */
    TLDR,
    /** Can provide concept/topic tags */
    CONCEPTS,
    /** Can provide citation counts */
    CITATION_COUNTS,
    /** Can provide PDF URLs */
    PDF_URLS,
    /** Supports pagination */
    PAGINATION,
    /** Can provide publication statistics */
    STATISTICS
}

