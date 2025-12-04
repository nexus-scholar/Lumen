package com.lumen.core.domain.stages

import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.domain.validation.ResearchQuestionsValidator
import com.lumen.core.services.llm.LlmService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock

private val logger = KotlinLogging.logger {}

/**
 * Stage 2: Research Questions Generation
 * Generates primary and secondary research questions from approved PICO framework
 */
class ResearchQuestionsStage(
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<ProblemFraming, ResearchQuestions> {

    override val stageName: String = "Stage 2: Research Questions Generation"

    companion object {
        private val RESEARCH_QUESTIONS_PROMPT = """
You are a systematic review methodology expert. Generate focused research questions from the PICO framework.

PICO Framework:
- **Population**: {{POPULATION}}
- **Intervention**: {{INTERVENTION}}
- **Comparison**: {{COMPARISON}}
- **Outcome**: {{OUTCOME}}
- **Study Designs**: {{STUDY_DESIGNS}}
- **Context**: {{CONTEXT}}

Generate:
1. **One primary research question** - The main question this systematic review will answer
   - Must be specific, measurable, and directly answerable through meta-analysis
   - Should clearly incorporate all PICO elements
   - Should be appropriate for the study design

2. **2-4 secondary research questions** - Supporting questions that explore:
   - Subgroup analyses (e.g., by age, severity, duration)
   - Moderating factors
   - Mechanisms or pathways
   - Comparative effectiveness across interventions
   - Different outcome measures

Guidelines:
- Use clear, answerable language following PICO structure
- Each question should end with "?"
- Be specific about population, intervention, and outcome
- Secondary questions should complement, not duplicate, the primary
- Include rationale explaining why each question matters

Return ONLY valid JSON matching this exact schema:
{
  "primary": {
    "text": "Clear, specific primary research question?",
    "rationale": "Why this is the most important question to answer"
  },
  "secondary": [
    {
      "text": "First secondary question exploring a specific aspect?",
      "rationale": "Why this secondary question adds value"
    },
    {
      "text": "Second secondary question?",
      "rationale": "Its importance"
    }
  ]
}

IMPORTANT:
- Primary question must incorporate population, intervention, and outcome explicitly
- Generate 2-4 secondary questions (not fewer, not more)
- Each rationale should be 1-2 sentences explaining the question's value
- Questions should be suitable for {{REVIEW_TYPE}} systematic review
        """.trimIndent()
    }

    override suspend fun execute(input: ProblemFraming): StageResult<ResearchQuestions> {
        logger.info { "Executing $stageName for problem framing" }

        // Precondition: PICO must be approved
        if (!input.approved) {
            logger.warn { "PICO not approved, cannot generate research questions" }
            return StageResult.Failure(
                PipelineError.PreconditionFailed(
                    "PICO framework must be approved before generating research questions. " +
                    "Please review and approve the PICO extraction first."
                )
            )
        }

        // Check if LLM is available
        if (!llmService.isAvailable()) {
            logger.warn { "LLM service not available, requiring manual input" }
            return StageResult.RequiresApproval(
                data = createEmptyQuestions(input),
                reason = "LLM service unavailable. Please manually enter research questions.",
                suggestions = listOf(
                    "Check API key configuration (OpenAI or OpenRouter)",
                    "Verify internet connection",
                    "Enter research questions manually using the form"
                )
            )
        }

        return try {
            // Generate questions using LLM
            val questions = generateQuestions(input)

            // Validate generated questions
            val validation = ResearchQuestionsValidator.validate(questions)

            if (!validation.isValid) {
                logger.warn { "Generated questions failed validation: ${validation.errors}" }

                // If validation fails critically, return failure
                return StageResult.Failure(
                    PipelineError.ValidationFailed(
                        "Generated research questions failed quality validation",
                        validation.errors
                    )
                )
            }

            // Note: Artifact storage will be handled by the UI layer since we don't have project ID
            // TODO: Refactor to include projectId in ProblemFraming input

            logger.info { "Research questions generated successfully with ${validation.warnings.size} warnings" }

            // Return for human approval (even if valid)
            StageResult.RequiresApproval(
                data = questions,
                reason = "Please review and approve the generated research questions",
                suggestions = validation.warnings
            )

        } catch (e: Exception) {
            logger.error(e) { "Research questions generation failed" }

            // Note: Error storage skipped - no project ID available
            // TODO: Refactor to include projectId in ProblemFraming input

            StageResult.Failure(
                PipelineError.LlmCallFailed(
                    "Research questions generation failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Generates research questions using LLM
     */
    private suspend fun generateQuestions(pico: ProblemFraming): ResearchQuestions {
        logger.debug { "Generating research questions from PICO" }

        // Build prompt
        val prompt = RESEARCH_QUESTIONS_PROMPT
            .replace("{{POPULATION}}", pico.population)
            .replace("{{INTERVENTION}}", pico.intervention)
            .replace("{{COMPARISON}}", pico.comparison ?: "None specified")
            .replace("{{OUTCOME}}", pico.outcome)
            .replace("{{STUDY_DESIGNS}}", pico.studyDesigns.joinToString(", "))
            .replace("{{CONTEXT}}", pico.context ?: "General")
            .replace("{{REVIEW_TYPE}}", "intervention") // TODO: Get from project

        // Call LLM with structured output
        val response = llmService.generateStructured<QuestionsResponse>(
            prompt = prompt,
            schema = QuestionsResponse.serializer(),
            temperature = 0.3 // Low temp for focused, consistent questions
        )

        // Convert to domain model
        val picoMapping = PicoMapping(
            population = pico.population,
            intervention = pico.intervention,
            comparison = pico.comparison,
            outcome = pico.outcome
        )

        return ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = response.primary.text.trim(),
                type = QuestionType.PRIMARY,
                rationale = response.primary.rationale.trim(),
                picoMapping = picoMapping
            ),
            secondary = response.secondary.mapIndexed { index, dto ->
                ResearchQuestion(
                    id = "secondary_${index + 1}",
                    text = dto.text.trim(),
                    type = QuestionType.SECONDARY,
                    rationale = dto.rationale.trim(),
                    picoMapping = picoMapping
                )
            },
            approved = false,
            llmModel = llmService.modelName,
            prompt = prompt,
            rawOutput = kotlinx.serialization.json.Json.encodeToString(
                QuestionsResponse.serializer(),
                response
            ),
            generatedAt = Clock.System.now()
        )
    }

    /**
     * Creates empty questions structure for manual entry
     */
    private fun createEmptyQuestions(pico: ProblemFraming): ResearchQuestions {
        val picoMapping = PicoMapping(
            population = pico.population,
            intervention = pico.intervention,
            comparison = pico.comparison,
            outcome = pico.outcome
        )

        return ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "",
                type = QuestionType.PRIMARY,
                rationale = null,
                picoMapping = picoMapping
            ),
            secondary = emptyList(),
            approved = false,
            generatedAt = Clock.System.now()
        )
    }

    /**
     * Extract project ID from PICO (temporary until ProblemFraming includes it)
     * For now, we'll use a placeholder that the UI will override
     */
    private fun getProjectId(@Suppress("UNUSED_PARAMETER") pico: ProblemFraming): String {
        // TODO: ProblemFraming should include projectId
        // The UI passes the project ID directly when calling the stage
        // This is a known workaround - see STAGE-02-RESEARCH-QUESTIONS-REPORT.md
        return "temp_project"
    }
}

