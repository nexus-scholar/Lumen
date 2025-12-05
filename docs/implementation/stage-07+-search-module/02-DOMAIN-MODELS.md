# 02. Domain Models & Data Structures

## The ScholarlyDocument
The "Truth" object passed throughout the application. It handles the "Swiss Cheese" problem (missing data) via nullable fields and fusion.

```kotlin
data class ScholarlyDocument(
    // --- IDENTITY ---
    val lumenId: String,        // Internal ID (e.g., "oa:W20034...")
    val doi: String?,           // Universal Key for Merging
    val sourceProvider: String, // Origin ("openalex", "crossref")

    // --- DISCOVERY LAYER (Phase 1) ---
    val title: String,
    val authors: List<Author>,
    val publicationYear: Int?,
    val venue: String?,         // Journal/Conference
    val citationCount: Int,
    val pdfUrl: String?,

    // --- ENRICHMENT LAYER (Phase 2) ---
    // Nullable = Not yet fetched
    val abstract: String? = null,
    val tldr: String? = null,   // AI Summary (Semantic Scholar)
    val concepts: List<Concept> = emptyList(),
    val references: List<String> = emptyList(), 

    // --- THE SIDECAR ---
    // Preserves the original JSON from the API.
    // Key = Provider ID, Value = Raw Object
    val rawSourceData: Map<String, Any?> = emptyMap(),

    // --- STATE FLAGS ---
    val isFullyHydrated: Boolean = false, // True if Phase 2 complete
    val retrievalConfidence: Double = 1.0
)
```

## The Search Intent
Encapsulates *what* the user (or AI) wants, avoiding string parsing in the engine.

```kotlin
data class SearchIntent(
    val query: String,
    val filters: SearchFilters = SearchFilters(),
    
    // CRITICAL: Determines payload size
    val mode: SearchMode = SearchMode.DISCOVERY, 
    
    // AI Context: Helps provider optimize queries
    // e.g., "medical_research" might trigger MeSH term expansion
    val domainContext: String? = null
)

enum class SearchMode { 
    DISCOVERY, // Fast, Lite fields
    ENRICHMENT // Deep fetch for specific IDs 
}
```

## The Fusion Strategy
When the same DOI is found in multiple providers:
1.  **Title**: Trust Crossref > OpenAlex
2.  **Abstract**: Trust OpenAlex > SemanticScholar
3.  **TLDR**: Trust SemanticScholar
4.  **Dates**: Trust Crossref
