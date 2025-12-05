# 05. The Research Probe (Idea Module Interface)

A specialized interface for the "Idea Generation" module. It decouples the *exploration of ideas* from the *retrieval of documents*.

## Purpose
To allow an AI Agent to "test the waters" (validate feasibility) without incurring the cost of downloading document lists.

## The Interface

```kotlin
interface ResearchProbe {
    /**
     * Returns "Signals" not Documents.
     * Used to validate PRISMA protocols.
     */
    suspend fun getSignalStrength(query: String): SignalMetrics
    
    suspend fun getTrendLine(query: String): Map<Year, Int>
}

data class SignalMetrics(
    val totalCount: Int,       // "Is there enough literature?"
    val isRising: Boolean,     // "Is this a hot topic?"
    val topConcepts: List<String> // "What keywords should I use?"
)
```

## Workflow (The Feasibility Loop)
1.  **Idea Module**: Generates a PICO question.
2.  **Probe**: Checks `getSignalStrength("AI in Surgery")`.
3.  **Result**: "15,000 papers" (Too broad).
4.  **Idea Module**: Refines query -> "Robotic Surgery RL".
5.  **Probe**: Checks again.
6.  **Result**: "400 papers" (Feasible).
