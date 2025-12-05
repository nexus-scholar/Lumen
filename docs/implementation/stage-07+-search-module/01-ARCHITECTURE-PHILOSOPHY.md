# 01. Architecture Philosophy & Strategy

## Core Mission
The Search Module is not merely a data retrieval tool; it is **context-building tooling for AI agents**. It is designed to act as the "sensory organ" for the Lumen application, converting chaotic global data into structured, actionable intelligence.

## Key Architectural Decisions

### 1. Federated API + Local Enrichment
We do not attempt to download the entire internet (e.g., full OpenAlex dumps).
*   **Global Discovery**: Relies on real-time API calls (OpenAlex, Crossref) for freshness.
*   **Local Persistence**: We only persist what the user interacts with.
*   **Rationale**: Balances storage constraints (Desktop app) with data freshness.

### 2. The "Discovery vs. Enrichment" Split
To optimize performance and cost, retrieval is split into two phases:
*   **Phase 1: Discovery (Lite)**
    *   **Goal**: Fast scanning (<300ms), deduplication, and UI rendering.
    *   **Data**: Title, Year, Authors, DOI, Venue.
    *   **Excludes**: Abstracts, References, Concepts (to save bandwidth).
*   **Phase 2: Enrichment (Deep)**
    *   **Trigger**: User interaction (click/select) or background prefetch.
    *   **Goal**: Full analysis context for AI.
    *   **Data**: Abstracts, Concepts, Citations, Raw JSON.

### 3. The "Sidecar" Pattern (Data Preservation)
**Problem**: Normalizing data into a strict schema leads to data loss (e.g., losing "Grant Numbers" because the UI doesn't have a field for it).
**Solution**: Every `ScholarlyDocument` carries a `rawSourceData: Map<String, Any>` field.
*   **UI**: Reads normalized fields.
*   **AI**: Can inspect the `rawSourceData` "Sidecar" to find hidden details specific to a provider.

### 4. Agentic Tooling First
The module is designed to be driven by AI:
*   **Structured Intent**: Queries are objects, not just strings.
*   **Probing**: A dedicated interface allows AI to "test" keywords (get counts/trends) without fetching documents, preventing hallucination.
*   **Feedback**: Providers return detailed error states, allowing AI to self-correct query syntax.
