package com.lumen.core.domain.stages

import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import kotlinx.datetime.Clock

/**
 * Stage 4: Query Generation
 * Generates database-specific Boolean queries with anti-hallucination validation
 */
class QueryGenerationStage(
    private val artifactStore: ArtifactStore,
    private val queryValidator: QueryValidator = QueryValidator()
) : PipelineStage<ConceptExpansion, DatabaseQueryPlan> {

    override val stageName: String = "Stage 4: Query Generation"

    override suspend fun execute(input: ConceptExpansion): StageResult<DatabaseQueryPlan> {
        // Check if concept expansion is approved
        if (!input.approved) {
            return StageResult.Failure(
                PipelineError.PreconditionFailed("Concept expansion must be approved before query generation")
            )
        }

        return try {
            // Generate queries for each database
            val queries = mutableMapOf<String, DatabaseQuery>()

            // OpenAlex query (primary for MVP)
            val openAlexQuery = generateOpenAlexQuery(input)
            val openAlexValidation = queryValidator.validate(openAlexQuery, "openalex")

            queries["openalex"] = DatabaseQuery(
                database = "openalex",
                queryText = openAlexQuery,
                filters = emptyMap(),
                translationNotes = "Template-based Boolean query for OpenAlex API",
                validationStatus = openAlexValidation.status,
                validationMessages = openAlexValidation.messages
            )

            // Create query plan
            val queryPlan = DatabaseQueryPlan(
                queries = queries,
                generationMethod = GenerationMethod.TEMPLATE_BASED,
                validated = openAlexValidation.status != ValidationStatus.INVALID,
                approved = false,
                generatedAt = Clock.System.now()
            )

            // Validate overall plan
            if (!queryPlan.validated) {
                return StageResult.Failure(
                    PipelineError.ValidationFailed(
                        "Query validation failed",
                        openAlexValidation.messages
                    )
                )
            }

            // Save artifact
            artifactStore.save(
                projectId = "",
                artifact = queryPlan,
                serializer = DatabaseQueryPlan.serializer(),
                filename = "DatabaseQueryPlan.json"
            )

            // Return for approval
            StageResult.RequiresApproval(
                data = queryPlan,
                reason = "Review generated search queries",
                suggestions = buildList {
                    add("Verify query captures all important concepts")
                    add("Check for overly restrictive or broad terms")
                    if (openAlexValidation.status == ValidationStatus.WARNING) {
                        addAll(openAlexValidation.messages)
                    }
                }
            )

        } catch (e: Exception) {
            StageResult.Failure(
                PipelineError.Unknown(
                    "Query generation failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Generate OpenAlex Boolean query from concept expansion
     */
    private fun generateOpenAlexQuery(expansion: ConceptExpansion): String {
        val parts = mutableListOf<String>()

        // Population block
        val populationTerms = buildTermList(expansion.populationBlock)
        if (populationTerms.isNotEmpty()) {
            parts.add("(${populationTerms.joinToString(" OR ")})")
        }

        // Intervention block
        val interventionTerms = buildTermList(expansion.interventionBlock)
        if (interventionTerms.isNotEmpty()) {
            parts.add("(${interventionTerms.joinToString(" OR ")})")
        }

        // Outcome block
        val outcomeTerms = buildTermList(expansion.outcomeBlock)
        if (outcomeTerms.isNotEmpty()) {
            parts.add("(${outcomeTerms.joinToString(" OR ")})")
        }

        // Comparison block (optional)
        expansion.comparisonBlock?.let { comparison ->
            val comparisonTerms = buildTermList(comparison)
            if (comparisonTerms.isNotEmpty()) {
                parts.add("(${comparisonTerms.joinToString(" OR ")})")
            }
        }

        // Combine with AND
        return parts.joinToString(" AND ")
    }

    /**
     * Build list of search terms from a concept block
     */
    private fun buildTermList(block: ConceptBlock): List<String> {
        val terms = mutableListOf<String>()

        // Add core term
        terms.add(quoteIfNeeded(block.coreTerm))

        // Add synonyms
        terms.addAll(block.synonyms.map { quoteIfNeeded(it) })

        // Add related terms
        terms.addAll(block.relatedTerms.map { quoteIfNeeded(it) })

        return terms.distinct()
    }

    /**
     * Quote multi-word terms for exact phrase matching
     */
    private fun quoteIfNeeded(term: String): String {
        return if (term.contains(" ")) {
            "\"$term\""
        } else {
            term
        }
    }
}

/**
 * Query validator for anti-hallucination checking
 */
class QueryValidator {

    private val databaseSpecs = mapOf(
        "openalex" to DatabaseSpec(
            allowedOperators = setOf("AND", "OR", "NOT"),
            forbiddenOperators = setOf("NEAR", "ADJ", "PROX", "W/", "PRE/"),
            maxQueryLength = 4000
        ),
        "pubmed" to DatabaseSpec(
            allowedOperators = setOf("AND", "OR", "NOT"),
            forbiddenOperators = setOf("NEAR", "ADJ", "W/"),
            maxQueryLength = 4000
        ),
        "scopus" to DatabaseSpec(
            allowedOperators = setOf("AND", "OR", "AND NOT", "W/", "PRE/"),
            forbiddenOperators = emptySet(),
            maxQueryLength = 4000
        )
    )

    fun validate(query: String, database: String): QueryValidationResult {
        val spec = databaseSpecs[database]
            ?: return QueryValidationResult.warning("Unknown database: $database")

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check for forbidden operators
        spec.forbiddenOperators.forEach { op ->
            if (query.contains(op, ignoreCase = true)) {
                errors.add("Operator '$op' is not supported by $database")
            }
        }

        // Check query length
        if (query.length > spec.maxQueryLength) {
            warnings.add("Query is very long (${query.length} chars), may hit API limits")
        }

        // Check for balanced quotes
        val quoteCount = query.count { it == '"' }
        if (quoteCount % 2 != 0) {
            errors.add("Unbalanced quotes in query")
        }

        // Check for balanced parentheses
        var parenCount = 0
        query.forEach { char ->
            when (char) {
                '(' -> parenCount++
                ')' -> parenCount--
            }
            if (parenCount < 0) {
                errors.add("Unbalanced parentheses in query")
                return@forEach
            }
        }
        if (parenCount != 0) {
            errors.add("Unbalanced parentheses in query")
        }

        // Check for empty query
        if (query.isBlank()) {
            errors.add("Query is empty")
        }

        return when {
            errors.isNotEmpty() -> QueryValidationResult.invalid(errors.joinToString("; "))
            warnings.isNotEmpty() -> QueryValidationResult.warning(warnings.joinToString("; "))
            else -> QueryValidationResult.valid()
        }
    }
}

/**
 * Database specification for query validation
 */
private data class DatabaseSpec(
    val allowedOperators: Set<String>,
    val forbiddenOperators: Set<String>,
    val maxQueryLength: Int
)

