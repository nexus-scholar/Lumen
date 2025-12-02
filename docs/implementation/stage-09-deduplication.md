# Stage 9: Deduplication

**Purpose:** Remove duplicate documents using conservative fuzzy matching.

---

## Data Models

```kotlin
@Serializable
data class DeduplicationResult(
    val totalBefore: Int,
    val totalAfter: Int,
    val duplicatesRemoved: Int,
    val clusters: List<DuplicateCluster>,
    val keptDocuments: List<Document>
)

@Serializable
data class DuplicateCluster(
    val clusterId: String,
    val representative: Document, // The one we keep
    val duplicates: List<Document>,
    val matchType: MatchType
)

@Serializable
enum class MatchType {
    DOI_EXACT,
    ARXIV_EXACT,
    TITLE_FUZZY_YEAR,
    TITLE_FUZZY_AUTHORS
}
```

---

## Algorithm

```
1. For each document:
   a. Check DOI exact match
   b. Check arXiv ID exact match
   c. Check title similarity (Levenshtein ≥ 97%) + year (± 1)
   d. If match found, add to cluster
2. Keep one representative per cluster
3. Return deduplicated list
```

**Conservative approach:** False negatives (keeping duplicates) are better than false positives (removing unique papers).

---

## Implementation

```kotlin
class DeduplicationStage : PipelineStage<List<Document>, DeduplicationResult> {
    
    companion object {
        const val TITLE_SIMILARITY_THRESHOLD = 0.97
    }
    
    override suspend fun execute(input: List<Document>): StageResult<DeduplicationResult> {
        val clusters = findDuplicates(input)
        val kept = clusters.map { it.representative }
        
        val result = DeduplicationResult(
            totalBefore = input.size,
            totalAfter = kept.size,
            duplicatesRemoved = input.size - kept.size,
            clusters = clusters,
            keptDocuments = kept
        )
        
        return StageResult.Success(result)
    }
    
    private fun findDuplicates(documents: List<Document>): List<DuplicateCluster> {
        val clusters = mutableListOf<DuplicateCluster>()
        val processed = mutableSetOf<String>()
        
        for (doc in documents) {
            if (doc.id in processed) continue
            
            val duplicates = documents.filter { other ->
                other.id != doc.id &&
                other.id !in processed &&
                areDuplicates(doc, other)
            }
            
            if (duplicates.isNotEmpty()) {
                val representative = selectRepresentative(doc, duplicates)
                
                clusters.add(DuplicateCluster(
                    clusterId = "cluster_${clusters.size + 1}",
                    representative = representative,
                    duplicates = (listOf(doc) + duplicates).filter { it != representative },
                    matchType = determineMatchType(doc, duplicates.first())
                ))
                
                processed.add(doc.id)
                duplicates.forEach { processed.add(it.id) }
            } else {
                // No duplicates, create single-member cluster
                clusters.add(DuplicateCluster(
                    clusterId = "cluster_${clusters.size + 1}",
                    representative = doc,
                    duplicates = emptyList(),
                    matchType = MatchType.DOI_EXACT // Dummy
                ))
                processed.add(doc.id)
            }
        }
        
        return clusters
    }
    
    private fun areDuplicates(a: Document, b: Document): Boolean {
        // 1. DOI exact match
        if (a.doi != null && b.doi != null && a.doi == b.doi) {
            return true
        }
        
        // 2. arXiv ID exact match
        if (a.externalIds.arxivId != null &&
            b.externalIds.arxivId != null &&
            a.externalIds.arxivId == b.externalIds.arxivId
        ) {
            return true
        }
        
        // 3. Title + year fuzzy match
        val titleSim = levenshteinSimilarity(
            normalize(a.title),
            normalize(b.title)
        )
        
        if (titleSim >= TITLE_SIMILARITY_THRESHOLD) {
            // Check year
            val yearMatch = a.year == null || b.year == null ||
                            kotlin.math.abs((a.year ?: 0) - (b.year ?: 0)) <= 1
            
            if (yearMatch) {
                return true
            }
        }
        
        return false
    }
    
    private fun normalize(title: String): String {
        return title
            .lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove punctuation
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }
    
    private fun levenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    private fun selectRepresentative(
        doc: Document,
        duplicates: List<Document>
    ): Document {
        // Prefer:
        // 1. Documents with DOI
        // 2. Documents with more complete metadata
        // 3. Documents from more authoritative sources
        
        val all = listOf(doc) + duplicates
        return all.maxByOrNull { 
            var score = 0
            if (it.doi != null) score += 10
            if (it.abstract != null) score += 5
            if (it.citationCount != null) score += 3
            if (it.venue != null) score += 2
            score
        } ?: doc
    }
    
    private fun determineMatchType(a: Document, b: Document): MatchType {
        return when {
            a.doi != null && a.doi == b.doi -> MatchType.DOI_EXACT
            a.externalIds.arxivId != null && 
                a.externalIds.arxivId == b.externalIds.arxivId -> MatchType.ARXIV_EXACT
            else -> MatchType.TITLE_FUZZY_YEAR
        }
    }
}
```

---

## Desktop UI

```kotlin
@Composable
fun DeduplicationScreen(viewModel: DeduplicationViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Deduplication", style = MaterialTheme.typography.headlineMedium)
        
        when (val state = viewModel.state) {
            is DeduplicationState.Running -> {
                LinearProgressIndicator()
                Text("Finding duplicates...")
            }
            
            is DeduplicationState.Completed -> {
                // Summary
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Before: ${state.result.totalBefore} documents")
                        Text("After: ${state.result.totalAfter} documents")
                        Text(
                            "Removed: ${state.result.duplicatesRemoved} duplicates",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Clusters with duplicates
                Text("Duplicate Clusters", style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(state.result.clusters.filter { it.duplicates.isNotEmpty() }) { cluster ->
                        DuplicateClusterCard(cluster)
                    }
                }
                
                Button(onClick = { viewModel.continueToScreening() }) {
                    Text("Continue to Screening →")
                }
            }
        }
    }
}
```

---

## CLI

```bash
# Run deduplication
lumen deduplicate --project <id>

# Output:
# Before: 776 documents
# Duplicates removed: 84
# After: 692 documents
# 
# Match types:
#   DOI exact: 45
#   arXiv exact: 12
#   Title fuzzy: 27
```

---

## Tests

```kotlin
class DeduplicationStageTest {
    
    @Test
    fun `removes exact DOI duplicates`() = runTest {
        val docs = listOf(
            Document(id = "1", title = "Paper A", doi = "10.1000/xyz", year = 2023),
            Document(id = "2", title = "Paper A (preprint)", doi = "10.1000/xyz", year = 2023)
        )
        
        val stage = DeduplicationStage()
        val result = stage.execute(docs)
        
        assertIs<StageResult.Success>(result)
        assertEquals(1, result.data.keptDocuments.size)
        assertEquals(1, result.data.duplicatesRemoved)
    }
    
    @Test
    fun `detects fuzzy title matches with year tolerance`() = runTest {
        val docs = listOf(
            Document(id = "1", title = "Machine Learning for Crops", year = 2023),
            Document(id = "2", title = "machine learning for crops", year = 2024) // Case + year
        )
        
        val stage = DeduplicationStage()
        val result = stage.execute(docs)
        
        assertIs<StageResult.Success>(result)
        assertEquals(1, result.data.keptDocuments.size)
    }
    
    @Test
    fun `keeps similar titles with different years`() = runTest {
        val docs = listOf(
            Document(id = "1", title = "Machine Learning for Crops", year = 2020),
            Document(id = "2", title = "Machine Learning for Crops", year = 2024) // Δyear > 1
        )
        
        val stage = DeduplicationStage()
        val result = stage.execute(docs)
        
        assertIs<StageResult.Success>(result)
        assertEquals(2, result.data.keptDocuments.size) // NOT duplicates
    }
}
```

---

## Next Stage

→ [Stage 10: Title/Abstract Screening](stage-10-title-abstract-screening.md)
