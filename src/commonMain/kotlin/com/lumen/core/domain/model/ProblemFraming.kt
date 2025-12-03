package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * PICO framework for systematic reviews
 * P = Population, I = Intervention, C = Comparison, O = Outcome
 */
@Serializable
data class ProblemFraming(
    val population: String,
    val intervention: String,
    val comparison: String? = null,
    val outcome: String,
    val studyDesigns: List<String> = emptyList(),
    val timeframe: String? = null,
    val context: String? = null,
    val approved: Boolean = false,
    val llmModel: String? = null,
    val prompt: String? = null,
    val rawOutput: String? = null,
    val extractedAt: Instant? = null
)

/**
 * Validation result for PICO extraction
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

