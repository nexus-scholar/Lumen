# Stage 2: Research Questions Generation

**Purpose:** Generate primary and secondary research questions from PICO framework.

## Data Models

```kotlin
@Serializable
data class ResearchQuestions(
    val primary: ResearchQuestion,
    val secondary: List<ResearchQuestion>,
    val approved: Boolean = false
)

@Serializable
data class ResearchQuestion(
    val id: String,
    val text: String,
    val type: QuestionType,
    val rationale: String? = null,
    val picoMapping: PicoMapping
)

@Serializable
enum class QuestionType {
    PRIMARY,
    SECONDARY,
    EXPLORATORY
}

@Serializable
data class PicoMapping(
    val population: String,
    val intervention: String,
    val comparison: String? = null,
    val outcome: String
)
```

## LLM Prompt

```kotlin
val RESEARCH_QUESTIONS_PROMPT = """
You are a systematic review methodology expert. Generate research questions from PICO.

PICO Framework:
- Population: {{POPULATION}}
- Intervention: {{INTERVENTION}}
- Comparison: {{COMPARISON}}
- Outcome: {{OUTCOME}}

Generate:
1. **One primary research question** - The main question the review answers
2. **2-4 secondary questions** - Sub-questions that support the primary

Guidelines:
- Use clear, answerable language
- Follow PICO structure
- Be specific about outcomes
- Secondary questions should explore moderators, subgroups, or mechanisms

Return JSON:
{
  "primary": {
    "text": string,
    "rationale": string
  },
  "secondary": [
    {
      "text": string,
      "rationale": string
    }
  ]
}
""".trimIndent()
```

## Implementation

```kotlin
class ResearchQuestionsStage(
    private val llmService: LlmService
) : PipelineStage<ProblemFraming, ResearchQuestions> {
    
    override suspend fun execute(input: ProblemFraming): StageResult<ResearchQuestions> {
        if (!input.approved) {
            return StageResult.Failure(
                PipelineError.PreconditionFailed("PICO must be approved first")
            )
        }
        
        try {
            val prompt = RESEARCH_QUESTIONS_PROMPT
                .replace("{{POPULATION}}", input.population)
                .replace("{{INTERVENTION}}", input.intervention)
                .replace("{{COMPARISON}}", input.comparison ?: "None")
                .replace("{{OUTCOME}}", input.outcome)
            
            val response = llmService.generateStructured<QuestionsResponse>(
                prompt = prompt,
                schema = QuestionsResponse.serializer(),
                temperature = 0.3
            )
            
            // Convert to domain model
            val questions = ResearchQuestions(
                primary = ResearchQuestion(
                    id = "primary_1",
                    text = response.primary.text,
                    type = QuestionType.PRIMARY,
                    rationale = response.primary.rationale,
                    picoMapping = PicoMapping(
                        population = input.population,
                        intervention = input.intervention,
                        comparison = input.comparison,
                        outcome = input.outcome
                    )
                ),
                secondary = response.secondary.mapIndexed { i, q ->
                    ResearchQuestion(
                        id = "secondary_${i + 1}",
                        text = q.text,
                        type = QuestionType.SECONDARY,
                        rationale = q.rationale,
                        picoMapping = PicoMapping(
                            population = input.population,
                            intervention = input.intervention,
                            comparison = input.comparison,
                            outcome = input.outcome
                        )
                    )
                },
                approved = false
            )
            
            return StageResult.RequiresApproval(
                data = questions,
                reason = "Review and approve research questions"
            )
            
        } catch (e: Exception) {
            return StageResult.Failure(
                PipelineError.LlmCallFailed("Question generation failed: ${e.message}")
            )
        }
    }
}

@Serializable
private data class QuestionsResponse(
    val primary: QuestionDto,
    val secondary: List<QuestionDto>
)

@Serializable
private data class QuestionDto(
    val text: String,
    val rationale: String
)
```

## Tests

```kotlin
class ResearchQuestionsStageTest {
    
    @Test
    fun `generates valid research questions`() = runTest {
        val mockLlm = MockLlmService()
        mockLlm.mockResponse = QuestionsResponse(
            primary = QuestionDto(
                text = "What is the diagnostic accuracy of ML for crop disease?",
                rationale = "Main outcome"
            ),
            secondary = listOf(
                QuestionDto(
                    text = "How do different ML algorithms compare?",
                    rationale = "Algorithm comparison"
                ),
                QuestionDto(
                    text = "What image modalities are most effective?",
                    rationale = "Modality analysis"
                )
            )
        )
        
        val stage = ResearchQuestionsStage(mockLlm)
        val pico = ProblemFraming(
            population = "Crops",
            intervention = "ML",
            outcome = "Diagnostic accuracy",
            approved = true
        )
        
        val result = stage.execute(pico)
        
        assertIs<StageResult.RequiresApproval>(result)
        val questions = result.data
        
        assertEquals(QuestionType.PRIMARY, questions.primary.type)
        assertEquals(2, questions.secondary.size)
    }
}
```

## Next Stage

→ [Stage 3: Concept Expansion](stage-03-concept-expansion.md)
