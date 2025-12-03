package com.lumen.core.domain.stages

import com.lumen.core.data.providers.SearchProvider
import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.services.llm.LlmService
import kotlinx.datetime.Clock

/**
 * Stage 4.5: Test & Refine Protocol
 * Executes test searches and iteratively refines queries
 */
class TestAndRefineStage(
    private val searchProviders: Map<String, SearchProvider>,
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<DatabaseQueryPlan, TestSearchResult> {

    override val stageName: String = "Stage 4.5: Test & Refine"

    companion object {
        const val TEST_SAMPLE_SIZE = 50
        const val MAX_ITERATIONS = 5
        const val TOO_NARROW_THRESHOLD = 100
        const val TOO_BROAD_THRESHOLD = 5000
    }

    override suspend fun execute(input: DatabaseQueryPlan): StageResult<TestSearchResult> {
        // Check if query plan is approved for testing
        if (!input.validated) {
            return StageResult.Failure(
                PipelineError.PreconditionFailed("Query plan must be validated before testing")
            )
        }

        return try {
            val testRuns = mutableListOf<TestRun>()
            var currentPlan = input
            var iteration = 1

            // Execute initial test search
            var testRun = executeTestSearch(currentPlan, iteration)
            testRuns.add(testRun)

            // Check if refinement is needed
            while (testRun.analysis.broadness != QueryBroadness.APPROPRIATE &&
                   iteration < MAX_ITERATIONS) {

                // Generate refinement suggestions
                val suggestions = generateRefinementSuggestions(testRun)

                // For MVP, we'll return for manual refinement
                // In future, could auto-apply suggestions
                val result = TestSearchResult(
                    testRuns = testRuns,
                    currentAnalysis = testRun.analysis,
                    refinementSuggestions = suggestions,
                    approved = false
                )

                artifactStore.save(
                    projectId = "",
                    artifact = result,
                    serializer = TestSearchResult.serializer(),
                    filename = "TestSearchResult.json"
                )

                return StageResult.RequiresApproval(
                    data = result,
                    reason = buildString {
                        append("Query is ${testRun.analysis.broadness.name.lowercase().replace('_', ' ')}")
                        append(" (${testRun.analysis.totalAcrossDatabases} total results)")
                    },
                    suggestions = suggestions.map { it.description }
                )
            }

            // Query is appropriate
            val result = TestSearchResult(
                testRuns = testRuns,
                currentAnalysis = testRun.analysis,
                refinementSuggestions = emptyList(),
                approved = true
            )

            artifactStore.save(
                projectId = "",
                artifact = result,
                serializer = TestSearchResult.serializer(),
                filename = "TestSearchResult.json"
            )

            StageResult.Success(result)

        } catch (e: Exception) {
            StageResult.Failure(
                PipelineError.ApiCallFailed(
                    "Test search failed: ${e.message}",
                    "multiple",
                    e
                )
            )
        }
    }

    /**
     * Execute a test search across all databases
     */
    private suspend fun executeTestSearch(
        queryPlan: DatabaseQueryPlan,
        iteration: Int
    ): TestRun {
        val results = mutableMapOf<String, DatabaseTestResult>()

        // Execute test searches
        queryPlan.queries.forEach { (dbName, query) ->
            val provider = searchProviders[dbName]
            if (provider != null && query.validationStatus != ValidationStatus.INVALID) {
                try {
                    val searchResult = provider.search(
                        query = query.queryText,
                        limit = TEST_SAMPLE_SIZE,
                        offset = 0,
                        filters = query.filters
                    )

                    // Estimate total count
                    val totalCount = provider.estimateCount(
                        query = query.queryText,
                        filters = query.filters
                    )

                    // Analyze sample
                    val yearDist = searchResult.documents
                        .mapNotNull { it.year }
                        .groupingBy { it }
                        .eachCount()

                    val topVenues = searchResult.documents
                        .mapNotNull { it.venue }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(10)
                        .map { it.key }

                    results[dbName] = DatabaseTestResult(
                        database = dbName,
                        estimatedTotal = totalCount,
                        samplePapers = searchResult.documents,
                        yearDistribution = yearDist,
                        topVenues = topVenues
                    )
                } catch (e: Exception) {
                    // Skip failed providers
                }
            }
        }

        // Analyze results
        val totalCount = results.values.sumOf { it.estimatedTotal }
        val broadness = when {
            totalCount < TOO_NARROW_THRESHOLD -> QueryBroadness.TOO_NARROW
            totalCount > TOO_BROAD_THRESHOLD -> QueryBroadness.TOO_BROAD
            else -> QueryBroadness.APPROPRIATE
        }

        val analysis = SearchAnalysis(
            broadness = broadness,
            totalAcrossDatabases = totalCount,
            precisionEstimate = null, // Could be calculated with known-relevant papers
            recommendations = buildRecommendations(broadness, totalCount)
        )

        return TestRun(
            runId = "test_${Clock.System.now().epochSeconds}_$iteration",
            iteration = iteration,
            timestamp = Clock.System.now(),
            queryPlan = queryPlan,
            results = results,
            analysis = analysis
        )
    }

    /**
     * Generate refinement suggestions based on test results
     */
    private suspend fun generateRefinementSuggestions(
        testRun: TestRun
    ): List<RefinementSuggestion> {
        val suggestions = mutableListOf<RefinementSuggestion>()

        when (testRun.analysis.broadness) {
            QueryBroadness.TOO_NARROW -> {
                suggestions.add(
                    RefinementSuggestion(
                        type = RefinementType.BROADEN_SYNONYMS,
                        description = "Add more synonyms and related terms",
                        suggestedChange = "Review concept expansion and add broader terms",
                        affectedDatabases = testRun.results.keys.toList(),
                        rationale = "Only ${testRun.analysis.totalAcrossDatabases} results found, which may miss relevant studies"
                    )
                )

                suggestions.add(
                    RefinementSuggestion(
                        type = RefinementType.RELAX_FILTERS,
                        description = "Remove or relax date restrictions",
                        suggestedChange = "Consider removing date filters or expanding date range",
                        affectedDatabases = testRun.results.keys.toList(),
                        rationale = "Broadening search criteria may capture more relevant studies"
                    )
                )
            }

            QueryBroadness.TOO_BROAD -> {
                suggestions.add(
                    RefinementSuggestion(
                        type = RefinementType.ADD_EXCLUSION_TERM,
                        description = "Add exclusion terms for irrelevant topics",
                        suggestedChange = "Identify common irrelevant topics in sample and exclude them",
                        affectedDatabases = testRun.results.keys.toList(),
                        rationale = "${testRun.analysis.totalAcrossDatabases} results is too many for manual screening"
                    )
                )

                suggestions.add(
                    RefinementSuggestion(
                        type = RefinementType.NARROW_DATE_RANGE,
                        description = "Restrict to more recent publications",
                        suggestedChange = "Add date filter (e.g., last 10 years)",
                        affectedDatabases = testRun.results.keys.toList(),
                        rationale = "Narrowing time range reduces volume while keeping recent evidence"
                    )
                )

                suggestions.add(
                    RefinementSuggestion(
                        type = RefinementType.ADD_FIELD_RESTRICTION,
                        description = "Restrict search to title/abstract only",
                        suggestedChange = "Limit search to title and abstract fields",
                        affectedDatabases = testRun.results.keys.toList(),
                        rationale = "Full-text search may be too broad"
                    )
                )
            }

            QueryBroadness.APPROPRIATE -> {
                // No suggestions needed
            }
        }

        return suggestions
    }

    /**
     * Build recommendations based on analysis
     */
    private fun buildRecommendations(
        broadness: QueryBroadness,
        totalCount: Int
    ): List<String> {
        return when (broadness) {
            QueryBroadness.TOO_NARROW -> listOf(
                "Query may be too restrictive ($totalCount results)",
                "Consider broadening search terms",
                "Review concept expansion for missing synonyms",
                "Check if filters are too strict"
            )
            QueryBroadness.TOO_BROAD -> listOf(
                "Query may be too broad ($totalCount results)",
                "Consider adding exclusion criteria",
                "Narrow date range if appropriate",
                "Add more specific terms to key concepts"
            )
            QueryBroadness.APPROPRIATE -> listOf(
                "Query appears well-balanced ($totalCount results)",
                "Ready to proceed with full search",
                "Review sample results for relevance"
            )
        }
    }
}

