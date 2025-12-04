# Task 09: Write Integration Tests
**Next:** ➡️ Task 10: Update Documentation

## Time: 180 minutes

---

✅ CI/CD pipeline includes these tests  
✅ Tests run in <30 seconds total  
✅ Tests are independent and idempotent  
✅ Error cases tested  
✅ File operations tested  
✅ Database operations tested  
✅ Code coverage >80% for new code  
✅ All tests pass consistently  
✅ Integration tests cover complete workflow  

## Acceptance Criteria

---

- [ ] Setup/teardown documented
- [ ] Test data explained
- [ ] Test purpose documented
### Documentation

- [ ] Performance acceptable (<5s per test)
- [ ] No flaky tests
- [ ] Tests pass in CI/CD
- [ ] All tests pass locally
### Test Execution

- [ ] Add concurrency tests
- [ ] Add file storage tests
- [ ] Add database tests
- [ ] Implement edge case tests
- [ ] Implement error path tests
- [ ] Implement happy path test
- [ ] Create integration test file
### Test Implementation

- [ ] Setup test environment
- [ ] Prepare test data
- [ ] Define test scenarios
- [ ] Identify all integration points
### Test Planning

## Checklist

---

- [ ] Edge cases (empty, max length, etc.)
- [ ] Concurrent access
- [ ] File artifact storage
- [ ] Database persistence
- [ ] Error handling paths
- [ ] Precondition validation
- [ ] Full pipeline execution (Stage 1 → 2 → 3)
### Integration Test Coverage

- Task 05 (Artifact Storage): >80%
- Task 04 (Pipeline Stage): >80%
- Task 02 (Validation): >90%
### Unit Test Coverage (Per Task)

## Test Coverage Goals

---

```
}
    }
        // Verify questions deleted
        // Delete project
        // Create project with questions
    fun `cascades delete on project removal`() {
    @Test
    
    }
        // Test thread safety
    fun `handles concurrent saves`() {
    @Test
    
    }
        // Load and compare
        // Verify JSON is valid
        // Verify file exists
        // Save using artifactStore
    fun `saves questions to JSON artifact`() {
    @Test
    
    }
        // Test foreign key relationships
        // Verify all fields persisted correctly
        // Insert into research_questions table
    fun `saves questions to database`() {
    @Test
    
class ResearchQuestionsPersistenceTest {

import kotlin.test.*
import org.junit.jupiter.api.*

package com.lumen.core.integration
```kotlin

### 2. ResearchQuestionsPersistenceTest.kt

```
}
    }
        // Verify user gets actionable error message
        // Verify error is caught and saved
        // Simulate LLM failure
    fun `handles LLM failure gracefully`() = runTest {
    @Test
    
    }
        assertTrue((result as StageResult.Failure).error.message.contains("approved"))
        assertTrue(result is StageResult.Failure)
        
        val result = questionsStage.execute(unapprovedPico)
        
        val unapprovedPico = ProblemFraming(..., approved = false)
        // Test that unapproved PICO blocks questions generation
    fun `validates pipeline preconditions`() = runTest {
    @Test
    
    }
        assertTrue(conceptResult is StageResult.RequiresApproval)
        val conceptResult = conceptStage.execute(approvedPico)
        // 7. Run concept expansion (should work with approved questions)
        
        assertEquals(questions.primary.text, loaded.primary.text)
        assertNotNull(loaded)
        val loaded = artifactStore.load<ResearchQuestions>(...)
        // 6. Verify artifact saved
        
        val approvedQuestions = questions.copy(approved = true)
        // 5. Approve questions
        
        assertFalse(questions.approved)
        assertTrue(questions.secondary.isNotEmpty())
        assertNotNull(questions.primary)
        val questions = (questionsResult as StageResult.RequiresApproval).data
        
        assertTrue(questionsResult is StageResult.RequiresApproval)
        val questionsResult = questionsStage.execute(approvedPico)
        // 4. Run research questions generation
        
        )
            approved = true
        val approvedPico = (picoResult as StageResult.RequiresApproval).data.copy(
        // 3. Approve PICO
        
        assertTrue(picoResult is StageResult.RequiresApproval)
        val picoResult = picoStage.execute(project)
        // 2. Run PICO extraction
        
        val project = Project(...)
        // 1. Create test project
    fun `complete workflow from PICO to Concept Expansion`() = runTest {
    @Test
    
    }
        // Setup artifact storage
        // Setup test database
        // Initialize with real or mock services
    fun setup() {
    @BeforeAll
    
    private lateinit var conceptStage: ConceptExpansionStage
    private lateinit var questionsStage: ResearchQuestionsStage
    private lateinit var picoStage: PicoExtractionStage
    
class ResearchQuestionsIntegrationTest {
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

import kotlin.test.*
import org.junit.jupiter.api.*
import kotlinx.coroutines.test.runTest
import com.lumen.core.domain.pipeline.*
import com.lumen.core.domain.stages.*
import com.lumen.core.domain.model.*

package com.lumen.core.integration
```kotlin

**Path:** `src/jvmTest/kotlin/com/lumen/core/integration/ResearchQuestionsIntegrationTest.kt`

### 1. ResearchQuestionsIntegrationTest.kt

## Implementation

---

3. **ResearchQuestionsUITest.kt** - UI workflow test (optional)
2. **ResearchQuestionsPersistenceTest.kt** - Database and file storage test
1. **ResearchQuestionsIntegrationTest.kt** - End-to-end pipeline test

## Files to Create

---

Create comprehensive integration tests that verify the complete Stage 2 workflow from PICO input through question generation, validation, storage, and UI interaction.

## Objective

---

**Assignee:** _____
**Dependencies:** All previous tasks  
**Estimated Effort:** 3 hours  
**Priority:** High  
**Status:** ⬜ Not Started  


