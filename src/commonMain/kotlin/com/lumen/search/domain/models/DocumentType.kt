package com.lumen.search.domain.models

import kotlinx.serialization.Serializable

/**
 * Document type classification
 */
@Serializable
enum class DocumentType {
    JOURNAL_ARTICLE,
    CONFERENCE_PAPER,
    PREPRINT,
    BOOK_CHAPTER,
    THESIS,
    REVIEW,
    OTHER
}

