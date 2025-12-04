# Task 04: Implement Pipeline Stage

**Status:** ⬜ Not Started  
**Priority:** Critical  
**Estimated Effort:** 2.5 hours  
**Dependencies:** Task 01 (Data Models), Task 02 (Validation Logic)  
**Assignee:** _____

---

## Objective

Implement `ResearchQuestionsStage` class that generates research questions from PICO framework using LLM, validates them, and integrates with the existing pipeline architecture.

---

## Files to Create

### 1. ResearchQuestionsStage.kt
**Path:** `src/commonMain/kotlin/com/lumen/core/domain/stages/ResearchQuestionsStage.kt`

---

## Implementation

### Complete ResearchQuestionsStage.kt

```kotlin
package com.lumen.core.domain.stages

import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.domain.validation.ResearchQuestionsValidator
import com.lumen.core.services.llm.LlmService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/**
 * Stage 2: Research Questions Generation
 * Generates primary and secondary research questions from approved PICO framework
 */
class ResearchQuestionsStage(
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<ProblemFraming, ResearchQuestions> {

    override val stageName: String = "Stage 2: Research Questions Generation"

    companion object {
        private val RESEARCH_QUESTIONS_PROMPT = """
You are a systematic review methodology expert. Generate focused research questions from the PICO framework.

PICO Framework:
- **Population**: {{POPULATION}}
- **Intervention**: {{INTERVENTION}}
- **Comparison**: {{COMPARISON}}
- **Outcome**: {{OUTCOME}}
- **Study Designs**: {{STUDY_DESIGNS}}
- **Context**: {{CONTEXT}}

Generate:
1. **One primary research question** - The main question this systematic review will answer
   - Must be specific, measurable, and directly answerable through meta-analysis
   - Should clearly incorporate all PICO elements
   - Should be appropriate for the study design

2. **2-4 secondary research questions** - Supporting questions that explore:
   - Subgroup analyses (e.g., by age, severity, duration)
   - Moderating factors
   - Mechanisms or pathways
   - Comparative effectiveness across interventions
   - Different outcome measures

Guidelines:
- Use clear, answerable language following PICO structure
- Each question should end with "?"
- Be specific about population, intervention, and outcome
- Secondary questions should complement, not duplicate, the primary
- Include rationale explaining why each question matters

Return ONLY valid JSON matching this exact schema:
{
  "primary": {
    "text": "Clear, specific primary research question?",
    "rationale": "Why this is the most important question to answer"
  },
  "secondary": [
    {
      "text": "First secondary question exploring a specific aspect?",
      "rationale": "Why this secondary question adds value"
    },
    {
      "text": "Second secondary question?",
      "rationale": "Its importance"
    }
  ]
}

IMPORTANT: 
- Primary question must incorporate population, intervention, and outcome explicitly
- Generate 2-4 secondary questions (not fewer, not more)
- Each rationale should be 1-2 sentences explaining the question's value
- Questions should be suitable for {{REVIEW_TYPE}} systematic review
        """.trimIndent()
    }

    override suspend fun execute(input: ProblemFraming): StageResult<ResearchQuestions> {
        logger.info { "Executing $stageName for problem framing" }

        // Precondition: PICO must be approved
        if (!input.approved) {
            logger.warn { "PICO not approved, cannot generate research questions" }
            return StageResult.Failure(
                PipelineError.PreconditionFailed(
                    "PICO framework must be approved before generating research questions. " +
                    "Please review and approve the PICO extraction first."
                )
            )
        }

        // Check if LLM is available
        if (!llmService.isAvailable()) {
            logger.warn { "LLM service not available, requiring manual input" }
            return StageResult.RequiresApproval(
                data = createEmptyQuestions(input),
                reason = "LLM service unavailable. Please manually enter research questions.",
                suggestions = listOf(
                    "Check API key configuration (OpenAI or OpenRouter)",
                    "Verify internet connection",
                    "Enter research questions manually using the form"
                )
            )
        }

        return try {
            // Generate questions using LLM
            val questions = generateQuestions(input)

            // Validate generated questions
            val validation = ResearchQuestionsValidator.validate(questions)
            
            if (!validation.isValid) {
                logger.warn { "Generated questions failed validation: ${validation.errors}" }
                
                // If validation fails critically, return failure
                return StageResult.Failure(
                    PipelineError.ValidationFailed(
                        "Generated research questions failed quality validation",
                        validation.errors
                    )
                )
            }

            // Save artifact
            artifactStore.save(
                projectId = getProjectId(input),
                artifact = questions,
                serializer = ResearchQuestions.serializer(),
                filename = "ResearchQuestions.json"
            )

            logger.info { "Research questions generated successfully with ${validation.warnings.size} warnings" }

            // Return for human approval (even if valid)
            StageResult.RequiresApproval(
                data = questions,
                reason = "Please review and approve the generated research questions",
                suggestions = validation.warnings
            )

        } catch (e: Exception) {
            logger.error(e) { "Research questions generation failed" }

            // Save error for debugging
            artifactStore.saveError(
                projectId = getProjectId(input),
                stageName = stageName,
                error = PipelineError.LlmCallFailed(e.message ?: "Unknown error", e),
                context = mapOf(
                    "population" to input.population,
                    "intervention" to input.intervention,
                    "outcome" to input.outcome
                )
            )

            StageResult.Failure(
                PipelineError.LlmCallFailed(
                    "Research questions generation failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Generates research questions using LLM
     */
    private suspend fun generateQuestions(pico: ProblemFraming): ResearchQuestions {
        logger.debug { "Generating research questions from PICO" }

        // Build prompt
        val prompt = RESEARCH_QUESTIONS_PROMPT
            .replace("{{POPULATION}}", pico.population)
            .replace("{{INTERVENTION}}", pico.intervention)
            .replace("{{COMPARISON}}", pico.comparison ?: "None specified")
            .replace("{{OUTCOME}}", pico.outcome)
            .replace("{{STUDY_DESIGNS}}", pico.studyDesigns.joinToString(", "))
            .replace("{{CONTEXT}}", pico.context ?: "General")
            .replace("{{REVIEW_TYPE}}", "intervention") // TODO: Get from project

        // Call LLM with structured output
        val response = llmService.generateStructured<QuestionsResponse>(
            prompt = prompt,
            schema = QuestionsResponse.serializer(),
            temperature = 0.3 // Low temp for focused, consistent questions
        )

        // Convert to domain model
        val picoMapping = PicoMapping(
            population = pico.population,
            intervention = pico.intervention,
            comparison = pico.comparison,
            outcome = pico.outcome
        )

        return ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = response.primary.text.trim(),
                type = QuestionType.PRIMARY,
                rationale = response.primary.rationale.trim(),
                picoMapping = picoMapping
            ),
            secondary = response.secondary.mapIndexed { index, dto ->
                ResearchQuestion(
                    id = "secondary_${index + 1}",
                    text = dto.text.trim(),
                    type = QuestionType.SECONDARY,
                    rationale = dto.rationale.trim(),
                    picoMapping = picoMapping
                )
            },
            approved = false,
            llmModel = llmService.modelName,
            prompt = prompt,
            rawOutput = kotlinx.serialization.json.Json.encodeToString(
                QuestionsResponse.serializer(),
                response
            ),
            generatedAt = Clock.System.now()
        )
    }

    /**
     * Creates empty questions structure for manual entry
     */
    private fun createEmptyQuestions(pico: ProblemFraming): ResearchQuestions {
        val picoMapping = PicoMapping(
            population = pico.population,
            intervention = pico.intervention,
            comparison = pico.comparison,
            outcome = pico.outcome
        )

        return ResearchQuestions(
            primary = ResearchQuestion(
                id = "primary_1",
                text = "",
                type = QuestionType.PRIMARY,
                rationale = null,
                picoMapping = picoMapping
            ),
            secondary = emptyList(),
            approved = false,
            generatedAt = Clock.System.now()
        )
    }

    /**
     * Extract project ID from PICO (temporary until ProblemFraming includes it)
     */
    private fun getProjectId(pico: ProblemFraming): String {
        // TODO: ProblemFraming should include projectId
        // For now, we need to get it from the calling context
        // This is a known limitation from the status report
        return "unknown_project"
    }
}
```

---

## Implementation Checklist

### Pre-Implementation
- [ ] Review `PicoExtractionStage.kt` as reference
- [ ] Review `PipelineStage` interface
- [ ] Understand `LlmService` API
- [ ] Understand `ArtifactStore` API
- [ ] Review error handling patterns

### Implementation Steps
- [ ] Create `ResearchQuestionsStage.kt` file
- [ ] Implement class structure extending `PipelineStage`
- [ ] Implement `execute()` method
- [ ] Add precondition check (PICO approved)
- [ ] Add LLM availability check
- [ ] Implement `generateQuestions()` method
- [ ] Create LLM prompt with variable substitution
- [ ] Implement response parsing
- [ ] Add validation integration
- [ ] Implement artifact storage
- [ ] Add error handling and logging
- [ ] Implement `createEmptyQuestions()` fallback
- [ ] Add comprehensive KDoc comments

### Post-Implementation
- [ ] Run `./gradlew build` to verify compilation
- [ ] Check no compiler warnings
- [ ] Verify imports are correct
- [ ] Test with mock LLM service
- [ ] Test error cases

---

## Acceptance Criteria

### Functional Requirements
✅ **AC1:** Implements `PipelineStage<ProblemFraming, ResearchQuestions>`  
✅ **AC2:** Checks PICO is approved before proceeding  
✅ **AC3:** Generates questions using LLM with structured output  
✅ **AC4:** Validates generated questions using validator  
✅ **AC5:** Saves questions to artifact store  
✅ **AC6:** Returns `RequiresApproval` for human review  
✅ **AC7:** Handles LLM unavailability gracefully  
✅ **AC8:** Handles generation failures with clear errors  
✅ **AC9:** Logs all operations for debugging  
✅ **AC10:** Stores LLM model, prompt, and raw output  

### Non-Functional Requirements
✅ **AC11:** Follows existing stage pattern consistently  
✅ **AC12:** No hardcoded values (use constants)  
✅ **AC13:** Temperature set appropriately (0.3 for consistency)  
✅ **AC14:** Comprehensive error messages  
✅ **AC15:** Thread-safe (no mutable state)  

---

## Unit Tests

### Create ResearchQuestionsStageTest.kt
**Path:** `src/jvmTest/kotlin/com/lumen/core/domain/stages/ResearchQuestionsStageTest.kt`

```kotlin
package com.lumen.core.domain.stages

import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.services.llm.LlmService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class ResearchQuestionsStageTest {

    private lateinit var llmService: LlmService
    private lateinit var artifactStore: ArtifactStore
    private lateinit var stage: ResearchQuestionsStage

    private val testPico = ProblemFraming(
        population = "Patients with type 2 diabetes",
        intervention = "Metformin",
        comparison = "Placebo",
        outcome = "Blood glucose levels",
        studyDesigns = listOf("RCT", "Cohort"),
        approved = true,
        extractedAt = Clock.System.now()
    )

    @BeforeEach
    fun setup() {
        llmService = mockk()
        artifactStore = mockk(relaxed = true)
        stage = ResearchQuestionsStage(llmService, artifactStore)
    }

    @Test
    fun `rejects unapproved PICO`() = runTest {
        val unapprovedPico = testPico.copy(approved = false)

        val result = stage.execute(unapprovedPico)

        assertTrue(result is StageResult.Failure)
        val failure = result as StageResult.Failure
        assertTrue(failure.error.message.contains("approved"))
    }

    @Test
    fun `handles LLM unavailable`() = runTest {
        every { llmService.isAvailable() } returns false

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.RequiresApproval)
        val approval = result as StageResult.RequiresApproval
        assertTrue(approval.reason.contains("unavailable"))
    }

    @Test
    fun `generates valid research questions`() = runTest {
        every { llmService.isAvailable() } returns true
        every { llmService.modelName } returns "gpt-4"
        
        val mockResponse = QuestionsResponse(
            primary = QuestionDto(
                text = "What is the effect of metformin on blood glucose levels in patients with type 2 diabetes?",
                rationale = "Primary outcome of interest"
            ),
            secondary = listOf(
                QuestionDto(
                    text = "How does the effect vary by patient age?",
                    rationale = "Explores age as moderator"
                ),
                QuestionDto(
                    text = "What is the effect on HbA1c levels?",
                    rationale = "Secondary outcome measure"
                )
            )
        )

        coEvery {
            llmService.generateStructured<QuestionsResponse>(
                any(), any(), any()
            )
        } returns mockResponse

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.RequiresApproval)
        val approval = result as StageResult.RequiresApproval<ResearchQuestions>
        
        assertEquals("primary_1", approval.data.primary.id)
        assertEquals(QuestionType.PRIMARY, approval.data.primary.type)
        assertEquals(2, approval.data.secondary.size)
        assertFalse(approval.data.approved)
        
        // Verify artifact saved
        verify {
            artifactStore.save(
                projectId = any(),
                artifact = any<ResearchQuestions>(),
                serializer = any<KSerializer<ResearchQuestions>>(),
                filename = "ResearchQuestions.json"
            )
        }
    }

    @Test
    fun `handles LLM generation failure`() = runTest {
        every { llmService.isAvailable() } returns true
        
        coEvery {
            llmService.generateStructured<QuestionsResponse>(any(), any(), any())
        } throws Exception("API error")

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.Failure)
        
        // Verify error saved
        verify {
            artifactStore.saveError(
                projectId = any(),
                stageName = any(),
                error = any(),
                context = any()
            )
        }
    }

    @Test
    fun `validates generated questions`() = runTest {
        every { llmService.isAvailable() } returns true
        every { llmService.modelName } returns "gpt-4"
        
        // Generate questions with validation issues
        val mockResponse = QuestionsResponse(
            primary = QuestionDto(
                text = "Short?", // Too short
                rationale = "Test"
            ),
            secondary = emptyList()
        )

        coEvery {
            llmService.generateStructured<QuestionsResponse>(any(), any(), any())
        } returns mockResponse

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.Failure)
        val failure = result as StageResult.Failure
        assertTrue(failure.error.message.contains("validation"))
    }
}
```

---

## Verification Steps

### Step 1: Compilation
```bash
./gradlew :compileKotlinJvm
```
**Expected:** No errors

### Step 2: Run Unit Tests
```bash
./gradlew test --tests ResearchQuestionsStageTest
```
**Expected:** All tests pass

### Step 3: Integration Test with Real LLM
Create manual test:
```kotlin
fun main() = runBlocking {
    val config = ConfigLoader.load()
    val httpClient = HttpClient(CIO)
    val llmService = OpenAiLlmService(
        apiKey = config.llm.openai.apiKey,
        model = "gpt-4-turbo",
        httpClient = httpClient
    )
    val artifactStore = FileArtifactStore(...)
    
    val stage = ResearchQuestionsStage(llmService, artifactStore)
    
    val pico = ProblemFraming(...)
    val result = stage.execute(pico)
    
    when (result) {
        is StageResult.RequiresApproval -> {
            println("Generated questions:")
            println("Primary: ${result.data.primary.text}")
            result.data.secondary.forEach {
                println("Secondary: ${it.text}")
            }
        }
        is StageResult.Failure -> {
            println("Failed: ${result.error.message}")
        }
    }
}
```

---

## Known Issues & Workarounds

### Issue 1: Project ID Not Available
**Problem:** `ProblemFraming` doesn't contain `projectId`  
**Workaround:** Return "unknown_project" temporarily  
**Proper Fix:** Add `projectId` to `ProblemFraming` (refactoring task)

### Issue 2: Review Type Not Available
**Problem:** Prompt uses hardcoded "intervention" review type  
**Workaround:** Use default "intervention"  
**Proper Fix:** Pass project context to stage execution

---

## Integration Points

### Uses
- Task 01: ResearchQuestions data models
- Task 02: ResearchQuestionsValidator
- Existing: LlmService
- Existing: ArtifactStore
- Existing: ProblemFraming

### Used By
- Task 06: DI Registration
- Task 07: UI Stage Card
- Task 09: Integration Tests

---

## Definition of Done

- [ ] ResearchQuestionsStage.kt created and compiles
- [ ] Implements all required methods
- [ ] Precondition checks implemented
- [ ] LLM integration working
- [ ] Validation integrated
- [ ] Artifact storage working
- [ ] Error handling comprehensive
- [ ] Unit tests written (>80% coverage)
- [ ] All tests passing
- [ ] KDoc comments added
- [ ] Follows existing patterns
- [ ] Code reviewed

---

## Time Tracking

| Activity | Estimated | Actual | Notes |
|----------|-----------|--------|-------|
| Planning & Reading | 30 min | | |
| Implementation | 60 min | | |
| Unit Tests | 45 min | | |
| Testing & Debugging | 15 min | | |
| **Total** | **150 min** | | |

---

**Next Task:** ➡️ Task 05: Implement Artifact Storage

---

**Task Created:** December 4, 2025  
**Last Updated:** December 4, 2025  
**Version:** 1.0

