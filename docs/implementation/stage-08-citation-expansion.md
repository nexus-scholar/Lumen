# Stage 8: Citation Expansion (Snowballing)

**Purpose:** Expand search results through forward/backward citation chaining.

---

## Data Models

```kotlin
@Serializable
data class CitationExpansionConfig(
    val seedPapers: List<String>, // DOIs
    val forwardCitations: Boolean = true,
    val backwardCitations: Boolean = true,
    val depth: Int = 1, // 1-3 iterations
    val maxCitationsPerPaper: Int = 100
)

@Serializable
data class ExpandedSearchResults(
    val originalResults: SearchResults,
    val expansionConfig: CitationExpansionConfig,
    val citationGraph: CitationGraph,
    val addedDocuments: List<Document>,
    val totalDocuments: Int
)

@Serializable
data class CitationGraph(
    val nodes: List<CitationNode>,
    val edges: List<CitationEdge>
)

@Serializable
data class CitationNode(
    val doi: String,
    val title: String,
    val isSeed: Boolean
)

@Serializable
data class CitationEdge(
    val from: String, // citing DOI
    val to: String,   // cited DOI
    val type: CitationType
)

@Serializable
enum class CitationType {
    FORWARD,  // Paper cites seed
    BACKWARD  // Seed cites paper
}
```

---

## Implementation

```kotlin
class CitationExpansionStage(
    private val citationService: CitationService
) : PipelineStage<SearchResults, ExpandedSearchResults> {
    
    override suspend fun execute(input: SearchResults): StageResult<ExpandedSearchResults> {
        // Select seed papers (top 20 by citation count)
        val allDocs = input.results.values.flatMap { it.documents }
        val seeds = selectSeedPapers(allDocs)
        
        if (seeds.isEmpty()) {
            return StageResult.Success(
                ExpandedSearchResults(
                    originalResults = input,
                    expansionConfig = CitationExpansionConfig(emptyList()),
                    citationGraph = CitationGraph(emptyList(), emptyList()),
                    addedDocuments = emptyList(),
                    totalDocuments = allDocs.size
                )
            )
        }
        
        val config = CitationExpansionConfig(
            seedPapers = seeds.mapNotNull { it.doi },
            forwardCitations = true,
            backwardCitations = true,
            depth = 1
        )
        
        val expanded = expandCitations(seeds, allDocs, config)
        
        return StageResult.Success(expanded)
    }
    
    private fun selectSeedPapers(documents: List<Document>): List<Document> {
        return documents
            .filter { it.doi != null && it.citationCount != null }
            .sortedByDescending { it.citationCount }
            .take(20)
    }
    
    private suspend fun expandCitations(
        seeds: List<Document>,
        existingDocs: List<Document>,
        config: CitationExpansionConfig
    ): ExpandedSearchResults {
        val existingDois = existingDocs.mapNotNull { it.doi }.toSet()
        val addedDocs = mutableListOf<Document>()
        val edges = mutableListOf<CitationEdge>()
        
        for (seed in seeds) {
            val seedDoi = seed.doi ?: continue
            
            // Forward citations
            if (config.forwardCitations) {
                val citing = citationService.getForwardCitations(seedDoi)
                    .filter { it.doi !in existingDois }
                    .take(config.maxCitationsPerPaper)
                
                addedDocs.addAll(citing)
                edges.addAll(citing.mapNotNull { doc ->
                    doc.doi?.let { CitationEdge(it, seedDoi, CitationType.FORWARD) }
                })
            }
            
            // Backward citations
            if (config.backwardCitations) {
                val cited = citationService.getBackwardCitations(seedDoi)
                    .filter { it.doi !in existingDois }
                    .take(config.maxCitationsPerPaper)
                
                addedDocs.addAll(cited)
                edges.addAll(cited.mapNotNull { doc ->
                    doc.doi?.let { CitationEdge(seedDoi, it, CitationType.BACKWARD) }
                })
            }
        }
        
        val nodes = (seeds + addedDocs).mapNotNull { doc ->
            doc.doi?.let {
                CitationNode(
                    doi = it,
                    title = doc.title,
                    isSeed = doc in seeds
                )
            }
        }
        
        return ExpandedSearchResults(
            originalResults = SearchResults(/* ... */),
            expansionConfig = config,
            citationGraph = CitationGraph(nodes, edges),
            addedDocuments = addedDocs,
            totalDocuments = existingDocs.size + addedDocs.size
        )
    }
}

interface CitationService {
    suspend fun getForwardCitations(doi: String): List<Document>
    suspend fun getBackwardCitations(doi: String): List<Document>
}

class OpenAlexCitationService(private val client: HttpClient) : CitationService {
    
    override suspend fun getForwardCitations(doi: String): List<Document> {
        val response = client.get("https://api.openalex.org/works") {
            parameter("filter", "cites:$doi")
            parameter("per-page", "100")
        }.body<OpenAlexResponse>()
        
        return response.results.map { it.toDocument() }
    }
    
    override suspend fun getBackwardCitations(doi: String): List<Document> {
        val work = client.get("https://api.openalex.org/works/$doi")
            .body<OpenAlexWork>()
        
        val citedDois = work.referenced_works
        return citedDois.mapNotNull { citedDoi ->
            try {
                client.get("https://api.openalex.org/works/$citedDoi")
                    .body<OpenAlexWork>()
                    .toDocument()
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

---

## Desktop UI

```kotlin
@Composable
fun CitationExpansionScreen(viewModel: CitationViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Citation Expansion (Snowballing)", style = MaterialTheme.typography.headlineMedium)
        
        // Seed selection
        Text("Selected Seeds: ${viewModel.seeds.size} papers")
        LazyColumn {
            items(viewModel.seeds) { seed ->
                SeedPaperCard(seed)
            }
        }
        
        // Configuration
        Row {
            Checkbox(
                checked = viewModel.forwardCitations,
                onCheckedChange = { viewModel.forwardCitations = it }
            )
            Text("Forward citations (papers citing seeds)")
        }
        
        Row {
            Checkbox(
                checked = viewModel.backwardCitations,
                onCheckedChange = { viewModel.backwardCitations = it }
            )
            Text("Backward citations (papers cited by seeds)")
        }
        
        Button(onClick = { viewModel.expand() }) {
            Text("Expand Citations")
        }
    }
}
```

---

## CLI

```bash
# Auto-select seeds and expand
lumen expand --project <id>

# Output:
# Selected 20 seed papers
# Fetching forward citations... 247 papers
# Fetching backward citations... 189 papers
# Total added: 436 papers
```

---

## Tests

```kotlin
class CitationExpansionStageTest {
    
    @Test
    fun `expands citations from seed papers`() = runTest {
        val mockService = MockCitationService()
        mockService.mockForward("10.1000/seed1", List(10) { mockDocument() })
        mockService.mockBackward("10.1000/seed1", List(5) { mockDocument() })
        
        val stage = CitationExpansionStage(mockService)
        val searchResults = createMockSearchResults()
        
        val result = stage.execute(searchResults)
        
        assertIs<StageResult.Success>(result)
        assertTrue(result.data.addedDocuments.size >= 15)
    }
}
```

---

## Next Stage

→ [Stage 9: Deduplication](stage-09-deduplication.md)
