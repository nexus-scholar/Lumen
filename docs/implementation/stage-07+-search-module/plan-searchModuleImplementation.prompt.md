# Plan: Implement Lumen Search Module (Stage 07+)

Create a standalone `search` module with the new architecture (ScholarlyDocument, Sidecar, Discovery/Enrichment, Orchestration). Uses shared HttpClient via Koin DI, mock-first testing, and an adapter layer for gradual migration of existing stages.

## Steps

1. **Create search module package structure** — Add new package hierarchy under `src/commonMain/kotlin/com/lumen/search/` with subfolders: `domain/models`, `domain/ports`, `domain/valueobjects`, `data/engine`, `data/governance`, `data/providers/{openalex,semanticscholar,crossref,arxiv}`, `data/mappers`, and `api`.

2. **Implement domain models** — Create `ScholarlyDocument.kt`, `Author.kt`, `Concept.kt`, `SearchIntent.kt`, `SearchMode.kt`, and value objects (`DOI.kt`, `YearRange.kt`) in `domain/models`; include Sidecar field (`rawSourceData: Map<String, Any?>`), hydration state flags, and `@Serializable` annotations.

3. **Define provider interfaces** — Create `SearchProvider.kt` interface in `domain/ports` with `id`, `capabilities`, `search(intent): Flow<ProviderResult>`, `fetchDetails(id)`, `debugQueryTranslation(intent)`; add `ResearchProbe.kt` interface with `getSignalStrength()` and `getTrendLine()` for AI probing; create `ProviderResult` sealed class and `ProviderCapability` enum.

4. **Implement providers** — Create new `OpenAlexSearchProvider`, `SemanticScholarProvider`, `CrossrefProvider`, `ArxivProvider` in new module using shared `HttpClient` from DI; each maps API responses → `ScholarlyDocument` with raw JSON attached to Sidecar; ArXiv uses lightweight XML parsing.

5. **Build orchestration layer** — Create `SearchOrchestrator.kt` in `data/engine` with parallel coroutine execution via `channelFlow`, provider selection by capabilities, streaming results; implement `ResultMerger.kt` with fusion priority logic (Crossref dates > OpenAlex abstract > SemanticScholar TLDR); add `ResourceGovernor.kt` in `data/governance` with per-provider `TokenBucket` rate limiters.

6. **Expose public API & DI** — Create `SearchClient.kt` and `ProbeClient.kt` in `api/` package; add Koin module `SearchModule.kt` in `search/di/` injecting shared `HttpClient` and wiring all components.

7. **Add adapter for existing stages** — Create `LegacySearchAdapter.kt` implementing old `SearchProvider` interface, converting `ScholarlyDocument` ↔ `Document`; mark old interface `@Deprecated`; update `SearchExecutionStage` and `TestAndRefineStage` to use adapter.

8. **Implement tests with MockEngine** — Add `ktor-client-mock` dependency; create JSON/XML fixtures; unit test provider mappings, `ResultMerger` fusion, `SearchOrchestrator` parallelism; add `@Tag("live")` optional smoke tests with low request limits.

## Further Considerations

1. **Error resilience** — Provider failures emit `ProviderResult.Error` in stream; Discovery mode skips failures silently (log only), Enrichment mode retries up to 3 times before surfacing error.

2. **ArXiv XML parsing** — Use `kotlinx-serialization` with a thin XML wrapper or `xmlutil` library for Atom feed parsing.

3. **Old model deprecation** — Mark `Document` and old `SearchProvider` as `@Deprecated` after adapter is stable; plan removal for next major version.

## Decisions Made

- **Migration strategy**: Create new module, adapt existing stages incrementally via `LegacySearchAdapter`.
- **HTTP client sharing**: Shared client via Koin DI for connection pooling.
- **Testing approach**: Mock-first with Ktor MockEngine, plus optional `@Tag("live")` smoke tests against real APIs.

## Reference Documents

- [00-IMPLEMENTATION-INDEX.md](./00-IMPLEMENTATION-INDEX.md) — High-level component diagram and module structure
- [01-ARCHITECTURE-PHILOSOPHY.md](./01-ARCHITECTURE-PHILOSOPHY.md) — Core design decisions (federated API, Discovery vs. Enrichment, Sidecar pattern)
- [02-DOMAIN-MODELS.md](./02-DOMAIN-MODELS.md) — ScholarlyDocument, SearchIntent, fusion strategy
- [03-PROVIDER-CONTRACT.md](./03-PROVIDER-CONTRACT.md) — SearchProvider interface, capabilities, error handling
- [04-ORCHESTRATION-LOGIC.md](./04-ORCHESTRATION-LOGIC.md) — Orchestrator pipeline, ResourceGovernor, UX patterns
- [05-RESEARCH-PROBE.md](./05-RESEARCH-PROBE.md) — ResearchProbe interface for AI feasibility validation

