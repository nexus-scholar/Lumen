package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Container for all research questions generated from PICO framework
 */
@Serializable
data class ResearchQuestions(
    val primary: ResearchQuestion,
    val secondary: List<ResearchQuestion>,
    val approved: Boolean = false,
    val llmModel: String? = null,
    val prompt: String? = null,
    val rawOutput: String? = null,
    val generatedAt: Instant? = null,
    val approvedAt: Instant? = null,
    val approvedBy: String? = null
)

/**
 * A single research question with its metadata
 */
@Serializable
data class ResearchQuestion(
    val id: String,
    val text: String,
    val type: QuestionType,
    val rationale: String? = null,
    val picoMapping: PicoMapping
)

/**
 * Type of research question
 */
@Serializable
enum class QuestionType {
    PRIMARY,
    SECONDARY,
    EXPLORATORY
}

/**
 * PICO element mapping for a research question
 */
@Serializable
data class PicoMapping(
    val population: String,
    val intervention: String,
    val comparison: String? = null,
    val outcome: String
)

/**
 * Internal DTO for LLM response parsing
 */
@Serializable
internal data class QuestionsResponse(
    val primary: QuestionDto,
    val secondary: List<QuestionDto>
)

/**
 * Internal DTO for individual question in LLM response
 */
@Serializable
internal data class QuestionDto(
    val text: String,
    val rationale: String
)

