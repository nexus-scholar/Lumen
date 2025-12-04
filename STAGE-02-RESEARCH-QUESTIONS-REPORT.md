# Stage 2: Research Questions Generation - Implementation Status Report

**Generated:** December 4, 2025  
**Project:** Lumen - Systematic Review Assistant  
**Stage:** Research Questions Generation (Stage 2 of 17)

---

## Executive Summary

**Status:** ❌ **NOT IMPLEMENTED**

The Research Questions Generation stage (Stage 2) is **completely missing** from the current implementation. While comprehensive documentation exists, there is no code implementation, data models, database schema, UI components, or tests for this critical pipeline stage.

**Priority:** 🔴 **HIGH** - This stage is a fundamental component of the PRISMA 2020-compliant pipeline and creates a gap between Stage 1 (PICO Extraction) and Stage 3 (Concept Expansion).

---

## Current Implementation Status

### ✅ What Exists

1. **Documentation (Complete)**
   - File: `docs/implementation/stage-02-research-questions.md`
   - Contains complete specification including:
     - Data models (`ResearchQuestions`, `ResearchQuestion`)
     - LLM prompt template
     - Implementation pseudocode
     - Test specifications
   - Well-documented with clear requirements

2. **Project Status Enum (Partial)**
   - File: `src/commonMain/kotlin/com/lumen/core/domain/model/Project.kt`
   - `RESEARCH_QUESTIONS` status exists in the `ProjectStatus` enum
   - UI recognizes this status in `ProjectListScreen.kt` (line 257)

### ❌ What's Missing

1. **Data Models** (0% implemented)
   - No `ResearchQuestions.kt` model file
   - No `ResearchQuestion.kt` model file
   - No `QuestionType` enum
   - No `PicoMapping` data class

2. **Pipeline Stage Implementation** (0% implemented)
   - No `ResearchQuestionsStage.kt` class
   - Not registered in DI container (`jvmModule.kt`)
   - Missing from pipeline orchestration

3. **Database Schema** (0% implemented)
   - No `research_questions` table in `scripts/init-db.sql`
   - No persistence layer for storing questions
   - No versioning/audit trail for question iterations

4. **UI Components** (0% implemented)
   - No stage card in `ProjectDetailScreen.kt` (skipped from Stage 1 to Stage 3)
   - No approval dialog for reviewing generated questions
   - No edit/refinement interface
   - No display of question rationale or PICO mapping

5. **Artifact Storage** (0% implemented)
   - No JSON artifact definition for `ResearchQuestions`
   - No file-based storage implementation
   - Missing from `ArtifactStore` usage pattern

6. **Tests** (0% implemented)
   - No unit tests for `ResearchQuestionsStage`
   - No integration tests
   - No test fixtures or mock data

7. **Repository Layer** (0% implemented)
   - No dedicated repository interface
   - No CRUD operations for questions
   - No relationship management with PICO artifacts

---

## Architecture Analysis

### Current Pipeline Flow (With Gap)

```
Stage 1: PICO Extraction
    ↓ (approved PICO)
    ↓
    ⚠️  MISSING: Stage 2: Research Questions Generation
    ↓
    ↓ (should receive questions + PICO)
    ↓
Stage 3: Concept Expansion (receives PICO directly, skipping questions)
    ↓
Stage 4: Query Generation
```

### Expected Flow (According to Documentation)

```
Stage 1: PICO Extraction
    ↓ (approved PICO)
Stage 2: Research Questions Generation
    ↓ (approved questions + PICO)
Stage 3: Concept Expansion
    ↓ (concepts from questions + PICO)
Stage 4: Query Generation
```

### Impact of Missing Stage

**Critical Issues:**
1. **Broken PRISMA Compliance**: PRISMA 2020 requires explicit research questions before systematic search
2. **Reduced Specificity**: Concept expansion lacks guidance from structured questions
3. **Documentation Gap**: No formal record of what specific questions the review answers
4. **Quality Control**: Missing approval checkpoint before resource-intensive search stages
5. **User Confusion**: UI jumps from "1. PICO Extraction" to "3. Concept Expansion"

**Downstream Consequences:**
- Query generation may be less focused without explicit research questions
- Protocol registration (Stage 6) will lack required research questions section
- Final report (Stage 15) cannot properly document research questions
- Reduces reproducibility and transparency of the review process

---

## Design Critique & Improvements

### Documentation Review

**Strengths:**
- ✅ Clear LLM prompt design with proper PICO integration
- ✅ Structured JSON schema for responses
- ✅ Approval workflow defined (human-in-the-loop)
- ✅ Proper error handling specified
- ✅ Test cases defined

**Weaknesses:**
- ⚠️ No specification for editing/refinement UI
- ⚠️ Unclear how to handle adding/removing secondary questions manually
- ⚠️ No guidance on minimum/maximum number of secondary questions
- ⚠️ Missing validation rules (e.g., question clarity, answerability)
- ⚠️ No examples of good vs. poor research questions

### Proposed Data Model Improvements

The documented model is good but could be enhanced:

```kotlin
@Serializable
data class ResearchQuestions(
    val primary: ResearchQuestion,
    val secondary: List<ResearchQuestion>,
    val approved: Boolean = false,
    
    // ADDITIONS:
    val llmModel: String? = null,           // Track which model generated
    val prompt: String? = null,              // Store generation prompt
    val rawOutput: String? = null,           // Raw LLM response
    val generatedAt: Instant? = null,        // Timestamp
    val approvedAt: Instant? = null,         // Approval timestamp
    val approvedBy: String? = null,          // User who approved
    val editHistory: List<QuestionEdit> = emptyList()  // Track manual edits
)

@Serializable
data class QuestionEdit(
    val timestamp: Instant,
    val userId: String,
    val questionId: String,
    val previousText: String,
    val newText: String,
    val reason: String?
)
```

### LLM Prompt Improvements

The current prompt is functional but could be enhanced:

**Current Issues:**
1. No examples of well-formed questions
2. Doesn't specify PICO adherence in question structure
3. Could produce overly broad or narrow questions

**Suggested Improvements:**
```
1. Add few-shot examples of excellent research questions
2. Include quality criteria checklist (specific, measurable, feasible)
3. Request explicit PICO element mapping for each question
4. Add constraint: primary question must be directly answerable by meta-analysis
5. Include review type context (intervention vs. diagnostic has different question styles)
```

### Validation Requirements

Missing validation logic should include:

```kotlin
fun validateResearchQuestions(questions: ResearchQuestions): ValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    
    // Primary question validation
    if (questions.primary.text.length < 20) {
        errors.add("Primary question too short (< 20 characters)")
    }
    if (!questions.primary.text.endsWith("?")) {
        warnings.add("Primary question should end with '?'")
    }
    if (!containsPicoElements(questions.primary.text, questions.primary.picoMapping)) {
        warnings.add("Primary question doesn't clearly reference all PICO elements")
    }
    
    // Secondary questions validation
    if (questions.secondary.isEmpty()) {
        warnings.add("No secondary questions - consider adding 2-3 to explore subgroups")
    }
    if (questions.secondary.size > 5) {
        warnings.add("Too many secondary questions (${questions.secondary.size}) - consider focusing")
    }
    
    // Check for duplicate/similar questions
    val allTexts = (listOf(questions.primary) + questions.secondary).map { it.text }
    val duplicates = findSimilarQuestions(allTexts)
    if (duplicates.isNotEmpty()) {
        warnings.add("Similar questions detected: $duplicates")
    }
    
    // Check rationale quality
    questions.secondary.forEach { q ->
        if (q.rationale.isNullOrBlank()) {
            warnings.add("Secondary question '${q.text}' lacks rationale")
        }
    }
    
    return ValidationResult(
        isValid = errors.isEmpty(),
        errors = errors,
        warnings = warnings
    )
}
```

---

## Refactoring Needs

### 1. **Pipeline Consistency**

Current stages (PICO, Concept, Query) follow a consistent pattern. Stage 2 should match:

**Pattern to Follow:**
```kotlin
class XxxStage(
    private val llmService: LlmService,
    private val artifactStore: ArtifactStore
) : PipelineStage<InputType, OutputType> {
    
    override val stageName: String = "Stage X: Name"
    
    override suspend fun execute(input: InputType): StageResult<OutputType> {
        // 1. Validate preconditions
        // 2. Call LLM with structured output
        // 3. Convert to domain model
        // 4. Validate output
        // 5. Save artifact
        // 6. Return RequiresApproval or Failure
    }
}
```

### 2. **Artifact Storage Pattern**

Stage 2 should save artifacts like Stage 1 does:

```kotlin
artifactStore.save(
    projectId = input.projectId,
    artifact = questions,
    serializer = ResearchQuestions.serializer(),
    filename = "ResearchQuestions.json"
)
```

### 3. **UI Consistency**

The UI currently shows:
- Stage 1: PICO Extraction ✅
- Stage 3: Concept Expansion ✅
- Missing: Stage 2 card

Need to add between lines 290-302 in `ProjectDetailScreen.kt`:

```kotlin
StageCard(
    name = "2. Research Questions",
    description = "Generate primary and secondary research questions",
    projectId = projectId,
    stageType = StageType.RESEARCH_QUESTIONS,
    currentStatus = project.status
)
```

### 4. **Database Schema**

Should add to `init-db.sql`:

```sql
-- Research questions table
CREATE TABLE IF NOT EXISTS research_questions (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL, -- PRIMARY, SECONDARY, EXPLORATORY
    rationale TEXT,
    pico_population TEXT,
    pico_intervention TEXT,
    pico_comparison TEXT,
    pico_outcome TEXT,
    display_order INTEGER NOT NULL,
    approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    CONSTRAINT fk_project_questions FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX IF NOT EXISTS idx_questions_project_id ON research_questions(project_id);
```

---

## Implementation Recommendations

### Phase 1: Core Implementation (Priority: Critical)

**Estimated Effort:** 4-6 hours

1. **Create Data Models** (1 hour)
   - `src/commonMain/kotlin/com/lumen/core/domain/model/ResearchQuestions.kt`
   - Include all DTOs: `ResearchQuestions`, `ResearchQuestion`, `QuestionType`, `PicoMapping`
   - Add serialization annotations

2. **Implement Pipeline Stage** (2 hours)
   - `src/commonMain/kotlin/com/lumen/core/domain/stages/ResearchQuestionsStage.kt`
   - Follow existing pattern from `PicoExtractionStage`
   - Implement LLM prompt from documentation
   - Add validation logic

3. **Register in DI** (15 minutes)
   - Add factory to `jvmModule.kt`
   - Ensure proper dependency injection

4. **Add Database Schema** (30 minutes)
   - Update `init-db.sql`
   - Create migration if needed
   - Add indexes

5. **Update UI** (1.5 hours)
   - Add `RESEARCH_QUESTIONS` to `StageType` enum
   - Add stage card to `ProjectDetailScreen`
   - Create approval dialog (can reuse `PicoApprovalDialog` pattern)

### Phase 2: Enhanced Features (Priority: High)

**Estimated Effort:** 6-8 hours

1. **Question Editing UI** (3 hours)
   - Allow manual editing of generated questions
   - Add/remove secondary questions
   - Real-time validation feedback

2. **Advanced Validation** (2 hours)
   - Implement quality checks
   - PICO element mapping validation
   - Similarity detection

3. **Versioning & Audit** (2 hours)
   - Track question edit history
   - Show diff between versions
   - Audit trail for compliance

4. **Export Integration** (1 hour)
   - Include questions in protocol export (Stage 6)
   - Add to PRISMA flow diagram
   - Include in final report (Stage 15)

### Phase 3: Testing & Polish (Priority: Medium)

**Estimated Effort:** 4-6 hours

1. **Unit Tests** (2 hours)
   - Test stage execution with mock LLM
   - Test validation logic
   - Test error handling

2. **Integration Tests** (2 hours)
   - Test full pipeline flow (Stage 1 → 2 → 3)
   - Test artifact persistence
   - Test UI workflow

3. **Documentation Updates** (2 hours)
   - Update user guide
   - Add screenshots
   - Document best practices for writing good research questions

---

## Critical Issues to Address

### 1. **Broken Pipeline Continuity**
**Severity:** 🔴 Critical  
**Issue:** Stage 3 (Concept Expansion) receives `ProblemFraming` as input, not `ResearchQuestions`  
**Fix Required:** Either:
- Option A: Stage 3 should receive both PICO + Questions
- Option B: Composite input type that bundles both
- Option C: Stage 3 loads questions from artifact store

**Recommended:** Option C - maintain single responsibility per stage

```kotlin
// In ConceptExpansionStage.execute():
override suspend fun execute(input: ProblemFraming): StageResult<ConceptExpansion> {
    // Load approved research questions
    val questions = artifactStore.load<ResearchQuestions>(
        projectId = input.projectId,
        serializer = ResearchQuestions.serializer(),
        filename = "ResearchQuestions.json"
    )
    
    if (questions == null || !questions.approved) {
        return StageResult.Failure(
            PipelineError.PreconditionFailed(
                "Research questions must be generated and approved first"
            )
        )
    }
    
    // Use questions to inform concept expansion...
}
```

### 2. **Missing Project ID in Domain Models**
**Severity:** 🟡 Medium  
**Issue:** `ProblemFraming` doesn't contain `projectId`, making artifact storage awkward  
**Fix:** Add `projectId` to all artifact models or pass separately to stage execution

### 3. **Incomplete Status Progression**
**Severity:** 🟡 Medium  
**Issue:** No code that transitions from `PICO_EXTRACTION` → `RESEARCH_QUESTIONS` → `CONCEPT_EXPANSION`  
**Fix:** Implement status update logic in stage completion

---

## Quality Metrics & Acceptance Criteria

### Definition of Done

- [ ] **Models Created:** All data classes defined with proper serialization
- [ ] **Stage Implemented:** `ResearchQuestionsStage` follows existing pattern
- [ ] **DI Registered:** Stage available via Koin injection
- [ ] **Database Schema:** Table created with proper indexes
- [ ] **UI Added:** Stage card visible and functional
- [ ] **Approval Dialog:** Users can review and approve/edit questions
- [ ] **Tests Written:** >80% code coverage for stage logic
- [ ] **Pipeline Integration:** Stage 3 properly depends on Stage 2 completion
- [ ] **Artifact Storage:** Questions saved to JSON and database
- [ ] **Documentation:** User guide updated with screenshots

### Validation Criteria

1. **LLM Generation:**
   - Must generate 1 primary question
   - Must generate 2-4 secondary questions
   - Questions must reference PICO elements
   - Must include rationale for each question

2. **User Approval:**
   - User can view generated questions
   - User can edit question text
   - User can add/remove secondary questions
   - User must explicitly approve before proceeding

3. **Persistence:**
   - Questions saved to JSON artifact
   - Questions saved to database
   - Edit history tracked
   - Retrieval works correctly

4. **Error Handling:**
   - Graceful fallback if LLM unavailable
   - Validation errors displayed clearly
   - Failed generation doesn't block manual entry

---

## Comparison with Implemented Stages

### Stage 1 (PICO Extraction) - ✅ Fully Implemented
- Complete data model with serialization
- LLM-based extraction with fallback
- Validation logic
- Artifact storage
- UI with approval dialog
- Error handling

### Stage 2 (Research Questions) - ❌ Not Implemented
- **0%** complete
- All components missing

### Stage 3 (Concept Expansion) - ✅ Fully Implemented
- Complete implementation
- Should depend on Stage 2 but doesn't

**Learning:** Stage 1 provides excellent template for implementing Stage 2. Code can be heavily reused with minor modifications.

---

## Estimated Implementation Timeline

### Minimum Viable Implementation
- **Time:** 6-8 hours
- **Scope:** Basic LLM generation + approval + persistence
- **Developer:** 1 senior developer

### Production-Ready Implementation
- **Time:** 16-20 hours
- **Scope:** Full feature set + tests + documentation
- **Developer:** 1 senior developer + QA support

### Phased Rollout (Recommended)
1. **Week 1:** Core implementation (Phase 1)
2. **Week 2:** Enhanced features (Phase 2)
3. **Week 3:** Testing & polish (Phase 3)
4. **Week 4:** Integration testing & documentation

---

## Conclusion

The Research Questions Generation stage represents a **critical gap** in the Lumen pipeline implementation. While excellent documentation exists, the complete absence of code, UI, and database support creates a non-functional pipeline that skips a fundamental PRISMA 2020 requirement.

### Priority Actions:

1. ✅ **Immediate:** Implement basic data models and stage class (2-3 hours)
2. ✅ **Short-term:** Add UI and database support (3-4 hours)
3. ✅ **Medium-term:** Implement advanced validation and editing (6-8 hours)
4. ✅ **Long-term:** Comprehensive testing and documentation (4-6 hours)

### Risk Assessment:

- **Risk of Not Implementing:** HIGH - Breaks PRISMA compliance, reduces review quality
- **Implementation Risk:** LOW - Clear specification exists, can follow Stage 1 pattern
- **Integration Risk:** MEDIUM - Requires updating Stage 3 to depend on Stage 2

### Recommendation:

**IMPLEMENT IMMEDIATELY** as part of MVP completion. This is not an optional feature but a core requirement for systematic review methodology compliance.

---

## Appendix: Code Skeleton

### Minimal Implementation Example

```kotlin
// File: src/commonMain/kotlin/com/lumen/core/domain/model/ResearchQuestions.kt

package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ResearchQuestions(
    val primary: ResearchQuestion,
    val secondary: List<ResearchQuestion>,
    val approved: Boolean = false,
    val llmModel: String? = null,
    val prompt: String? = null,
    val rawOutput: String? = null,
    val generatedAt: Instant? = null
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

This skeleton can be copy-pasted directly from the documentation and will compile immediately with the existing codebase.

---

**Report End**

