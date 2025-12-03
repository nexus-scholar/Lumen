package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Database query plan with anti-hallucination validation
 */
@Serializable
data class DatabaseQueryPlan(
    val queries: Map<String, DatabaseQuery>,
    val generationMethod: GenerationMethod,
    val validated: Boolean = false,
    val approved: Boolean = false,
    val generatedAt: Instant? = null
)

@Serializable
data class DatabaseQuery(
    val database: String,
    val queryText: String,
    val filters: Map<String, String> = emptyMap(),
    val translationNotes: String? = null,
    val validationStatus: ValidationStatus,
    val validationMessages: List<String> = emptyList()
)

@Serializable
enum class GenerationMethod {
    LLM_GENERATED,
    TEMPLATE_BASED,
    HYBRID,
    MANUAL
}

@Serializable
enum class ValidationStatus {
    VALID,
    INVALID,
    WARNING,
    NOT_VALIDATED
}

/**
 * Query validation result
 */
data class QueryValidationResult(
    val status: ValidationStatus,
    val messages: List<String> = emptyList()
) {
    companion object {
        fun valid() = QueryValidationResult(ValidationStatus.VALID)
        fun invalid(message: String) = QueryValidationResult(ValidationStatus.INVALID, listOf(message))
        fun warning(message: String) = QueryValidationResult(ValidationStatus.WARNING, listOf(message))
    }
}

