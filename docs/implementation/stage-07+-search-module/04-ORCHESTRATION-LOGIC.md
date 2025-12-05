# 04. Orchestration & Governance

The Orchestrator is the "Brain" of the module. It manages parallel execution, merging, and resource usage.

## The Pipeline Logic
1.  **Input**: Receives `SearchIntent`.
2.  **Selection**: Filters providers based on `intent` and `capabilities`.
3.  **Governance Check**: Checks `ResourceGovernor` for API token budget.
4.  **Parallel Execution**: Launches a coroutine for each provider.
5.  **Normalization**: Converts raw API responses to `ScholarlyDocument`.
6.  **Streaming**: Emits results via `Flow` as they arrive.
7.  **Background Prefetch**: (Optional) Triggers Enrichment for top 3 results.

## The Resource Governor (Safety Layer)
Prevents the AI or User from banning the IP address.

```kotlin
interface ResourceGovernor {
    // Returns true if budget allows
    suspend fun requestPermit(providerId: String, cost: RequestCost): Boolean
    
    // Usage tracking
    fun recordUsage(providerId: String, tokens: Int)
}
```

## The "Invisible Hand" UX
*   **Default**: Always perform **Lite Search**.
*   **Hover**: Trigger **Background Fetch** for abstract/TLDR.
*   **Select**: Trigger **Full Hydration** (Phase 2) for storage.
