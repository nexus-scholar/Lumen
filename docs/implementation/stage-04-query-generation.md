# Stage 4: Database Query Generation

**Purpose:** Generate database-specific Boolean queries with anti-hallucination validation.

## Data Models

```kotlin
@Serializable
data class DatabaseQueryPlan(
    val queries: Map<String, DatabaseQuery>,
    val generationMethod: GenerationMethod,
    val validated: Boolean = false,
    val approved: Boolean = false
)

@Serializable
data class DatabaseQuery(
    val database: String,
    val queryText: String,
    val filters: Map<String, String> = emptyMap(),
    val translationNotes: String? = null,
    val validationStatus: ValidationStatus
)

@Serializable
enum class GenerationMethod {
    LLM_GENERATED,
    TEMPLATE_BASED,
    HYBRID
}

@Serializable
enum class ValidationStatus {
    VALID,
    INVALID,
    WARNING
}
```

## Anti-Hallucination Validator

```kotlin
class QueryValidator {
    
    private val databaseSpecs = mapOf(
        "pubmed" to DatabaseSpec(
            allowedOperators = setOf("AND", "OR", "NOT"),
            allowedFields = setOf("[Title/Abstract]", "[MeSH Terms]", "[Author]"),
            forbiddenOperators = setOf("NEAR", "ADJ", "PROX", "W/")
        ),
        "scopus" to DatabaseSpec(
            allowedOperators = setOf("AND", "OR", "AND NOT", "W/", "PRE/"),
            allowedFields = setOf("TITLE-ABS-KEY", "AUTH", "AFFIL"),
            forbiddenOperators = emptySet()
        ),
        "openalex" to DatabaseSpec(
            allowedOperators = setOf("AND", "OR", "NOT"),
            allowedFields = setOf("title", "abstract", "fulltext"),
            forbiddenOperators = setOf("NEAR", "ADJ", "W/")
        )
    )
    
    fun validate(query: String, database: String): QueryValidationResult {
        val spec = databaseSpecs[database]
            ?: return QueryValidationResult.invalid("Unknown database: $database")
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check for hallucinated operators
        spec.forbiddenOperators.forEach { op ->
            if (query.contains(op, ignoreCase = true)) {
                errors.add("Operator '$op' is not supported by $database")
            }
        }
        
        // Check for valid field tags
        val fieldPattern = Regex("\\[([^]]+)\\]")
        fieldPattern.findAll(query).forEach { match ->
            val field = match.value
            if (field !in spec.allowedFields) {
                warnings.add("Field tag '$field' may not be valid for $database")
            }
        }
        
        // Check query length
        if (query.length > 4000) {
            warnings.add("Query is very long (${query.length} chars), may hit API limits")
        }
        
        return when {
            errors.isNotEmpty() -> QueryValidationResult.invalid(errors.joinToString("; "))
            warnings.isNotEmpty() -> QueryValidationResult.warning(warnings.joinToString("; "))
            else -> QueryValidationResult.valid()
        }
    }
}

data class DatabaseSpec(
    val allowedOperators: Set<String>,
    val allowedFields: Set<String>,
    val forbiddenOperators: Set<String>
)

sealed class QueryValidationResult {
    data class Valid(val message: String = "Query is valid") : QueryValidationResult()
    data class Warning(val message: String) : QueryValidationResult()
    data class Invalid(val message: String) : QueryValidationResult()
    
    companion object {
        fun valid() = Valid()
        fun warning(msg: String) = Warning(msg)
        fun invalid(msg: String) = Invalid(msg)
    }
}
```

## Implementation

```kotlin
class QueryGenerationStage(
    private val llmService: LlmService,
    private val validator: QueryValidator
) : PipelineStage<ConceptExpansion, DatabaseQueryPlan> {
    
    override suspend fun execute(input: ConceptExpansion): StageResult<DatabaseQueryPlan> {
        val databases = listOf("pubmed", "openalex", "crossref", "arxiv")
        val queries = mutableMapOf<String, DatabaseQuery>()
        
        for (db in databases) {
            // Try LLM generation first
            val llmQuery = try {
                generateQueryWithLlm(db, input)
            } catch (e: Exception) {
                null
            }
            
            // Validate LLM output
            val query = if (llmQuery != null) {
                val validation = validator.validate(llmQuery, db)
                when (validation) {
                    is QueryValidationResult.Valid -> {
                        DatabaseQuery(
                            database = db,
                            queryText = llmQuery,
                            validationStatus = ValidationStatus.VALID
                        )
                    }
                    is QueryValidationResult.Warning -> {
                        DatabaseQuery(
                            database = db,
                            queryText = llmQuery,
                            validationStatus = ValidationStatus.WARNING,
                            translationNotes = validation.message
                        )
                    }
                    is QueryValidationResult.Invalid -> {
                        // Fall back to template
                        generateQueryWithTemplate(db, input)
                    }
                }
            } else {
                // Fall back to template
                generateQueryWithTemplate(db, input)
            }
            
            queries[db] = query
        }
        
        val plan = DatabaseQueryPlan(
            queries = queries,
            generationMethod = GenerationMethod.HYBRID,
            validated = true,
            approved = false
        )
        
        return StageResult.RequiresApproval(
            data = plan,
            reason = "Review generated queries before test search"
        )
    }
    
    private suspend fun generateQueryWithLlm(
        database: String,
        concepts: ConceptExpansion
    ): String {
        // LLM prompt for query generation
        val prompt = buildQueryPrompt(database, concepts)
        return llmService.generate(prompt)
    }
    
    private fun generateQueryWithTemplate(
        database: String,
        concepts: ConceptExpansion
    ): DatabaseQuery {
        // Deterministic template-based query
        val query = when (database) {
            "pubmed" -> buildPubMedQuery(concepts)
            "openalex" -> buildOpenAlexQuery(concepts)
            else -> buildGenericQuery(concepts)
        }
        
        return DatabaseQuery(
            database = database,
            queryText = query,
            validationStatus = ValidationStatus.VALID,
            translationNotes = "Generated from template (LLM validation failed)"
        )
    }
    
    private fun buildPubMedQuery(concepts: ConceptExpansion): String {
        val popTerms = concepts.populationBlock.synonyms.joinToString(" OR ") {
            "\"$it\"[Title/Abstract]"
        }
        val intTerms = concepts.interventionBlock.synonyms.joinToString(" OR ") {
            "\"$it\"[Title/Abstract]"
        }
        val outTerms = concepts.outcomeBlock.synonyms.joinToString(" OR ") {
            "\"$it\"[Title/Abstract]"
        }
        
        return "($popTerms) AND ($intTerms) AND ($outTerms)"
    }
}
```

## Tests

```kotlin
class QueryValidatorTest {
    
    @Test
    fun `detects hallucinated NEAR operator in PubMed query`() {
        val validator = QueryValidator()
        val query = "machine NEAR/3 learning AND crops"
        
        val result = validator.validate(query, "pubmed")
        
        assertIs<QueryValidationResult.Invalid>(result)
        assertTrue(result.message.contains("NEAR"))
    }
    
    @Test
    fun `accepts valid PubMed query`() {
        val validator = QueryValidator()
        val query = "(machine learning[Title/Abstract]) AND (crops[Title/Abstract])"
        
        val result = validator.validate(query, "pubmed")
        
        assertIs<QueryValidationResult.Valid>(result)
    }
}
```

## Next Stage

→ [Stage 4.5: Test & Refine](stage-04.5-test-refine.md)
