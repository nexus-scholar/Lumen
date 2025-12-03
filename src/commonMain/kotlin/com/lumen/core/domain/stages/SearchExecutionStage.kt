package com.lumen.core.domain.stages

import com.lumen.core.data.providers.SearchProvider
import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock

/**
 * Stage 7: Search Execution
 * Executes validated queries across all databases and collects results
 */
class SearchExecutionStage(
    private val searchProviders: Map<String, SearchProvider>,
    private val saveDocuments: suspend (String, List<Document>) -> Unit,
    private val artifactStore: ArtifactStore
) : PipelineStage<TestSearchResult, SearchResults> {

    override val stageName: String = "Stage 7: Search Execution"

    override suspend fun execute(input: TestSearchResult): StageResult<SearchResults> {
        // Check if test results are approved
        if (!input.approved) {
            return StageResult.Failure(
                PipelineError.PreconditionFailed("Test search results must be approved before full execution")
            )
        }

        // Get the approved query plan from the latest test run
        val queryPlan = input.testRuns.lastOrNull()?.queryPlan
            ?: return StageResult.Failure(
                PipelineError.PreconditionFailed("No query plan found in test results")
            )

        return try {
            val searchId = "search_${Clock.System.now().epochSeconds}"
            val executionLog = mutableListOf<ExecutionLogEntry>()

            // Execute searches in parallel
            val results = executeParallelSearches(queryPlan, executionLog)

            // Collect all documents
            val allDocuments = results.values.flatMap { it.documents }

            // Save documents to database
            saveDocuments("", allDocuments) // projectId will be set by orchestrator

            // Create search results
            val searchResults = SearchResults(
                searchId = searchId,
                projectId = "", // Will be set by orchestrator
                executedAt = Clock.System.now(),
                queryPlan = queryPlan,
                results = results,
                totalDocuments = allDocuments.size,
                executionLog = executionLog
            )

            // Save artifact
            artifactStore.save(
                projectId = "",
                artifact = searchResults,
                serializer = SearchResults.serializer(),
                filename = "SearchResults.json"
            )

            StageResult.Success(searchResults)

        } catch (e: Exception) {
            StageResult.Failure(
                PipelineError.ApiCallFailed(
                    "Search execution failed: ${e.message}",
                    "multiple",
                    e
                )
            )
        }
    }

    /**
     * Execute searches across all providers in parallel
     */
    private suspend fun executeParallelSearches(
        queryPlan: DatabaseQueryPlan,
        log: MutableList<ExecutionLogEntry>
    ): Map<String, DatabaseSearchResult> = coroutineScope {
        queryPlan.queries.map { (dbName, query) ->
            async {
                executeForDatabase(dbName, query, log)
            }
        }.awaitAll().associateBy { it.database }
    }

    /**
     * Execute search for a single database
     */
    private suspend fun executeForDatabase(
        dbName: String,
        query: DatabaseQuery,
        log: MutableList<ExecutionLogEntry>
    ): DatabaseSearchResult {
        val startTime = Clock.System.now()

        log.add(
            ExecutionLogEntry(
                timestamp = startTime,
                database = dbName,
                action = "START",
                details = "Executing search query"
            )
        )

        val provider = searchProviders[dbName]
        if (provider == null) {
            log.add(
                ExecutionLogEntry(
                    timestamp = Clock.System.now(),
                    database = dbName,
                    action = "ERROR",
                    details = "Provider not available"
                )
            )
            return DatabaseSearchResult(
                database = dbName,
                query = query.queryText,
                totalCount = 0,
                documents = emptyList(),
                executionTimeMs = 0,
                error = "Provider not available"
            )
        }

        return try {
            // Fetch all results with pagination
            val allDocuments = mutableListOf<Document>()
            var offset = 0
            val pageSize = 200
            var hasMore = true

            while (hasMore) {
                val result = provider.search(
                    query = query.queryText,
                    limit = pageSize,
                    offset = offset,
                    filters = query.filters
                )

                allDocuments.addAll(result.documents)
                offset += pageSize
                hasMore = result.hasMore && allDocuments.size < 10000 // Cap at 10k per database

                log.add(
                    ExecutionLogEntry(
                        timestamp = Clock.System.now(),
                        database = dbName,
                        action = "PROGRESS",
                        details = "Retrieved ${allDocuments.size} documents"
                    )
                )
            }

            val executionTime = (Clock.System.now() - startTime).inWholeMilliseconds

            log.add(
                ExecutionLogEntry(
                    timestamp = Clock.System.now(),
                    database = dbName,
                    action = "COMPLETE",
                    details = "Retrieved ${allDocuments.size} documents in ${executionTime}ms"
                )
            )

            DatabaseSearchResult(
                database = dbName,
                query = query.queryText,
                totalCount = allDocuments.size,
                documents = allDocuments,
                executionTimeMs = executionTime,
                error = null
            )

        } catch (e: Exception) {
            val executionTime = (Clock.System.now() - startTime).inWholeMilliseconds

            log.add(
                ExecutionLogEntry(
                    timestamp = Clock.System.now(),
                    database = dbName,
                    action = "ERROR",
                    details = "Search failed: ${e.message}"
                )
            )

            DatabaseSearchResult(
                database = dbName,
                query = query.queryText,
                totalCount = 0,
                documents = emptyList(),
                executionTimeMs = executionTime,
                error = e.message
            )
        }
    }
}


