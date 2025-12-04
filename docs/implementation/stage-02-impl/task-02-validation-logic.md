# Task 02: Implement Validation Logic

**Status:** ⬜ Not Started  
**Priority:** Critical  
**Estimated Effort:** 1.5 hours  
**Dependencies:** Task 01 (Data Models)  
**Assignee:** _____

---

## Objective

Implement comprehensive validation logic for research questions to ensure quality, completeness, and PRISMA compliance before proceeding to concept expansion.

---

## Files to Create/Modify

### 1. ResearchQuestionsValidator.kt (New)
**Path:** `src/commonMain/kotlin/com/lumen/core/domain/validation/ResearchQuestionsValidator.kt`

### 2. Update ValidationResult.kt (Modify)
**Path:** `src/commonMain/kotlin/com/lumen/core/domain/model/ProblemFraming.kt`
(ValidationResult already exists here, may need to move to separate file)

---

## Implementation

### Create ResearchQuestionsValidator.kt

```kotlin
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
    private const val MIN_SECONDARY_QUESTIONS = 0
    private const val MAX_SECONDARY_QUESTIONS = 5
    private const val MIN_RATIONALE_LENGTH = 10
    
    private val PLACEHOLDER_WORDS = listOf(
        "TODO", "TBD", "N/A", "Unknown", "None", "null", "example"
    )
    
    private val REQUIRED_QUESTION_WORDS = listOf(
        "what", "how", "does", "is", "are", "which", "why"
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
                "Consider focusing on ${MAX_SECONDARY_QUESTIONS} or fewer for clarity."
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
```

---

## Implementation Checklist

### Pre-Implementation
- [ ] Review existing `ValidationResult` class in `ProblemFraming.kt`
- [ ] Review PicoExtractionStage validation for consistency
- [ ] Understand validation requirements from documentation

### Implementation Steps
- [ ] Create `validation` package if not exists
- [ ] Create `ResearchQuestionsValidator.kt` file
- [ ] Implement core `validate()` function
- [ ] Implement `validateQuestion()` helper
- [ ] Implement `containsPicoElements()` check
- [ ] Implement `validateUniqueness()` check
- [ ] Implement `validatePicoConsistency()` check
- [ ] Add helper functions for similarity detection
- [ ] Add constants for validation thresholds

### Post-Implementation
- [ ] Run `./gradlew build` to verify compilation
- [ ] Write unit tests for all validation functions
- [ ] Test with edge cases (empty, very long, etc.)
- [ ] Verify error messages are clear and actionable

---

## Acceptance Criteria

### Functional Requirements
✅ **AC1:** Validates primary question completeness and quality  
✅ **AC2:** Validates secondary questions (0-5 allowed)  
✅ **AC3:** Checks for duplicate or similar questions  
✅ **AC4:** Validates PICO element presence in question text  
✅ **AC5:** Checks question format (ends with ?, has question words)  
✅ **AC6:** Validates rationale presence and quality  
✅ **AC7:** Returns clear error messages for failures  
✅ **AC8:** Returns warnings for non-critical issues  
✅ **AC9:** Detects placeholder text  
✅ **AC10:** Checks PICO consistency across questions  

### Non-Functional Requirements
✅ **AC11:** Validation completes in <100ms  
✅ **AC12:** No external dependencies  
✅ **AC13:** Pure functions (no side effects)  
✅ **AC14:** Thread-safe (object singleton)  

---

## Unit Tests

### Create ResearchQuestionsValidatorTest.kt
**Path:** `src/jvmTest/kotlin/com/lumen/core/domain/validation/ResearchQuestionsValidatorTest.kt`

```kotlin
package com.lumen.core.domain.validation

import com.lumen.core.domain.model.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResearchQuestionsValidatorTest {
    
    private val validPicoMapping = PicoMapping(
        population = "patients with diabetes",
        intervention = "metformin",
        comparison = "placebo",
        outcome = "blood glucose levels"
    )
    
    @Test
    fun `validates complete valid research questions`() {
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "What is the effect of metformin on blood glucose levels in patients with diabetes?",
                type = QuestionType.PRIMARY,
                rationale = "This is the main research question to determine efficacy",
                picoMapping = validPicoMapping
            ),
            secondary = listOf(
                ResearchQuestion(
                    id = "secondary_1",
                    text = "How does the effect vary by patient age?",
                    type = QuestionType.SECONDARY,
                    rationale = "Explores age as a moderator",
                    picoMapping = validPicoMapping
                )
            ),
            approved = false,
            generatedAt = Clock.System.now()
        )
        
        val result = ResearchQuestionsValidator.validate(questions)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `detects question too short`() {
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "Too short?",
                type = QuestionType.PRIMARY,
                rationale = "Test rationale",
                picoMapping = validPicoMapping
            ),
            secondary = emptyList(),
            approved = false
        )
        
        val result = ResearchQuestionsValidator.validate(questions)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("too short") })
    }
    
    @Test
    fun `detects missing question mark`() {
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "What is the effect of metformin on blood glucose in diabetic patients",
                type = QuestionType.PRIMARY,
                rationale = "Test rationale",
                picoMapping = validPicoMapping
            ),
            secondary = emptyList(),
            approved = false
        )
        
        val result = ResearchQuestionsValidator.validate(questions)
        
        assertTrue(result.warnings.any { it.contains("question mark") })
    }
    
    @Test
    fun `detects placeholder text`() {
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "What is the TODO of metformin on blood glucose in diabetic patients?",
                type = QuestionType.PRIMARY,
                rationale = "Test rationale",
                picoMapping = validPicoMapping
            ),
            secondary = emptyList(),
            approved = false
        )
        
        val result = ResearchQuestionsValidator.validate(questions)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("placeholder") })
    }
    
    @Test
    fun `warns on too many secondary questions`() {
        val secondary = (1..6).map { i ->
            ResearchQuestion(
                id = "secondary_$i",
                text = "What is the effect of metformin in subgroup $i of diabetic patients?",
                type = QuestionType.SECONDARY,
                rationale = "Subgroup analysis $i",
                picoMapping = validPicoMapping
            )
        }
        
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "What is the effect of metformin on blood glucose in diabetic patients?",
                type = QuestionType.PRIMARY,
                rationale = "Main question",
                picoMapping = validPicoMapping
            ),
            secondary = secondary,
            approved = false
        )
        
        val result = ResearchQuestionsValidator.validate(questions)
        
        assertTrue(result.warnings.any { it.contains("Too many") })
    }
    
    @Test
    fun `detects duplicate questions`() {
        val duplicateText = "What is the effect of metformin on blood glucose in diabetic patients?"
        
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = duplicateText,
                type = QuestionType.PRIMARY,
                rationale = "Main question",
                picoMapping = validPicoMapping
            ),
            secondary = listOf(
                ResearchQuestion(
                    id = "secondary_1",
                    text = duplicateText,
                    type = QuestionType.SECONDARY,
                    rationale = "Duplicate",
                    picoMapping = validPicoMapping
                )
            ),
            approved = false
        )
        
        val result = ResearchQuestionsValidator.validate(questions)
        
        assertTrue(result.warnings.any { it.contains("Duplicate") })
    }
}
```

---

## Verification Steps

### Step 1: Run Unit Tests
```bash
./gradlew test --tests ResearchQuestionsValidatorTest
```
**Expected:** All tests pass

### Step 2: Manual Validation Test
Create test in desktop app or CLI:
```kotlin
val testQuestions = ResearchQuestions(...)
val result = ResearchQuestionsValidator.validate(testQuestions)
println("Valid: ${result.isValid}")
println("Errors: ${result.errors}")
println("Warnings: ${result.warnings}")
```

### Step 3: Edge Case Testing
- [ ] Empty question text
- [ ] Very long question (>500 chars)
- [ ] Question without question word
- [ ] All secondary questions empty
- [ ] 10 secondary questions
- [ ] Missing rationale
- [ ] Identical questions

---

## Integration Points

### Used By
- Task 04: Pipeline Stage (validates before approval)
- Task 08: Approval Dialog (shows validation errors in UI)

### Uses
- Task 01: Data Models (validates ResearchQuestions)
- `ValidationResult` from existing code

---

## Quality Metrics

- [ ] Test coverage >90%
- [ ] All validation rules tested
- [ ] Performance <100ms for typical input
- [ ] No false positives in tests
- [ ] Clear, actionable error messages

---

## Definition of Done

- [ ] All validation functions implemented
- [ ] Unit tests written with >90% coverage
- [ ] All tests passing
- [ ] Edge cases handled
- [ ] Error messages clear and helpful
- [ ] Performance acceptable
- [ ] Code reviewed
- [ ] Documentation complete

---

## Time Tracking

| Activity | Estimated | Actual | Notes |
|----------|-----------|--------|-------|
| Planning | 15 min | | |
| Implementation | 45 min | | |
| Unit Tests | 30 min | | |
| Testing & Fixes | 20 min | | |
| **Total** | **110 min** | | |

---

**Next Task:** ➡️ Task 03: Add Database Schema

---

**Task Created:** December 4, 2025  
**Last Updated:** December 4, 2025  
**Version:** 1.0

