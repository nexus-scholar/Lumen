# Lumen Implementation Guides

This folder contains detailed technical implementation guides for each of the 15 pipeline stages.

## How to Use These Guides

Each stage guide includes:
- **Data Models:** Kotlin code for input/output types
- **Algorithm:** Step-by-step logic flow
- **LLM Integration:** Prompts and structured output schemas (where applicable)
- **API Calls:** External service integration details
- **UI Mockups:** Desktop/CLI interface designs
- **Testing:** Unit and integration test examples
- **Edge Cases:** Error handling and validation

## Stage Index

### Identification Phase
- [Stage 0: Project Setup](stage-00-project-setup.md)
- [Stage 1: PICO Extraction](stage-01-pico-extraction.md)
- [Stage 2: Research Questions](stage-02-research-questions.md)
- [Stage 3: Concept Expansion](stage-03-concept-expansion.md)
- [Stage 4: Query Generation](stage-04-query-generation.md)
- [Stage 4.5: Test & Refine Protocol](stage-04.5-test-refine.md) ⭐
- [Stage 5: Screening Criteria](stage-05-screening-criteria.md)
- [Stage 6: Protocol Registration](stage-06-protocol-registration.md)

### Search & Retrieval
- [Stage 7: Search Execution](stage-07-search-execution.md)
- [Stage 8: Citation Expansion](stage-08-citation-expansion.md)
- [Stage 9: Deduplication](stage-09-deduplication.md)

### Screening
- [Stage 10: Title/Abstract Screening](stage-10-title-abstract-screening.md)
- [Stage 11: Full-Text Screening](stage-11-fulltext-screening.md)

### Data Extraction & Assessment
- [Stage 12: Data Extraction](stage-12-data-extraction.md)
- [Stage 13: Risk of Bias Assessment](stage-13-risk-of-bias.md)

### Synthesis & Reporting
- [Stage 14: Synthesis & Analytics](stage-14-synthesis-analytics.md) ⭐
- [Stage 15: Export & PRISMA Reporting](stage-15-export-reporting.md)

## Implementation Order

For MVP (Months 1-6), implement in this order:
1. Stage 0 (Project Setup)
2. Stage 1 (PICO - can start with manual input)
3. Stage 4 (Query Generation - deterministic templates first)
4. Stage 7 (Search Execution - OpenAlex only initially)
5. Stage 4.5 (Test & Refine - core differentiator)
6. Stage 9 (Deduplication - basic DOI matching)

For Phase 2 (Months 7-12), add:
7. Stage 8 (Citation Expansion)
8. Stage 10 (Title/Abstract Screening)
9. Stage 15 (Basic exports)

## Common Patterns

### Pipeline Stage Interface

All stages follow this pattern:

```kotlin
interface PipelineStage<I, O> {
    suspend fun execute(input: I): StageResult<O>
}

sealed class StageResult<out T> {
    data class Success<T>(val data: T) : StageResult<T>()
    data class Failure(val error: PipelineError) : StageResult<Nothing>()
    data class RequiresApproval<T>(
        val data: T,
        val reason: String
    ) : StageResult<T>()
}
```

### Persistence Pattern

```kotlin
class ArtifactStore {
    fun <T> save(projectId: String, artifact: T, serializer: KSerializer<T>) {
        val json = Json.encodeToString(serializer, artifact)
        val path = "data/$projectId/artifacts/${artifact::class.simpleName}.json"
        File(path).writeText(json)
    }
    
    fun <T> load(projectId: String, type: KClass<T>, serializer: KSerializer<T>): T {
        val path = "data/$projectId/artifacts/${type.simpleName}.json"
        val json = File(path).readText()
        return Json.decodeFromString(serializer, json)
    }
}
```

### LLM Integration Pattern

```kotlin
class LlmService(private val apiKey: String) {
    suspend fun <T> generateStructured(
        prompt: String,
        schema: KSerializer<T>,
        temperature: Double = 0.0
    ): T {
        val response = client.post("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            setBody(json {
                "model" to "gpt-4"
                "messages" to arrayOf(
                    mapOf("role" to "system", "content" to "You are a research assistant."),
                    mapOf("role" to "user", "content" to prompt)
                )
                "temperature" to temperature
            })
        }
        return Json.decodeFromString(schema, response.choices[0].message.content)
    }
}
```

## Testing Strategy

Each stage should have:

1. **Unit Tests** (70%+ coverage):
   - Test pure functions (validation, parsing, transformation)
   - Mock external dependencies (LLM, APIs)

2. **Integration Tests** (key paths):
   - Test stage with real API calls (use test data)
   - Verify artifact persistence

3. **End-to-End Tests** (critical paths):
   - Test full pipeline (Stages 0-7)
   - Verify PRISMA compliance

Example test:

```kotlin
class Stage4Test {
    @Test
    fun `generate PubMed query from PICO`() = runTest {
        val pico = ProblemFraming(
            population = "crops with fungal diseases",
            intervention = "machine learning detection",
            outcome = "diagnostic accuracy"
        )
        
        val stage = QueryGenerationStage(mockLlmService, queryValidator)
        val result = stage.execute(pico)
        
        assertIs<StageResult.Success>(result)
        val plan = result.data
        
        assertTrue(plan.queries.containsKey("pubmed"))
        val pubmedQuery = plan.queries["pubmed"]!!
        assertTrue(pubmedQuery.queryText.contains("[Title/Abstract]"))
        assertFalse(pubmedQuery.queryText.contains("NEAR")) // No hallucinations
    }
}
```

## Contributing

When implementing a stage:

1. Read the stage guide thoroughly
2. Implement data models first
3. Write tests alongside implementation
4. Update the guide if you discover edge cases
5. Document any deviations from the spec

---

**Ready to implement? Start with [Stage 0: Project Setup](stage-00-project-setup.md)!**
