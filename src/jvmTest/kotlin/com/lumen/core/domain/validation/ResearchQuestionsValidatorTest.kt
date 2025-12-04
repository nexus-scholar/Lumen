package com.lumen.core.domain.validation

import com.lumen.core.domain.model.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
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

    @Test
    fun `warns on no secondary questions`() {
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "What is the effect of metformin on blood glucose levels in patients with diabetes?",
                type = QuestionType.PRIMARY,
                rationale = "Main question",
                picoMapping = validPicoMapping
            ),
            secondary = emptyList(),
            approved = false
        )

        val result = ResearchQuestionsValidator.validate(questions)

        assertTrue(result.warnings.any { it.contains("No secondary questions") })
    }

    @Test
    fun `validates question has required elements`() {
        val questions = ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "What is the effect of metformin on blood glucose levels in diabetic patients?",
                type = QuestionType.PRIMARY,
                rationale = "This question addresses the primary outcome",
                picoMapping = validPicoMapping
            ),
            secondary = emptyList(),
            approved = false
        )

        val result = ResearchQuestionsValidator.validate(questions)

        // Should be valid - contains intervention (metformin) and outcome (blood glucose)
        assertTrue(result.isValid)
    }
}

