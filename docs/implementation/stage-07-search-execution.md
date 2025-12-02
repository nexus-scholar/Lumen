# Stage 7: Search Execution

**Purpose:** Execute validated queries across all databases and collect results.

---

## Data Models

```kotlin
@Serializable
data class SearchResults(
    val searchId: String,
    val executedAt: Instant,
    val queryPlan: DatabaseQueryPlan,
    val results: Map<String, DatabaseSearchResult>,
    val totalDocuments: Int,
    val executionLog: List<ExecutionLogEntry>
)

@Serializable
data class DatabaseSearchResult(
    val database: String,
    val query: String,
    val totalCount: Int,
    val documents: List<Document>,
    val executionTimeMs: Long,
    val error: String? = null
)

@Serializable
data class ExecutionLogEntry(
    val timestamp: Instant,
    val database: String,
    val action: String,
    val details: String
)
```

---

## Implementation

```kotlin
class SearchExecutionStage(
    private val searchEngine: SearchEngine,
    private val documentStore: DocumentStore
) : PipelineStage<DatabaseQueryPlan, SearchResults> {
    
    override suspend fun execute(input: DatabaseQueryPlan): StageResult<SearchResults> {
        if (!input.approved) {
            return StageResult.Failure(
                PipelineError.PreconditionFailed("Query plan must be approved")
            )
        }
        
        val searchId = "search_${Clock.System.now().epochSeconds}"
        val results = mutableMapOf<String, DatabaseSearchResult>()
        val log = mutableListOf<ExecutionLogEntry>()
        
        // Execute searches in parallel
        coroutineScope {
            input.queries.map { (dbName, query) ->
                async {
                    executeForDatabase(dbName, query, log)
                }
            }.awaitAll().forEach { result ->
                results[result.database] = result
            }
        }
        
        // Store all documents
        val allDocs = results.values.flatMap { it.documents }
        documentStore.saveDocuments(allDocs)
        
        val searchResults = SearchResults(
            searchId = searchId,
            executedAt = Clock.System.now(),
            queryPlan = input,
            results = results,
            totalDocuments = allDocs.size,
            executionLog = log
        )
        
        return StageResult.Success(searchResults)
    }
    
    private suspend fun executeForDatabase(
        dbName: String,
        query: DatabaseQuery,
        log: MutableList<ExecutionLogEntry>
    ): DatabaseSearchResult {
        log.add(ExecutionLogEntry(
            timestamp = Clock.System.now(),
            database = dbName,
            action = "START",
            details = "Executing query"
        ))
        
        val startTime = Clock.System.now()
        
        return try {
            val result = searchEngine.search(
                database = dbName,
                query = query.queryText,
                filters = query.filters
            )
            
            val endTime = Clock.System.now()
            val durationMs = (endTime - startTime).inWholeMilliseconds
            
            log.add(ExecutionLogEntry(
                timestamp = endTime,
                database = dbName,
                action = "COMPLETE",
                details = "Retrieved ${result.documents.size} documents in ${durationMs}ms"
            ))
            
            DatabaseSearchResult(
                database = dbName,
                query = query.queryText,
                totalCount = result.totalCount,
                documents = result.documents,
                executionTimeMs = durationMs
            )
        } catch (e: Exception) {
            log.add(ExecutionLogEntry(
                timestamp = Clock.System.now(),
                database = dbName,
                action = "ERROR",
                details = e.message ?: "Unknown error"
            ))
            
            DatabaseSearchResult(
                database = dbName,
                query = query.queryText,
                totalCount = 0,
                documents = emptyList(),
                executionTimeMs = 0,
                error = e.message
            )
        }
    }
}
```

---

## Search Engine

```kotlin
class SearchEngine(
    private val providers: Map<String, SearchProvider>
) {
    suspend fun search(
        database: String,
        query: String,
        filters: Map<String, String> = emptyMap(),
        limit: Int = Int.MAX_VALUE
    ): SearchResult {
        val provider = providers[database]
            ?: throw IllegalArgumentException("Unknown database: $database")
        
        return provider.search(query, filters, limit)
    }
}

interface SearchProvider {
    suspend fun search(
        query: String,
        filters: Map<String, String>,
        limit: Int
    ): SearchResult
}

data class SearchResult(
    val totalCount: Int,
    val documents: List<Document>
)
```

---

## Provider Implementations

### OpenAlex Provider

```kotlin
class OpenAlexProvider(private val client: HttpClient) : SearchProvider {
    
    override suspend fun search(
        query: String,
        filters: Map<String, String>,
        limit: Int
    ): SearchResult {
        val params = buildMap {
            put("filter", query)
            filters.forEach { (k, v) -> put(k, v) }
            put("per-page", "200")
        }
        
        val allDocs = mutableListOf<Document>()
        var cursor: String? = null
        
        while (allDocs.size < limit) {
            val response = client.get("https://api.openalex.org/works") {
                params.forEach { (k, v) -> parameter(k, v) }
                if (cursor != null) parameter("cursor", cursor)
            }.body<OpenAlexResponse>()
            
            allDocs.addAll(response.results.map { it.toDocument() })
            
            cursor = response.meta.next_cursor
            if (cursor == null) break
        }
        
        return SearchResult(
            totalCount = allDocs.size,
            documents = allDocs.take(limit)
        )
    }
}
```

---

## Desktop UI

```kotlin
@Composable
fun SearchExecutionScreen(viewModel: SearchViewModel) {
    when (val state = viewModel.state) {
        is SearchState.Running -> {
            Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                Text("Executing Search...", style = MaterialTheme.typography.headlineMedium)
                
                state.progress.forEach { (db, status) ->
                    DatabaseProgressCard(db, status)
                }
                
                LinearProgressIndicator(
                    progress = state.overallProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        is SearchState.Completed -> {
            Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                Text("Search Complete", style = MaterialTheme.typography.headlineMedium)
                
                Text("Total: ${state.results.totalDocuments} documents")
                
                state.results.results.forEach { (db, result) ->
                    ResultSummaryCard(db, result)
                }
                
                Button(onClick = { viewModel.continueToDeduplication() }) {
                    Text("Continue to Deduplication →")
                }
            }
        }
    }
}
```

---

## CLI

```bash
# Execute full search
lumen search --project <id>

# Output:
# Executing searches...
# [OpenAlex] 347 documents (2.3s)
# [Crossref] 215 documents (1.8s)
# [arXiv] 89 documents (0.9s)
# [Semantic Scholar] 125 documents (1.2s)
# 
# Total: 776 documents
# Saved to: data/<project>/project.db
```

---

## Tests

```kotlin
class SearchExecutionStageTest {
    
    @Test
    fun `executes search across multiple databases`() = runTest {
        val mockEngine = MockSearchEngine()
        mockEngine.mockResult("openalex", documents = List(100) { mockDocument() })
        mockEngine.mockResult("crossref", documents = List(50) { mockDocument() })
        
        val stage = SearchExecutionStage(mockEngine, mockStore)
        val plan = createApprovedQueryPlan()
        
        val result = stage.execute(plan)
        
        assertIs<StageResult.Success>(result)
        assertEquals(150, result.data.totalDocuments)
    }
}
```

---

## Next Stage

→ [Stage 8: Citation Expansion](stage-08-citation-expansion.md)
