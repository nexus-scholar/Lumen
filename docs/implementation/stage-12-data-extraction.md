# Stage 12: Data Extraction

**Purpose:** Extract structured data from included studies.

## Data Models

```kotlin
@Serializable
data class ExtractionTemplate(
    val name: String,
    val fields: List<ExtractionField>
)

@Serializable
data class ExtractionField(
    val id: String,
    val label: String,
    val type: FieldType,
    val required: Boolean,
    val options: List<String>? = null
)

@Serializable
enum class FieldType {
    TEXT,
    NUMBER,
    DATE,
    SELECT,
    MULTI_SELECT
}
```

## Implementation

Template-based extraction forms. Future: LLM-assisted extraction.

See full implementation in repo.

## Next Stage

→ [Stage 13: Risk of Bias](stage-13-risk-of-bias.md)
