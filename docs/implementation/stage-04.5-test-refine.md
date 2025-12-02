# Stage 4.5: Test & Refine Protocol

**Purpose:** Execute test searches, analyze results, and iteratively refine queries before committing to full search.

**Status:** ⭐ Core differentiator for Lumen

---

## Data Models

```kotlin
@Serializable
data class TestSearchResult(
    val testRuns: List<TestRun>,
    val currentAnalysis: SearchAnalysis,
    val refinementSuggestions: List<RefinementSuggestion>,
    val approved: Boolean = false
)

@Serializable
data class TestRun(
    val runId: String,
    val iteration: Int,
    val timestamp: Instant,
    val queryPlan: DatabaseQueryPlan,
    val results: Map<String, DatabaseTestResult>,
    val analysis: SearchAnalysis
)

@Serializable
data class DatabaseTestResult(
    val database: String,
    val estimatedTotal: Int,
    val samplePapers: List<Document>, // Top 50
    val yearDistribution: Map<Int, Int>,
    val topVenues: List<String>
)

@Serializable
data class SearchAnalysis(
    val broadness: QueryBroadness,
    val totalAcrossDatabases: Int,
    val precisionEstimate: Double?,
    val recommendations: List<String>
)

@Serializable
enum class QueryBroadness {
    TOO_NARROW,   // < 100 total
    APPROPRIATE,  // 100-5000
    TOO_BROAD     // > 5000
}

@Serializable
data class RefinementSuggestion(
    val type: RefinementType,
    val description: String,
    val suggestedChange: String,
    val affectedDatabases: List<String>,
    val rationale: String
)

@Serializable
enum class RefinementType {
    ADD_EXCLUSION_TERM,
    NARROW_DATE_RANGE,
    ADD_FIELD_RESTRICTION,
    BROADEN_SYNONYMS,
    RELAX_FILTERS
}
```

---

## Implementation

```kotlin
class TestAndRefineStage(
    private val searchEngine: SearchEngine,
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<DatabaseQueryPlan, TestSearchResult> {
    
    companion object {
        const val TEST_SAMPLE_SIZE = 50
        const val MAX_ITERATIONS = 5
    }
    
    override suspend fun execute(input: DatabaseQueryPlan): StageResult<TestSearchResult> {
        val testRuns = mutableListOf<TestRun>()
        var currentPlan = input
        var iteration = 1
        
        // Run initial test
        var testRun = executeTestSearch(currentPlan, iteration)
        testRuns.add(testRun)
        
        // Check if refinement needed
        if (testRun.analysis.broadness == QueryBroadness.APPROPRIATE) {
            return StageResult.Success(
                TestSearchResult(
                    testRuns = testRuns,
                    currentAnalysis = testRun.analysis,
                    refinementSuggestions = emptyList(),
                    approved = false
                )
            )
        }
        
        // Generate refinement suggestions
        val suggestions = generateRefinements(testRun)
        
        return StageResult.RequiresApproval(
            data = TestSearchResult(
                testRuns = testRuns,
                currentAnalysis = testRun.analysis,
                refinementSuggestions = suggestions,
                approved = false
            ),
            reason = "Query scope is ${testRun.analysis.broadness}. Review suggestions and iterate."
        )
    }
    
    private suspend fun executeTestSearch(
        plan: DatabaseQueryPlan,
        iteration: Int
    ): TestRun {
        val results = mutableMapOf<String, DatabaseTestResult>()
        
        for ((dbName, query) in plan.queries) {
            try {
                val searchResult = searchEngine.search(
                    database = dbName,
                    query = query.queryText,
                    filters = query.filters,
                    limit = TEST_SAMPLE_SIZE
                )
                
                results[dbName] = DatabaseTestResult(
                    database = dbName,
                    estimatedTotal = searchResult.totalCount,
                    samplePapers = searchResult.documents.take(TEST_SAMPLE_SIZE),
                    yearDistribution = computeYearDistribution(searchResult.documents),
                    topVenues = extractTopVenues(searchResult.documents, limit = 5)
                )
            } catch (e: Exception) {
                // Log but continue with other databases
                println("Test search failed for $dbName: ${e.message}")
            }
        }
        
        val analysis = analyzeResults(results)
        
        return TestRun(
            runId = "test_${Clock.System.now().epochSeconds}_$iteration",
            iteration = iteration,
            timestamp = Clock.System.now(),
            queryPlan = plan,
            results = results,
            analysis = analysis
        )
    }
    
    private fun analyzeResults(results: Map<String, DatabaseTestResult>): SearchAnalysis {
        val total = results.values.sumOf { it.estimatedTotal }
        
        val broadness = when {
            total < 100 -> QueryBroadness.TOO_NARROW
            total > 5000 -> QueryBroadness.TOO_BROAD
            else -> QueryBroadness.APPROPRIATE
        }
        
        val recommendations = buildList {
            when (broadness) {
                QueryBroadness.TOO_NARROW -> {
                    add("Consider adding more synonyms")
                    add("Broaden date range if too restrictive")
                    add("Check for overly specific field restrictions")
                }
                QueryBroadness.TOO_BROAD -> {
                    add("Add exclusion terms (e.g., NOT blockchain)")
                    add("Narrow date range to recent years")
                    add("Add more specific outcome terms")
                }
                QueryBroadness.APPROPRIATE -> {
                    add("Query scope looks good")
                }
            }
        }
        
        return SearchAnalysis(
            broadness = broadness,
            totalAcrossDatabases = total,
            precisionEstimate = null,
            recommendations = recommendations
        )
    }
    
    private suspend fun generateRefinements(testRun: TestRun): List<RefinementSuggestion> {
        val prompt = buildRefinementPrompt(testRun)
        
        return try {
            llmService.generateStructured(
                prompt = prompt,
                schema = ListSerializer(RefinementSuggestion.serializer()),
                temperature = 0.3
            )
        } catch (e: Exception) {
            // Fallback to rule-based suggestions
            generateRuleBasedSuggestions(testRun)
        }
    }
    
    private fun buildRefinementPrompt(testRun: TestRun): String {
        val total = testRun.results.values.sumOf { it.estimatedTotal }
        val sampleTitles = testRun.results.values
            .flatMap { it.samplePapers }
            .take(10)
            .map { it.title }
        
        return """
        Test search returned $total results (${testRun.analysis.broadness}).
        
        Sample paper titles:
        ${sampleTitles.joinToString("\n") { "- $it" }}
        
        Current queries:
        ${testRun.queryPlan.queries.entries.joinToString("\n") { (db, q) -> 
            "$db: ${q.queryText}"
        }}
        
        Goal: ${if (testRun.analysis.broadness == QueryBroadness.TOO_BROAD) "Narrow" else "Broaden"} the search.
        
        Provide 2-3 specific, actionable refinement suggestions.
        Return JSON array matching RefinementSuggestion schema.
        """.trimIndent()
    }
    
    private fun generateRuleBasedSuggestions(testRun: TestRun): List<RefinementSuggestion> {
        return when (testRun.analysis.broadness) {
            QueryBroadness.TOO_BROAD -> listOf(
                RefinementSuggestion(
                    type = RefinementType.NARROW_DATE_RANGE,
                    description = "Limit to recent publications",
                    suggestedChange = "Add year filter: 2019-2024",
                    affectedDatabases = testRun.results.keys.toList(),
                    rationale = "Reduces results while focusing on current research"
                )
            )
            QueryBroadness.TOO_NARROW -> listOf(
                RefinementSuggestion(
                    type = RefinementType.BROADEN_SYNONYMS,
                    description = "Add more intervention synonyms",
                    suggestedChange = "Include: AI, neural networks, computer vision",
                    affectedDatabases = testRun.results.keys.toList(),
                    rationale = "Captures related methods"
                )
            )
            QueryBroadness.APPROPRIATE -> emptyList()
        }
    }
    
    private fun computeYearDistribution(docs: List<Document>): Map<Int, Int> {
        return docs
            .mapNotNull { it.year }
            .groupingBy { it }
            .eachCount()
    }
    
    private fun extractTopVenues(docs: List<Document>, limit: Int): List<String> {
        return docs
            .mapNotNull { it.venue }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
}
```

---

## Desktop UI

```kotlin
@Composable
fun TestRefineScreen(viewModel: TestRefineViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Test & Refine Search Strategy",
            style = MaterialTheme.typography.headlineMedium
        )
        
        when (val state = viewModel.state) {
            is TestRefineState.Testing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Running test searches (50 papers per database)...")
            }
            
            is TestRefineState.Results -> {
                // Results summary cards
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.results.entries.toList()) { (db, result) ->
                        DatabaseResultCard(db, result)
                    }
                }
                
                // Broadness indicator
                BroadnessCard(state.analysis.broadness)
                
                // Refinement suggestions
                if (state.suggestions.isNotEmpty()) {
                    Text(
                        "Suggested Refinements",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    state.suggestions.forEachIndexed { index, suggestion ->
                        RefinementSuggestionCard(
                            suggestion = suggestion,
                            onApply = { viewModel.applySuggestion(index) }
                        )
                    }
                }
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.retestWithChanges() },
                        enabled = viewModel.hasChanges
                    ) {
                        Text("↻ Re-test")
                    }
                    
                    Button(
                        onClick = { viewModel.approveAndContinue() },
                        enabled = state.analysis.broadness == QueryBroadness.APPROPRIATE
                    ) {
                        Text("✓ Approve & Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun BroadnessCard(broadness: QueryBroadness) {
    val (color, icon, message) = when (broadness) {
        QueryBroadness.TOO_NARROW -> Triple(
            Color(0xFFFF9800),
            "⚠️",
            "Too narrow (< 100 results)"
        )
        QueryBroadness.APPROPRIATE -> Triple(
            Color(0xFF4CAF50),
            "✓",
            "Appropriate scope (100-5000 results)"
        )
        QueryBroadness.TOO_BROAD -> Triple(
            Color(0xFFF44336),
            "⚠️",
            "Too broad (> 5000 results)"
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, color = Color.White, fontSize = 18.sp)
        }
    }
}
```

---

## CLI

```bash
# Run test search
lumen test --project <id>

# Output:
# Running test searches...
# OpenAlex: 1,247 results (50 sampled)
# Crossref: 892 results (50 sampled)
# arXiv: 45 results (45 sampled)
# 
# Total: 2,184 results → APPROPRIATE
# ✓ Query scope looks good

# If too broad:
# Total: 8,543 results → TOO_BROAD
# Suggestions:
#   1. Add exclusion: NOT blockchain
#   2. Narrow date range: 2020-2024

# Apply suggestion #1 and re-test
lumen test --project <id> --apply 1

# Skip test & refine (not recommended)
lumen run --project <id> --skip-test
```

---

## Tests

```kotlin
class TestAndRefineStageTest {
    
    @Test
    fun `detects too-broad query`() = runTest {
        val mockEngine = MockSearchEngine()
        mockEngine.mockResult("openalex", totalCount = 8543)
        
        val stage = TestAndRefineStage(mockEngine, mockLlm, mockStore)
        val plan = createMockQueryPlan()
        
        val result = stage.execute(plan)
        
        assertIs<StageResult.RequiresApproval>(result)
        assertEquals(QueryBroadness.TOO_BROAD, result.data.currentAnalysis.broadness)
        assertTrue(result.data.refinementSuggestions.isNotEmpty())
    }
    
    @Test
    fun `accepts appropriate query scope`() = runTest {
        val mockEngine = MockSearchEngine()
        mockEngine.mockResult("openalex", totalCount = 892)
        
        val stage = TestAndRefineStage(mockEngine, mockLlm, mockStore)
        val plan = createMockQueryPlan()
        
        val result = stage.execute(plan)
        
        assertIs<StageResult.Success>(result)
        assertEquals(QueryBroadness.APPROPRIATE, result.data.currentAnalysis.broadness)
        assertTrue(result.data.refinementSuggestions.isEmpty())
    }
}
```

---

## Edge Cases

1. **API failures**: Continue with other databases, warn user
2. **Zero results**: Flag as critical issue, suggest broader terms
3. **Max iterations reached**: Force approval or abort
4. **LLM refinement fails**: Fall back to rule-based suggestions

---

## Next Stage

→ [Stage 5: Screening Criteria](stage-05-screening-criteria.md)
