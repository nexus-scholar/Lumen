# Task 01: Create Data Models

**Status:** ⬜ Not Started  
**Priority:** Critical  
**Estimated Effort:** 1 hour  
**Dependencies:** None  
**Assignee:** _____

---

## Objective

Create all Kotlin data models required for Research Questions functionality with proper serialization, immutability, and type safety.

---

## Files to Create

### 1. ResearchQuestions.kt
**Path:** `src/commonMain/kotlin/com/lumen/core/domain/model/ResearchQuestions.kt`

**Content:**
```kotlin
package com.lumen.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Container for all research questions generated from PICO framework
 */
@Serializable
data class ResearchQuestions(
    val primary: ResearchQuestion,
    val secondary: List<ResearchQuestion>,
    val approved: Boolean = false,
    val llmModel: String? = null,
    val prompt: String? = null,
    val rawOutput: String? = null,
    val generatedAt: Instant? = null,
    val approvedAt: Instant? = null,
    val approvedBy: String? = null
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

/**
 * Internal DTO for LLM response parsing
 */
@Serializable
internal data class QuestionsResponse(
    val primary: QuestionDto,
    val secondary: List<QuestionDto>
)

@Serializable
internal data class QuestionDto(
    val text: String,
    val rationale: String
)
```

---

## Implementation Checklist

### Pre-Implementation
- [ ] Read existing PICO model (`ProblemFraming.kt`) for consistency
- [ ] Review Kotlin serialization documentation
- [ ] Check naming conventions in existing models

### Implementation Steps
- [ ] Create directory structure if not exists
- [ ] Create `ResearchQuestions.kt` file
- [ ] Copy data model code from above
- [ ] Add necessary imports
- [ ] Verify file compiles without errors

### Post-Implementation
- [ ] Run `./gradlew build` to verify compilation
- [ ] Check no compiler warnings
- [ ] Verify serialization annotations present
- [ ] Confirm all fields are immutable (val, not var)
- [ ] Add KDoc comments for public classes

---

## Acceptance Criteria

### Functional Requirements
✅ **AC1:** `ResearchQuestions` data class exists with all specified fields  
✅ **AC2:** `ResearchQuestion` contains id, text, type, rationale, and PICO mapping  
✅ **AC3:** `QuestionType` enum has PRIMARY, SECONDARY, EXPLORATORY values  
✅ **AC4:** `PicoMapping` matches PICO structure from `ProblemFraming`  
✅ **AC5:** All classes are `@Serializable` for JSON conversion  
✅ **AC6:** Internal DTOs for LLM response parsing included

### Non-Functional Requirements
✅ **AC7:** All fields are immutable (`val`)  
✅ **AC8:** No compilation errors or warnings  
✅ **AC9:** Follows project naming conventions  
✅ **AC10:** Package structure matches existing domain models  

---

## Verification Steps

### Step 1: Compilation Check
```bash
./gradlew :compileKotlinJvm
```
**Expected:** No errors, clean build

### Step 2: Serialization Test
Create temporary test in `src/jvmTest/kotlin`:
```kotlin
@Test
fun `ResearchQuestions can be serialized to JSON`() {
    val questions = ResearchQuestions(
        primary = ResearchQuestion(
            id = "primary_1",
            text = "What is the effect of X on Y?",
            type = QuestionType.PRIMARY,
            rationale = "Main research question",
            picoMapping = PicoMapping("Population", "Intervention", null, "Outcome")
        ),
        secondary = emptyList(),
        approved = false,
        generatedAt = Clock.System.now()
    )
    
    val json = Json.encodeToString(ResearchQuestions.serializer(), questions)
    val decoded = Json.decodeFromString(ResearchQuestions.serializer(), json)
    
    assertEquals(questions.primary.text, decoded.primary.text)
}
```
**Expected:** Test passes

### Step 3: IDE Check
- [ ] Open file in IntelliJ IDEA
- [ ] No red underlines or warnings
- [ ] Auto-completion works for all fields
- [ ] `@Serializable` annotation recognized

### Step 4: Code Review Checklist
- [ ] All fields have appropriate types
- [ ] Nullable fields use `?` correctly
- [ ] Default values are reasonable
- [ ] No mutable collections
- [ ] KDoc comments added for public APIs

---

## Integration Points

### Used By (Downstream Dependencies)
- Task 02: Validation Logic (will validate these models)
- Task 04: Pipeline Stage (will create instances)
- Task 05: Artifact Storage (will serialize to JSON)
- Task 08: Approval Dialog (will display in UI)

### Uses (Upstream Dependencies)
- None (foundation task)

---

## Rollback Procedure

If this task needs to be rolled back:

1. Delete file:
   ```bash
   rm src/commonMain/kotlin/com/lumen/core/domain/model/ResearchQuestions.kt
   ```

2. Verify clean build:
   ```bash
   ./gradlew clean build
   ```

3. Check no references:
   ```bash
   grep -r "ResearchQuestions" src/
   ```

---

## Common Issues & Solutions

### Issue 1: Serialization Import Not Found
**Symptom:** `Cannot find 'Serializable'`  
**Solution:** Add to `build.gradle.kts`:
```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
    }
}
```

### Issue 2: Instant Class Not Found
**Symptom:** `Cannot resolve 'Instant'`  
**Solution:** Add import:
```kotlin
import kotlinx.datetime.Instant
```
And ensure kotlinx-datetime dependency exists.

### Issue 3: Circular Dependency
**Symptom:** Build fails with circular reference  
**Solution:** Ensure no imports between this file and others in same package

---

## Quality Metrics

### Code Quality
- [ ] Cyclomatic complexity: 1 (data classes only)
- [ ] Lines of code: <100
- [ ] No code smells reported by detekt
- [ ] All fields properly typed

### Documentation
- [ ] KDoc for `ResearchQuestions` class
- [ ] KDoc for `ResearchQuestion` class
- [ ] Field descriptions for non-obvious fields
- [ ] Example usage in comments

---

## Testing Strategy

### Unit Tests (Optional for Data Classes)
Since these are pure data classes, comprehensive unit tests are not required. However, can add:
- Serialization round-trip test
- Default value tests
- Equality tests

### Integration Tests
Will be covered in Task 09.

---

## Definition of Done

- [x] All files created in correct locations
- [x] Code compiles without errors or warnings
- [x] All acceptance criteria met
- [x] Serialization tested manually or with test
- [x] Code follows Kotlin conventions
- [x] KDoc comments added
- [x] Committed to version control
- [x] Peer reviewed (if applicable)
- [x] This checklist completed

---

## Time Tracking

| Activity | Estimated | Actual | Notes |
|----------|-----------|--------|-------|
| Reading & Planning | 15 min | | |
| Implementation | 30 min | | |
| Testing | 10 min | | |
| Documentation | 5 min | | |
| **Total** | **60 min** | | |

---

## Next Task

➡️ **Task 02: Implement Validation Logic**  
Once data models are created and verified, proceed to implement validation functions that will ensure question quality.

---

**Task Created:** December 4, 2025  
**Last Updated:** December 4, 2025  
**Version:** 1.0

