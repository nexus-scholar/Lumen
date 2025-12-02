# Stage 1: PICO Extraction

**Purpose:** Extract structured PICO framework from research question using LLM.

## Data Models

```kotlin
@Serializable
data class ProblemFraming(
    val population: String,
    val intervention: String,
    val comparison: String? = null,
    val outcome: String,
    val studyDesigns: List<String> = emptyList(),
    val timeframe: String? = null,
    val context: String? = null,
    val approved: Boolean = false,
    val llmModel: String? = null,
    val prompt: String? = null,
    val rawOutput: String? = null
)
```

## LLM Prompt

```kotlin
val PICO_EXTRACTION_PROMPT = """
You are a research methodology expert. Extract PICO components from the following research question.

Research Question:
{{RESEARCH_IDEA}}

Extract:
- **Population**: The group being studied (e.g., "patients with type 2 diabetes", "wheat crops")
- **Intervention**: The treatment/method being investigated (e.g., "metformin", "machine learning detection")
- **Comparison**: The alternative or control (optional, e.g., "placebo", "traditional methods")
- **Outcome**: What is being measured (e.g., "blood glucose levels", "diagnostic accuracy")
- **Study Designs**: Appropriate study types (e.g., ["RCT", "cohort study"])
- **Timeframe**: If mentioned (e.g., "2015-2024")
- **Context**: Setting or constraints (e.g., "low-resource settings", "field conditions")

Return ONLY valid JSON matching this schema:
{
  "population": string,
  "intervention": string,
  "comparison": string | null,
  "outcome": string,
  "study_designs": string[],
  "timeframe": string | null,
  "context": string | null
}

Be specific and detailed. If comparison is not applicable, use null.
""".trimIndent()
```

## Implementation

```kotlin
class PicoExtractionStage(
    private val llmService: LlmService
) : PipelineStage<ProjectContext, ProblemFraming> {
    
    override suspend fun execute(input: ProjectContext): StageResult<ProblemFraming> {
        try {
            // Generate prompt
            val prompt = PICO_EXTRACTION_PROMPT.replace(
                "{{RESEARCH_IDEA}}",
                input.rawIdea
            )
            
            // Call LLM with structured output
            val response = llmService.generateStructured<PicoResponse>(
                prompt = prompt,
                schema = PicoResponse.serializer(),
                temperature = 0.2 // Low temp for factual extraction
            )
            
            // Convert to domain model
            val pico = ProblemFraming(
                population = response.population,
                intervention = response.intervention,
                comparison = response.comparison,
                outcome = response.outcome,
                studyDesigns = response.study_designs,
                timeframe = response.timeframe,
                context = response.context,
                approved = false, // Requires user approval
                llmModel = "gpt-4",
                prompt = prompt,
                rawOutput = Json.encodeToString(PicoResponse.serializer(), response)
            )
            
            // Validate
            val validation = validatePico(pico)
            if (!validation.isValid) {
                return StageResult.Failure(
                    PipelineError.ValidationFailed(validation.errors)
                )
            }
            
            // Save artifact
            artifactStore.save(input.id, pico, ProblemFraming.serializer())
            
            // Requires human approval before continuing
            return StageResult.RequiresApproval(
                data = pico,
                reason = "Please review and approve PICO extraction"
            )
            
        } catch (e: Exception) {
            return StageResult.Failure(
                PipelineError.LlmCallFailed("PICO extraction failed: ${e.message}")
            )
        }
    }
    
    private fun validatePico(pico: ProblemFraming): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (pico.population.length < 5) {
            errors.add("Population description too short")
        }
        if (pico.intervention.length < 5) {
            errors.add("Intervention description too short")
        }
        if (pico.outcome.length < 5) {
            errors.add("Outcome description too short")
        }
        
        // Check for placeholder text
        val placeholders = listOf("TODO", "TBD", "N/A", "Unknown")
        listOf(pico.population, pico.intervention, pico.outcome).forEach { field ->
            if (placeholders.any { it in field }) {
                errors.add("Field contains placeholder text: $field")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

@Serializable
private data class PicoResponse(
    val population: String,
    val intervention: String,
    val comparison: String? = null,
    val outcome: String,
    val study_designs: List<String> = emptyList(),
    val timeframe: String? = null,
    val context: String? = null
)
```

## Desktop UI

```kotlin
@Composable
fun PicoExtractionScreen(viewModel: PicoViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("PICO Framework Extraction", style = MaterialTheme.typography.headlineMedium)
        
        when (val state = viewModel.state) {
            is PicoState.Loading -> {
                CircularProgressIndicator()
                Text("Extracting PICO from your research question...")
            }
            
            is PicoState.Generated -> {
                // Show editable PICO
                OutlinedTextField(
                    value = state.pico.population,
                    onValueChange = { viewModel.updatePopulation(it) },
                    label = { Text("Population") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = state.pico.intervention,
                    onValueChange = { viewModel.updateIntervention(it) },
                    label = { Text("Intervention") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = state.pico.comparison ?: "",
                    onValueChange = { viewModel.updateComparison(it) },
                    label = { Text("Comparison (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = state.pico.outcome,
                    onValueChange = { viewModel.updateOutcome(it) },
                    label = { Text("Outcome") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row {
                    Button(
                        onClick = { viewModel.regenerate() },
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text("↻ Regenerate")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = { viewModel.approve() },
                        enabled = state.pico.population.isNotBlank() &&
                                  state.pico.intervention.isNotBlank() &&
                                  state.pico.outcome.isNotBlank()
                    ) {
                        Text("✓ Approve & Continue")
                    }
                }
            }
            
            is PicoState.Error -> {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.retry() }) {
                    Text("Retry")
                }
            }
        }
    }
}
```

## CLI

```bash
# Extract PICO (requires approval)
lumen run --project <id> --stage pico

# Manual PICO entry (skip LLM)
lumen pico --project <id> \
  --population "Wheat crops with fungal diseases" \
  --intervention "Deep learning image classification" \
  --outcome "Diagnostic accuracy (sensitivity/specificity)"
```

## Tests

```kotlin
class PicoExtractionStageTest {
    
    @Test
    fun `extracts valid PICO from research question`() = runTest {
        val mockLlm = MockLlmService()
        mockLlm.mockResponse = PicoResponse(
            population = "Wheat crops with fungal diseases",
            intervention = "Machine learning detection methods",
            outcome = "Diagnostic accuracy",
            study_designs = listOf("cohort", "case-control")
        )
        
        val stage = PicoExtractionStage(mockLlm)
        val context = ProjectContext(
            id = "test",
            rawIdea = "What are ML methods for detecting wheat disease?",
            reviewType = ReviewType.DIAGNOSTIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = stage.execute(context)
        
        assertIs<StageResult.RequiresApproval>(result)
        val pico = result.data
        
        assertEquals("Wheat crops with fungal diseases", pico.population)
        assertEquals("Machine learning detection methods", pico.intervention)
        assertEquals("Diagnostic accuracy", pico.outcome)
        assertFalse(pico.approved) // Must be manually approved
    }
    
    @Test
    fun `rejects PICO with too-short fields`() = runTest {
        val mockLlm = MockLlmService()
        mockLlm.mockResponse = PicoResponse(
            population = "ML", // Too short
            intervention = "AI", // Too short
            outcome = "acc" // Too short
        )
        
        val stage = PicoExtractionStage(mockLlm)
        val context = ProjectContext(
            id = "test",
            rawIdea = "Test",
            reviewType = ReviewType.INTERVENTION,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = stage.execute(context)
        
        assertIs<StageResult.Failure>(result)
    }
}
```

## Next Stage

→ [Stage 2: Research Questions](stage-02-research-questions.md)
