# Stage 3: Concept Expansion

**Purpose:** Expand PICO terms into searchable synonyms and related concepts.

## Data Models

```kotlin
@Serializable
data class ConceptExpansion(
    val populationBlock: ConceptBlock,
    val interventionBlock: ConceptBlock,
    val outcomeBlock: ConceptBlock,
    val comparisonBlock: ConceptBlock? = null,
    val approved: Boolean = false
)

@Serializable
data class ConceptBlock(
    val coreTerm: String,
    val synonyms: List<String>,
    val relatedTerms: List<String>,
    val meshTerms: List<String> = emptyList(),
    val exclusionTerms: List<String> = emptyList()
)
```

## Implementation

```kotlin
class ConceptExpansionStage(
    private val llmService: LlmService,
    private val meshService: MeshService
) : PipelineStage<ProblemFraming, ConceptExpansion> {
    
    override suspend fun execute(input: ProblemFraming): StageResult<ConceptExpansion> {
        val populationBlock = expandConcept(input.population, "population")
        val interventionBlock = expandConcept(input.intervention, "intervention")
        val outcomeBlock = expandConcept(input.outcome, "outcome")
        val comparisonBlock = input.comparison?.let { expandConcept(it, "comparison") }
        
        val expansion = ConceptExpansion(
            populationBlock = populationBlock,
            interventionBlock = interventionBlock,
            outcomeBlock = outcomeBlock,
            comparisonBlock = comparisonBlock,
            approved = false
        )
        
        return StageResult.RequiresApproval(
            data = expansion,
            reason = "Review expanded terms"
        )
    }
    
    private suspend fun expandConcept(term: String, type: String): ConceptBlock {
        // LLM-based synonym generation
        val synonyms = llmService.generateSynonyms(term)
        
        // MeSH term lookup
        val meshTerms = meshService.searchMeshTerms(term)
        
        return ConceptBlock(
            coreTerm = term,
            synonyms = synonyms,
            relatedTerms = emptyList(),
            meshTerms = meshTerms
        )
    }
}
```

See full implementation in repo.

## Next Stage

→ [Stage 4: Query Generation](stage-04-query-generation.md)
