package com.lumen.core.domain.stages

import com.lumen.core.domain.model.ConceptBlock
import com.lumen.core.domain.model.ConceptExpansion
import com.lumen.core.domain.model.ProblemFraming
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.services.llm.LlmService
import kotlinx.datetime.Clock

/**
 * Stage 3: Concept Expansion
 * Expands PICO terms into searchable synonyms and related concepts
 */
class ConceptExpansionStage(
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<ProblemFraming, ConceptExpansion> {

    override val stageName: String = "Stage 3: Concept Expansion"

    override suspend fun execute(input: ProblemFraming): StageResult<ConceptExpansion> {
        // Check if PICO is approved
        if (!input.approved) {
            return StageResult.Failure(
                PipelineError.PreconditionFailed("PICO must be approved before concept expansion")
            )
        }

        return try {
            // Expand each PICO component
            val populationBlock = expandConcept(
                input.population,
                "population",
                "the group being studied in a systematic review"
            )

            val interventionBlock = expandConcept(
                input.intervention,
                "intervention",
                "the treatment, method, or exposure being investigated"
            )

            val outcomeBlock = expandConcept(
                input.outcome,
                "outcome",
                "the measured result or effect being assessed"
            )

            val comparisonBlock = input.comparison?.let {
                expandConcept(
                    it,
                    "comparison",
                    "the alternative treatment or control condition"
                )
            }

            // Create expansion artifact
            val expansion = ConceptExpansion(
                populationBlock = populationBlock,
                interventionBlock = interventionBlock,
                outcomeBlock = outcomeBlock,
                comparisonBlock = comparisonBlock,
                approved = false,
                expandedAt = Clock.System.now()
            )

            // Save artifact
            artifactStore.save(
                projectId = "", // Will be set by pipeline orchestrator
                artifact = expansion,
                serializer = ConceptExpansion.serializer(),
                filename = "ConceptExpansion.json"
            )

            // Return for approval
            StageResult.RequiresApproval(
                data = expansion,
                reason = "Review expanded terms and synonyms",
                suggestions = listOf(
                    "Verify all synonyms are relevant",
                    "Add any missing important terms",
                    "Remove overly broad terms that might reduce precision"
                )
            )

        } catch (e: Exception) {
            StageResult.Failure(
                PipelineError.LlmCallFailed(
                    "Concept expansion failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Expand a single concept term into synonyms and related terms
     */
    private suspend fun expandConcept(
        term: String,
        conceptType: String,
        description: String
    ): ConceptBlock {
        // Check if LLM is available
        if (!llmService.isAvailable()) {
            // Fallback to minimal expansion
            return ConceptBlock(
                coreTerm = term,
                synonyms = emptyList(),
                relatedTerms = emptyList(),
                meshTerms = emptyList(),
                exclusionTerms = emptyList()
            )
        }

        // Generate synonyms using LLM
        val synonyms = try {
            llmService.generateSynonyms(
                term = term,
                context = "$conceptType: $description"
            )
        } catch (e: Exception) {
            emptyList()
        }

        // TODO: MeSH term lookup (stub for MVP)
        val meshTerms = emptyList<String>()

        return ConceptBlock(
            coreTerm = term,
            synonyms = synonyms,
            relatedTerms = emptyList(), // Could be enhanced with semantic similarity
            meshTerms = meshTerms,
            exclusionTerms = emptyList() // Could be populated by LLM or manually
        )
    }
}

