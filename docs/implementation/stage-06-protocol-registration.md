# Stage 6: Protocol Registration

**Purpose:** Generate PROSPERO/OSF-compatible protocol document for pre-registration.

---

## Data Models

```kotlin
@Serializable
data class ProtocolDocument(
    val metadata: ProtocolMetadata,
    val sections: ProtocolSections,
    val registrationInfo: RegistrationInfo? = null
)

@Serializable
data class ProtocolMetadata(
    val title: String,
    val authors: List<Author>,
    val version: String = "1.0",
    val generatedAt: Instant
)

@Serializable
data class ProtocolSections(
    val background: String,
    val objectives: String,
    val eligibilityCriteria: String,
    val informationSources: String,
    val searchStrategy: String,
    val studySelection: String,
    val dataExtraction: String,
    val riskOfBias: String,
    val synthesis: String
)

@Serializable
data class RegistrationInfo(
    val registry: Registry,
    val registrationId: String? = null,
    val registrationDate: Instant? = null,
    val registrationUrl: String? = null
)

@Serializable
enum class Registry {
    PROSPERO,
    OSF,
    INTERNAL
}
```

---

## Implementation

```kotlin
class ProtocolGenerationStage(
    private val context: ProjectContext,
    private val pico: ProblemFraming,
    private val questions: ResearchQuestions,
    private val queryPlan: DatabaseQueryPlan,
    private val criteria: ScreeningCriteria
) : PipelineStage<Unit, ProtocolDocument> {
    
    override suspend fun execute(input: Unit): StageResult<ProtocolDocument> {
        val protocol = ProtocolDocument(
            metadata = ProtocolMetadata(
                title = context.title ?: "Systematic Review: ${context.rawIdea}",
                authors = context.authors,
                version = "1.0",
                generatedAt = Clock.System.now()
            ),
            sections = ProtocolSections(
                background = generateBackground(),
                objectives = generateObjectives(),
                eligibilityCriteria = generateCriteria(),
                informationSources = generateSources(),
                searchStrategy = generateSearchStrategy(),
                studySelection = generateSelectionProcess(),
                dataExtraction = "To be defined during review",
                riskOfBias = "To be assessed using appropriate tools",
                synthesis = "Narrative synthesis planned"
            ),
            registrationInfo = null
        )
        
        return StageResult.Success(protocol)
    }
    
    private fun generateBackground(): String {
        return """
        ## Background
        
        This systematic review aims to synthesize evidence on ${pico.intervention} 
        for ${pico.population}, with a focus on ${pico.outcome}.
        
        **Rationale:** ${context.rawIdea}
        """.trimIndent()
    }
    
    private fun generateObjectives(): String {
        return """
        ## Objectives
        
        **Primary objective:** ${questions.primary.text}
        
        **Secondary objectives:**
        ${questions.secondary.joinToString("\n") { "- ${it.text}" }}
        """.trimIndent()
    }
    
    private fun generateCriteria(): String {
        return """
        ## Eligibility Criteria
        
        ### Inclusion Criteria
        ${criteria.inclusionCriteria.joinToString("\n") { "- ${it.description}" }}
        
        ### Exclusion Criteria
        ${criteria.exclusionCriteria.joinToString("\n") { "- ${it.description}" }}
        """.trimIndent()
    }
    
    private fun generateSources(): String {
        val databases = queryPlan.queries.keys.joinToString(", ")
        return """
        ## Information Sources
        
        The following databases will be searched:
        - $databases
        
        Additional sources:
        - Forward and backward citation chaining
        - Grey literature (if applicable)
        """.trimIndent()
    }
    
    private fun generateSearchStrategy(): String {
        return """
        ## Search Strategy
        
        ${queryPlan.queries.entries.joinToString("\n\n") { (db, query) ->
            """
            ### $db
            ```
            ${query.queryText}
            ```
            """.trimIndent()
        }}
        """.trimIndent()
    }
    
    private fun generateSelectionProcess(): String {
        return """
        ## Study Selection Process
        
        1. **Title/Abstract Screening:** Two independent reviewers
        2. **Full-Text Assessment:** Two independent reviewers
        3. **Conflict Resolution:** Third reviewer or consensus discussion
        4. **Documentation:** PRISMA flow diagram
        """.trimIndent()
    }
}

// Export to Markdown
fun ProtocolDocument.toMarkdown(): String {
    return """
    # ${metadata.title}
    
    **Version:** ${metadata.version}  
    **Generated:** ${metadata.generatedAt}
    
    **Authors:** ${metadata.authors.joinToString(", ") { it.fullName }}
    
    ---
    
    ${sections.background}
    
    ${sections.objectives}
    
    ${sections.eligibilityCriteria}
    
    ${sections.informationSources}
    
    ${sections.searchStrategy}
    
    ${sections.studySelection}
    
    ${sections.dataExtraction}
    
    ${sections.riskOfBias}
    
    ${sections.synthesis}
    """.trimIndent()
}
```

---

## Export Formats

1. **Markdown** → Human-readable
2. **PDF** → For submission (via markdown → PDF converter)
3. **DOCX** (future) → MS Word format
4. **XML** (future) → Direct PROSPERO upload

---

## CLI

```bash
# Generate protocol
lumen protocol --project <id> --format markdown

# Output: data/<project_id>/export/PROTOCOL.md

# Generate PDF
lumen protocol --project <id> --format pdf
```

---

## Next Stage

→ [Stage 7: Search Execution](stage-07-search-execution.md)
