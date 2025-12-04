# Task 06: Register in DI Container

**Status:** ⬜ Not Started  
**Priority:** Critical  
**Estimated Effort:** 30 minutes  
**Dependencies:** Task 04 (Pipeline Stage)  
**Assignee:** _____

---

## Objective

Register `ResearchQuestionsStage` in the Koin dependency injection container so it can be instantiated throughout the application.

---

## Implementation

### Modify JvmModule.kt

**File:** `src/jvmMain/kotlin/com/lumen/core/di/JvmModule.kt`

Add after `PicoExtractionStage` registration (around line 140):

```kotlin
factory {
    ResearchQuestionsStage(
        llmService = get(),
        artifactStore = get()
    )
}
```

---

## Complete Code Change

```kotlin
// ...existing factories...

factory {
    PicoExtractionStage(
        llmService = get(),
        artifactStore = get()
    )
}

// ADD THIS:
factory {
    ResearchQuestionsStage(
        llmService = get(),
        artifactStore = get()
    )
}

factory {
    ConceptExpansionStage(
        llmService = get(),
        artifactStore = get()
    )
}

// ...rest of file...
```

---

## Checklist

- [ ] Open `JvmModule.kt`
- [ ] Import `ResearchQuestionsStage`
- [ ] Add factory registration
- [ ] Build project to verify
- [ ] Test DI injection works

---

## Verification

### Test DI Injection

```kotlin
@Test
fun `can inject ResearchQuestionsStage from Koin`() {
    startKoin {
        modules(jvmModule, coreModule)
    }
    
    val stage = get<ResearchQuestionsStage>()
    assertNotNull(stage)
    assertEquals("Stage 2: Research Questions Generation", stage.stageName)
    
    stopKoin()
}
```

### Manual Test in App

```kotlin
// In ProjectDetailScreen.kt or similar
private object StageKoinHelper : KoinComponent {
    fun getQuestionsStage(): ResearchQuestionsStage {
        return get<ResearchQuestionsStage>()
    }
}
```

---

## Acceptance Criteria

✅ Factory registered in JvmModule  
✅ Dependencies (llmService, artifactStore) correctly injected  
✅ No DI errors on app startup  
✅ Stage can be retrieved with `get<ResearchQuestionsStage>()`  
✅ Build succeeds with no warnings  

---

## Time: 30 minutes

**Next:** ➡️ Task 07: Add UI Stage Card

