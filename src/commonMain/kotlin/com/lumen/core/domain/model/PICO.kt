package com.lumen.core.domain.model

/**
 * PICO framework for systematic reviews
 * P = Population, I = Intervention, C = Comparison, O = Outcome
 */
data class PICO(
    val population: String,
    val intervention: String,
    val comparison: String?,
    val outcome: String
)

