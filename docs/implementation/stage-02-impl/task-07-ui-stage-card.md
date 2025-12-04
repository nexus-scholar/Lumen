# Task 07: Add UI Stage Card
**Next:** ➡️ Task 08: Create Approval Dialog

## Time: 90 minutes

---

   - [ ] Network timeout
   - [ ] LLM API error
   - [ ] PICO not approved
   - [ ] PICO not found
4. **Edge Cases:**

   - [ ] Error message if LLM unavailable
   - [ ] Shows warnings if any
   - [ ] Displays questions in result area
   - [ ] Generates questions (if LLM available)
   - [ ] Shows loading spinner
   - [ ] Click Run button
3. **Execution Test:**

   - [ ] Stays enabled after completion
   - [ ] Enabled after PICO approved
   - [ ] Disabled on new project (before PICO)
2. **Enable/Disable Test:**

   - [ ] Description text clear
   - [ ] Styling consistent with other cards
   - [ ] Card appears in correct order (between 1 and 3)
1. **Visual Test:**

### Manual Test Checklist

## Testing

---

✅ Error handling shows clear messages  
✅ Shows warnings if any  
✅ Displays generated questions in result  
✅ Shows loading state while executing  
✅ Run button works and calls stage  
✅ Enabled only after PICO completed  
✅ Card shows "2. Research Questions"  
✅ Stage card visible in UI between PICO and Concept  

## Acceptance Criteria

---

- [ ] Test execution works
- [ ] Test card is disabled before PICO
- [ ] Test card is enabled after PICO approval
- [ ] Test UI displays card correctly
- [ ] Add necessary imports
- [ ] Add getQuestionsStage() helper
- [ ] Implement execution logic in when block
- [ ] Add StageCard for Research Questions
- [ ] Update isEnabled logic
- [ ] Add RESEARCH_QUESTIONS to StageType enum

## Checklist

---

```
}
    }
        throw e
        e.printStackTrace()
        println("❌ DEBUG: Failed to get ResearchQuestionsStage: ${e.message}")
    } catch (e: Exception) {
        get<ResearchQuestionsStage>()
    return try {
    println("🔍 DEBUG: Getting ResearchQuestionsStage from Koin...")
fun getQuestionsStage(): ResearchQuestionsStage {
```kotlin

**Around line 95:**

### 5. Add Helper to StageKoinHelper

```
}
    // ...rest of stages...
    
    }
        // ...existing concept code...
    StageType.CONCEPT -> {
    
    }
        }
            }
                isRunning = false
            } finally {
                result = "❌ Error: ${e.message}"
                e.printStackTrace()
                println("❌ DEBUG: Exception: ${e.message}")
            } catch (e: Exception) {
                }
                    }
                        result = "❌ Failed: ${stageResult.error.message}"
                        println("❌ DEBUG: Research questions generation failed")
                    is StageResult.Failure -> {
                    }
                        }
                            }
                                }
                                    appendLine("  • $it")
                                stageResult.suggestions.forEach {
                                appendLine("⚠️ Warnings:")
                                appendLine()
                            if (stageResult.suggestions.isNotEmpty()) {
                            }
                                appendLine("Secondary ${i+1}: ${q.text}")
                            stageResult.data.secondary.forEachIndexed { i, q ->
                            appendLine()
                            appendLine("Primary: ${stageResult.data.primary.text}")
                            appendLine()
                            appendLine("✅ Questions generated - Review required")
                        result = buildString {
                        println("⚠️ DEBUG: Questions require approval")
                    is StageResult.RequiresApproval -> {
                    }
                        isCompleted = true
                        result = "✅ Research questions generated successfully"
                        println("✅ DEBUG: Research questions generated")
                    is StageResult.Success -> {
                when (val stageResult = stage.execute(pico)) {
                
                val stage = StageKoinHelper.getQuestionsStage()
                println("🔍 DEBUG: Executing research questions generation...")
                
                }
                    return@launch
                    isRunning = false
                    result = "❌ PICO must be approved before generating questions."
                if (!pico.approved) {
                
                val pico = Json.decodeFromString<ProblemFraming>(picoJson.readText())
                
                }
                    return@launch
                    isRunning = false
                    result = "❌ PICO not found. Please complete Stage 1 first."
                if (!picoJson.exists()) {
                val picoJson = File("data/$projectId/ProblemFraming.json")
                // Load approved PICO
            try {
            
            result = null
            isRunning = true
        scope.launch {
        println("🔍 DEBUG: Loading PICO for research questions generation...")
    StageType.RESEARCH_QUESTIONS -> {
    // ADD THIS:
    
    }
        // ...existing PICO code...
    StageType.PICO -> {
when (stageType) {
```kotlin

**In StageCard's onClick handler (around line 495):**

### 4. Add Execution Logic in StageCard

```
)
    currentStatus = project.status
    stageType = StageType.CONCEPT,
    projectId = projectId,
    description = "Generate synonyms and related terms",
    name = "3. Concept Expansion",
StageCard(

)
    currentStatus = project.status
    stageType = StageType.RESEARCH_QUESTIONS,
    projectId = projectId,
    description = "Generate primary and secondary research questions",
    name = "2. Research Questions",
StageCard(
// ADD THIS:

)
    currentStatus = project.status
    stageType = StageType.PICO,
    projectId = projectId,
    description = "Extract Population, Intervention, Comparison, Outcome",
    name = "1. PICO Extraction",
StageCard(
```kotlin

**In ProjectDetailScreen composable, after PICO card (around line 290):**

### 3. Add Stage Card

```
}
    StageType.DEDUP -> currentStatus >= ProjectStatus.SEARCH_EXECUTION
    StageType.SEARCH -> currentStatus >= ProjectStatus.TEST_REFINE
    StageType.TEST -> currentStatus >= ProjectStatus.QUERY_GENERATION
    StageType.QUERY -> currentStatus >= ProjectStatus.CONCEPT_EXPANSION
    StageType.CONCEPT -> currentStatus >= ProjectStatus.RESEARCH_QUESTIONS  // CHANGE FROM PICO_EXTRACTION
    StageType.RESEARCH_QUESTIONS -> currentStatus >= ProjectStatus.PICO_EXTRACTION  // ADD THIS
    StageType.PICO -> true
val isEnabled = when (stageType) {
```kotlin

**Around line 376:**

### 2. Update isEnabled Logic

```
}
    DEDUP
    SEARCH, 
    TEST, 
    QUERY, 
    CONCEPT, 
    RESEARCH_QUESTIONS,  // ADD THIS
    PICO, 
enum class StageType {
```kotlin

**File:** `src/jvmMain/kotlin/com/lumen/desktop/ui/ProjectDetailScreen.kt` (around line 358)

### 1. Update StageType Enum

## Implementation

---

2. **StageType** enum - Add RESEARCH_QUESTIONS
1. **ProjectDetailScreen.kt** - Add stage card

## Files to Modify

---

Add Stage 2 card to the UI pipeline view in `ProjectDetailScreen.kt`, allowing users to run the research questions generation stage.

## Objective

---

**Assignee:** _____
**Dependencies:** Task 06 (DI Registration)  
**Estimated Effort:** 1.5 hours  
**Priority:** High  
**Status:** ⬜ Not Started  


