# 03. The Provider Contract

The `SearchProvider` interface is the "plug" for the system. It abstracts API complexity so the Orchestrator remains clean.

## Principles
1.  **Encapsulation**: Only the Provider knows the specific API syntax (e.g., OpenAlex filters).
2.  **Capabilities**: Providers declare what they *can* do, preventing invalid queries.
3.  **Resilience**: Errors are returned as sealed classes, not exceptions.

## Interface Definition

```kotlin
interface SearchProvider {
    val id: String // e.g., "openalex"
    val capabilities: Set<ProviderCapability>

    /**
     * Phase 1: Streamed Search
     * Must respect intent.mode (Lite vs Deep)
     */
    fun search(intent: SearchIntent): Flow<ProviderResult>

    /**
     * Phase 2: Direct Enrichment
     * Fetches full metadata for a known ID
     */
    suspend fun fetchDetails(id: String): ScholarlyDocument?

    /**
     * Tooling: Returns Query Translation
     * Allows AI to see exactly what syntax was sent to the API
     */
    fun debugQueryTranslation(intent: SearchIntent): String
}
```

## Recommended Provider Configurations

| Provider | Role | Phase 1 Fields (Lite) | Phase 2 Unique Value |
| :--- | :--- | :--- | :--- |
| **OpenAlex** | Primary | Title, Year, DOI, ID | Concepts, Citations, Abstract (Inverted Index) |
| **Semantic Scholar** | AI/Summary | Title, Year, ID | **TLDR** (AI Summary), Citation Velocity |
| **Crossref** | Validator | DOI, Date, Title | Accurate Dates, Funders |
| **ArXiv** | Pre-prints | Full Atom Feed | Version History |

## Error Handling
*   **Rate Limits**: Must return `Retry-After` headers if available.
*   **Timeouts**: Fail silently in `search()` stream, log warning.
