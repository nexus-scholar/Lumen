# Stage 10: Title/Abstract Screening

**Purpose:** Screen documents by title/abstract for relevance.

## Data Models

```kotlin
@Serializable
data class ScreeningDecision(
    val documentId: String,
    val reviewerId: String,
    val decision: Decision,
    val reason: ExclusionReason?,
    val notes: String?,
    val timestamp: Instant
)

@Serializable
enum class Decision {
    INCLUDE,
    EXCLUDE,
    MAYBE
}

@Serializable
enum class ExclusionReason {
    WRONG_POPULATION,
    WRONG_INTERVENTION,
    WRONG_OUTCOME,
    WRONG_STUDY_DESIGN,
    NOT_PRIMARY_RESEARCH,
    DUPLICATE,
    OTHER
}
```

## Implementation

Desktop UI with keyboard shortcuts for rapid screening. Supports dual-reviewer workflow with conflict resolution.

See full implementation in repo.

## Next Stage

→ [Stage 11: Full-Text Screening](stage-11-fulltext-screening.md)
