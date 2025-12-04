package com.lumen.core.domain.validation

import com.lumen.core.domain.model.ResearchQuestion
import com.lumen.core.domain.model.ResearchQuestions
import com.lumen.core.domain.model.ValidationResult

/**
 * Validates research questions for quality and completeness
 */
object ResearchQuestionsValidator {

    private const val MIN_QUESTION_LENGTH = 20
    private const val MAX_QUESTION_LENGTH = 500
    private const val MAX_SECONDARY_QUESTIONS = 5
    private const val MIN_RATIONALE_LENGTH = 10

    private val PLACEHOLDER_WORDS = listOf(
        "TODO", "TBD", "N/A", "Unknown", "None", "null", "example"
    )

    private val REQUIRED_QUESTION_WORDS = listOf(
        "what", "how", "does", "is", "are", "which", "why", "can", "do"
    )

    /**
     * Validates a complete ResearchQuestions object
     */
    fun validate(questions: ResearchQuestions): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate primary question
        validateQuestion(questions.primary, "Primary", errors, warnings)

        // Validate secondary questions
        if (questions.secondary.isEmpty()) {
            warnings.add("No secondary questions provided. Consider adding 2-3 to explore subgroups or moderators.")
        }

        if (questions.secondary.size > MAX_SECONDARY_QUESTIONS) {
            warnings.add(
                "Too many secondary questions (${questions.secondary.size}). " +
                "Consider focusing on $MAX_SECONDARY_QUESTIONS or fewer for clarity."
            )
        }

        questions.secondary.forEachIndexed { index, question ->
            validateQuestion(question, "Secondary #${index + 1}", errors, warnings)
        }

        // Check for duplicate or very similar questions
        validateUniqueness(questions, warnings)

        // Validate PICO consistency across questions
        validatePicoConsistency(questions, warnings)

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validates a single research question
     */
    private fun validateQuestion(
        question: ResearchQuestion,
        label: String,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val text = question.text.trim()

        // Length validation
        if (text.length < MIN_QUESTION_LENGTH) {
            errors.add("$label question too short (${text.length} chars). Minimum: $MIN_QUESTION_LENGTH")
        }

        if (text.length > MAX_QUESTION_LENGTH) {
            warnings.add("$label question very long (${text.length} chars). Consider simplifying.")
        }

        // Format validation
        if (!text.endsWith("?")) {
            warnings.add("$label question should end with a question mark: '$text'")
        }

        // Content validation
        val lowerText = text.lowercase()
        val hasQuestionWord = REQUIRED_QUESTION_WORDS.any { lowerText.contains(it) }
        if (!hasQuestionWord) {
            warnings.add(
                "$label question should start with a question word (what, how, does, etc.): '$text'"
            )
        }

        // Placeholder check
        PLACEHOLDER_WORDS.forEach { placeholder ->
            if (text.contains(placeholder, ignoreCase = true)) {
                errors.add("$label question contains placeholder text: '$placeholder'")
            }
        }

        // PICO element check
        if (!containsPicoElements(text, question.picoMapping)) {
            warnings.add(
                "$label question doesn't clearly reference all PICO elements. " +
                "Ensure population, intervention, and outcome are mentioned."
            )
        }

        // Rationale validation
        if (question.rationale.isNullOrBlank()) {
            warnings.add("$label question lacks rationale explaining its importance")
        } else if (question.rationale.length < MIN_RATIONALE_LENGTH) {
            warnings.add("$label question rationale too brief (${question.rationale.length} chars)")
        }

        // Answerability check
        if (isVague(text)) {
            warnings.add("$label question may be too vague or broad: '$text'")
        }
    }

    /**
     * Checks if question text contains PICO elements
     */
    private fun containsPicoElements(text: String, pico: com.lumen.core.domain.model.PicoMapping): Boolean {
        val lowerText = text.lowercase()

        // Check if key terms from PICO are present
        val populationWords = pico.population.lowercase().split(" ").filter { it.length > 3 }
        val interventionWords = pico.intervention.lowercase().split(" ").filter { it.length > 3 }
        val outcomeWords = pico.outcome.lowercase().split(" ").filter { it.length > 3 }

        val hasPopulation = populationWords.any { lowerText.contains(it) }
        val hasIntervention = interventionWords.any { lowerText.contains(it) }
        val hasOutcome = outcomeWords.any { lowerText.contains(it) }

        // Should contain at least intervention and outcome
        return hasIntervention && hasOutcome
    }

    /**
     * Checks if question is too vague
     */
    private fun isVague(text: String): Boolean {
        val vaguePatterns = listOf(
            "better", "improve", "affect", "impact", "influence", "change"
        )

        val lowerText = text.lowercase()
        val vagueWordCount = vaguePatterns.count { lowerText.contains(it) }

        // Too many vague words without specifics
        return vagueWordCount > 2 && text.length < 100
    }

    /**
     * Validates that questions are unique and not duplicates
     */
    private fun validateUniqueness(
        questions: ResearchQuestions,
        warnings: MutableList<String>
    ) {
        val allQuestions = listOf(questions.primary) + questions.secondary
        val texts = allQuestions.map { it.text.lowercase().trim() }

        // Check exact duplicates
        val duplicates = texts.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            warnings.add("Duplicate questions found: ${duplicates.keys.joinToString()}")
        }

        // Check very similar questions (simple similarity check)
        for (i in texts.indices) {
            for (j in i + 1 until texts.size) {
                if (areSimilar(texts[i], texts[j])) {
                    warnings.add(
                        "Questions ${i + 1} and ${j + 1} are very similar. " +
                        "Consider consolidating or differentiating them."
                    )
                }
            }
        }
    }

    /**
     * Simple similarity check (Jaccard similarity on words)
     */
    private fun areSimilar(text1: String, text2: String, threshold: Double = 0.7): Boolean {
        val words1 = text1.split("\\s+".toRegex()).filter { it.length > 3 }.toSet()
        val words2 = text2.split("\\s+".toRegex()).filter { it.length > 3 }.toSet()

        if (words1.isEmpty() || words2.isEmpty()) return false

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return (intersection.toDouble() / union.toDouble()) >= threshold
    }

    /**
     * Validates PICO consistency across all questions
     */
    private fun validatePicoConsistency(
        questions: ResearchQuestions,
        warnings: MutableList<String>
    ) {
        val primaryPico = questions.primary.picoMapping

        questions.secondary.forEachIndexed { index, question ->
            val secondaryPico = question.picoMapping

            // Secondary questions should generally have same PICO
            if (primaryPico.population != secondaryPico.population) {
                warnings.add(
                    "Secondary question #${index + 1} has different population than primary. " +
                    "Ensure this is intentional."
                )
            }

            if (primaryPico.intervention != secondaryPico.intervention) {
                warnings.add(
                    "Secondary question #${index + 1} has different intervention than primary. " +
                    "This may indicate exploring different interventions (acceptable for exploratory questions)."
                )
            }
        }
    }
}

