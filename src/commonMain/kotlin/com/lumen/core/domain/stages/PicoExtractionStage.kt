package com.lumen.core.domain.stages

import com.lumen.core.domain.model.ProblemFraming
import com.lumen.core.domain.model.Project
import com.lumen.core.domain.model.ValidationResult
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.services.llm.LlmService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/**
 * Stage 1: PICO Extraction
 * Extracts PICO framework from research question using LLM
 */
class PicoExtractionStage(
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<Project, ProblemFraming> {

    override val stageName: String = "Stage 1: PICO Extraction"

    companion object {
        private val PICO_EXTRACTION_PROMPT = """
You are a research methodology expert. Extract PICO components from the following research question.

Research Question:
{{RESEARCH_IDEA}}

Extract:
- **Population**: The group being studied (e.g., "patients with type 2 diabetes", "wheat crops")
- **Intervention**: The treatment/method being investigated (e.g., "metformin", "machine learning detection")
- **Comparison**: The alternative or control (optional, e.g., "placebo", "traditional methods")
- **Outcome**: What is being measured (e.g., "blood glucose levels", "diagnostic accuracy")
- **Study Designs**: Appropriate study types (e.g., ["RCT", "cohort study"])
- **Timeframe**: If mentioned (e.g., "2015-2024")
- **Context**: Setting or constraints (e.g., "low-resource settings", "field conditions")

Return ONLY valid JSON matching this schema:
{
  "population": string,
  "intervention": string,
  "comparison": string | null,
  "outcome": string,
  "study_designs": string[],
  "timeframe": string | null,
  "context": string | null
}

Be specific and detailed. If comparison is not applicable, use null.
        """.trimIndent()
    }

    override suspend fun execute(input: Project): StageResult<ProblemFraming> {
        logger.info { "Executing $stageName for project ${input.id}" }

        // Check if LLM is available
        if (!llmService.isAvailable()) {
            logger.warn { "LLM service not available, requiring manual input" }
            return StageResult.RequiresApproval(
                data = createEmptyPico(),
                reason = "LLM service unavailable. Please manually enter PICO components.",
                suggestions = listOf(
                    "Check OpenAI API key configuration",
                    "Verify internet connection",
                    "Enter PICO components manually"
                )
            )
        }

        return try {
            // Generate prompt
            val prompt = PICO_EXTRACTION_PROMPT.replace(
                "{{RESEARCH_IDEA}}",
                input.rawIdea
            )

            // Call LLM with structured output
            val response = llmService.generateStructured<PicoResponse>(
                prompt = prompt,
                schema = PicoResponse.serializer(),
                temperature = 0.2 // Low temp for factual extraction
            )

            // Convert to domain model
            val pico = ProblemFraming(
                population = response.population,
                intervention = response.intervention,
                comparison = response.comparison,
                outcome = response.outcome,
                studyDesigns = response.study_designs,
                timeframe = response.timeframe,
                context = response.context,
                approved = false,
                llmModel = llmService.modelName,
                prompt = prompt,
                rawOutput = kotlinx.serialization.json.Json.encodeToString(
                    PicoResponse.serializer(),
                    response
                ),
                extractedAt = Clock.System.now()
            )

            // Validate
            val validation = validatePico(pico)
            if (!validation.isValid) {
                return StageResult.Failure(
                    PipelineError.ValidationFailed(
                        "PICO validation failed",
                        validation.errors
                    )
                )
            }

            // Save artifact
            artifactStore.save(
                projectId = input.id,
                artifact = pico,
                serializer = ProblemFraming.serializer(),
                filename = "ProblemFraming.json"
            )

            logger.info { "PICO extraction completed for project ${input.id}" }

            // Requires human approval before continuing
            StageResult.RequiresApproval(
                data = pico,
                reason = "Please review and approve PICO extraction",
                suggestions = validation.warnings
            )

        } catch (e: Exception) {
            logger.error(e) { "PICO extraction failed" }

            // Save error
            artifactStore.saveError(
                projectId = input.id,
                stageName = stageName,
                error = PipelineError.LlmCallFailed(e.message ?: "Unknown error", e),
                context = mapOf("rawIdea" to input.rawIdea)
            )

            StageResult.Failure(
                PipelineError.LlmCallFailed(
                    "PICO extraction failed: ${e.message}",
                    e
                )
            )
        }
    }

    private fun validatePico(pico: ProblemFraming): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check minimum length
        if (pico.population.length < 5) {
            errors.add("Population description too short (< 5 characters)")
        }
        if (pico.intervention.length < 5) {
            errors.add("Intervention description too short (< 5 characters)")
        }
        if (pico.outcome.length < 5) {
            errors.add("Outcome description too short (< 5 characters)")
        }

        // Check for placeholder text
        val placeholders = listOf("TODO", "TBD", "N/A", "Unknown", "None", "null")
        listOf(
            "population" to pico.population,
            "intervention" to pico.intervention,
            "outcome" to pico.outcome
        ).forEach { (field, value) ->
            if (placeholders.any { it.equals(value, ignoreCase = true) }) {
                errors.add("Field '$field' contains placeholder text: $value")
            }
        }

        // Warnings
        if (pico.studyDesigns.isEmpty()) {
            warnings.add("No study designs specified - may affect search precision")
        }

        if (pico.comparison == null && pico.intervention.isNotEmpty()) {
            warnings.add("No comparison specified - consider if a comparator is needed")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun createEmptyPico(): ProblemFraming {
        return ProblemFraming(
            population = "",
            intervention = "",
            comparison = null,
            outcome = "",
            studyDesigns = emptyList(),
            timeframe = null,
            context = null,
            approved = false,
            extractedAt = Clock.System.now()
        )
    }
}

/**
 * Internal response model from LLM
 */
@Serializable
private data class PicoResponse(
    val population: String,
    val intervention: String,
    val comparison: String? = null,
    val outcome: String,
    val study_designs: List<String> = emptyList(),
    val timeframe: String? = null,
    val context: String? = null
)

