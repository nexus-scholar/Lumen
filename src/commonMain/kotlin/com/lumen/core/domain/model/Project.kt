package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a systematic review project
 */
@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String,
    val rawIdea: String,
    val reviewType: ReviewType,
    val authors: List<Author> = emptyList(),
    val fundingSource: String? = null,
    val conflictsOfInterest: String? = null,
    val targetJournal: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: ProjectStatus,
    val currentStage: String? = null,
    val enableVersionControl: Boolean = false
)

@Serializable
enum class ProjectStatus {
    CREATED,
    PICO_EXTRACTION,
    RESEARCH_QUESTIONS,
    CONCEPT_EXPANSION,
    QUERY_GENERATION,
    TEST_REFINE,
    PROTOCOL_REGISTRATION,
    SEARCH_EXECUTION,
    CITATION_EXPANSION,
    DEDUPLICATION,
    SCREENING,
    FULLTEXT_SCREENING,
    DATA_EXTRACTION,
    RISK_OF_BIAS,
    SYNTHESIS,
    EXPORT,
    COMPLETED
}

@Serializable
enum class ReviewType {
    INTERVENTION,
    DIAGNOSTIC,
    PROGNOSTIC,
    SCOPING,
    QUALITATIVE,
    RAPID
}

@Serializable
data class Author(
    val fullName: String,
    val email: String? = null,
    val orcid: String? = null,
    val affiliation: String? = null,
    val role: AuthorRole = AuthorRole.CO_INVESTIGATOR
)

@Serializable
enum class AuthorRole {
    LEAD,
    CO_INVESTIGATOR,
    REVIEWER,
    ADVISOR
}

