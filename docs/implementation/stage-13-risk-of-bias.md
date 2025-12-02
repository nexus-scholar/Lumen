# Stage 13: Risk of Bias Assessment

**Purpose:** Assess methodological quality using RoB 2.0, QUADAS-2, etc.

## Data Models

```kotlin
@Serializable
data class RiskOfBiasAssessment(
    val documentId: String,
    val tool: RobTool,
    val domains: List<DomainAssessment>
)

@Serializable
enum class RobTool {
    ROB_2,
    ROBINS_I,
    QUADAS_2,
    NEWCASTLE_OTTAWA
}

@Serializable
data class DomainAssessment(
    val domain: String,
    val judgment: RiskJudgment,
    val support: String
)

@Serializable
enum class RiskJudgment {
    LOW,
    SOME_CONCERNS,
    HIGH
}
```

## Implementation

Guided RoB assessment with traffic-light visualizations.

See full implementation in repo.

## Next Stage

→ [Stage 14: Synthesis & Analytics](stage-14-synthesis-analytics.md)
