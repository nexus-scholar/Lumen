# Stage 11: Full-Text Screening

**Purpose:** Assess full-text articles for final inclusion.

## Data Models

```kotlin
@Serializable
data class FullTextAssessment(
    val documentId: String,
    val retrievalStatus: RetrievalStatus,
    val pdfPath: String?,
    val decision: Decision,
    val exclusionReasons: List<ExclusionReason>,
    val notes: String?
)

@Serializable
enum class RetrievalStatus {
    RETRIEVED,
    NOT_FOUND,
    PAYWALLED,
    WRONG_LANGUAGE,
    PENDING
}
```

## Implementation

PDF viewer with highlighting and annotation support (Phase 3).

See full implementation in repo.

## Next Stage

→ [Stage 12: Data Extraction](stage-12-data-extraction.md)
