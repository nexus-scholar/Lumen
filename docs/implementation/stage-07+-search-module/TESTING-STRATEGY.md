# Search Module Testing Strategy

## Overview

This document outlines a comprehensive testing strategy for the Lumen Search Module to achieve production and research-grade quality. The goal is **≥85% code coverage** with robust tests covering unit, integration, and end-to-end scenarios.

---

## 1. Testing Architecture

### 1.1 Test Categories

| Category | Location | Purpose | Coverage Target |
|----------|----------|---------|-----------------|
| **Unit Tests** | `commonTest` | Pure logic, domain models, value objects | 95% |
| **Integration Tests** | `jvmTest` | Provider mappings, HTTP mocking, DI wiring | 85% |
| **Contract Tests** | `jvmTest` | API response parsing, schema validation | 100% of providers |
| **Live Smoke Tests** | `jvmTest` (tagged) | Optional real API verification | N/A (manual) |

### 1.2 Test Infrastructure

```
src/
├── commonTest/kotlin/com/lumen/search/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── ScholarlyDocumentTest.kt
│   │   │   ├── AuthorTest.kt
│   │   │   ├── SearchIntentTest.kt
│   │   │   └── SearchFiltersTest.kt
│   │   ├── valueobjects/
│   │   │   ├── DOITest.kt
│   │   │   └── YearRangeTest.kt
│   │   └── ports/
│   │       ├── ProviderResultTest.kt
│   │       └── SearchStatisticsTest.kt
│   └── data/
│       └── engine/
│           ├── ResultMergerTest.kt
│           └── OrchestratorConfigTest.kt
│
└── jvmTest/kotlin/com/lumen/search/
    ├── fixtures/
    │   ├── OpenAlexFixtures.kt
    │   ├── SemanticScholarFixtures.kt
    │   ├── CrossrefFixtures.kt
    │   ├── ArxivFixtures.kt
    │   └── TestSearchIntents.kt
    ├── testutils/
    │   ├── MockEngineFactory.kt
    │   ├── TestHttpClientProvider.kt
    │   └── SearchTestBase.kt
    ├── providers/
    │   ├── openalex/
    │   │   ├── OpenAlexSearchProviderTest.kt
    │   │   └── OpenAlexMappingTest.kt
    │   ├── semanticscholar/
    │   │   ├── SemanticScholarProviderTest.kt
    │   │   └── SemanticScholarMappingTest.kt
    │   ├── crossref/
    │   │   ├── CrossrefProviderTest.kt
    │   │   └── CrossrefMappingTest.kt
    │   └── arxiv/
    │       ├── ArxivProviderTest.kt
    │       └── ArxivMappingTest.kt
    ├── engine/
    │   ├── SearchOrchestratorTest.kt
    │   └── ResultMergerIntegrationTest.kt
    ├── governance/
    │   ├── ResourceGovernorTest.kt
    │   └── TokenBucketTest.kt
    ├── api/
    │   ├── SearchClientTest.kt
    │   └── ProbeClientTest.kt
    ├── adapter/
    │   └── LegacySearchAdapterTest.kt
    ├── di/
    │   └── SearchModuleTest.kt
    └── live/
        └── LiveSmokeTests.kt  (tagged: @Tag("live"))
```

---

## 2. Unit Tests (commonTest)

### 2.1 Domain Models

#### `ScholarlyDocumentTest.kt`
- **Serialization round-trip**: Serialize → Deserialize → Assert equality
- **Default values**: Verify defaults (empty lists, false flags, 1.0 confidence)
- **Sidecar preservation**: Raw JSON objects correctly stored/retrieved
- **Hydration state**: `isFullyHydrated` flag semantics
- **Merge tracking**: `mergedFromIds` list management

#### `AuthorTest.kt`
- **Name parsing**: Handle various formats (first/last, initials, single name)
- **ORCID validation**: Valid/invalid ORCID patterns
- **Equality**: Same author from different providers matches

#### `SearchIntentTest.kt`
- **Builder pattern**: Fluent construction
- **Filter combination**: Multiple filters applied correctly
- **Mode switching**: DISCOVERY vs ENRICHMENT defaults
- **Validation**: Reject empty queries, negative offsets

#### `SearchFiltersTest.kt`
- **Year range**: Validate boundaries, single year, open-ended
- **Document types**: Enum mapping, case insensitivity
- **Boolean flags**: `openAccessOnly`, `hasPdf` combinations

### 2.2 Value Objects

#### `DOITest.kt`
- **Normalization**: Case normalization, prefix stripping (https://doi.org/)
- **Validation**: Pattern matching for valid DOIs
- **Equality**: Same DOI in different formats equals
- **Edge cases**: Malformed DOIs, empty strings

#### `YearRangeTest.kt`
- **Construction**: Valid ranges (2000..2024)
- **Validation**: Reject inverted ranges, future years
- **Contains**: Year-in-range checking
- **Open-ended**: Start-only or end-only ranges

### 2.3 Engine Logic

#### `ResultMergerTest.kt`
- **Trust hierarchy**: Title from Crossref beats OpenAlex
- **Abstract selection**: OpenAlex abstract preferred
- **TLDR preservation**: Only from Semantic Scholar
- **Author merging**: Deduplication by name
- **Citation count**: Maximum value wins
- **Sidecar merging**: All raw data preserved
- **Null handling**: Graceful null propagation
- **Same-provider merge**: Handles duplicate from same source

---

## 3. Integration Tests (jvmTest)

### 3.1 Test Fixtures Strategy

Each provider gets:
1. **Success fixtures**: Typical API responses with complete data
2. **Minimal fixtures**: Edge case with missing optional fields
3. **Error fixtures**: 4xx/5xx responses
4. **Empty fixtures**: Valid response with 0 results

```kotlin
// Example: OpenAlexFixtures.kt
object OpenAlexFixtures {
    val TYPICAL_WORK_RESPONSE = """
    {
        "id": "https://openalex.org/W2741809807",
        "doi": "https://doi.org/10.1038/s41586-019-1666-5",
        "title": "The effect of...",
        "publication_year": 2019,
        ...
    }
    """.trimIndent()
    
    val MINIMAL_WORK_RESPONSE = """{ "id": "...", "title": "Untitled" }"""
    
    val SEARCH_RESPONSE_PAGE_1 = loadFixture("openalex/search-page1.json")
    val SEARCH_RESPONSE_EMPTY = """{ "meta": { "count": 0 }, "results": [] }"""
}
```

### 3.2 MockEngine Setup

```kotlin
// MockEngineFactory.kt
object MockEngineFactory {
    fun createForOpenAlex(
        searchResponse: String = OpenAlexFixtures.SEARCH_RESPONSE_PAGE_1,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.host == "api.openalex.org" &&
                        request.url.encodedPath.startsWith("/works") -> {
                            respond(
                                content = searchResponse,
                                status = statusCode,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        else -> error("Unhandled: ${request.url}")
                    }
                }
            }
        }
    }
}
```

### 3.3 Provider Tests

Each provider implementation gets:

#### `OpenAlexSearchProviderTest.kt`
```kotlin
class OpenAlexSearchProviderTest {
    
    @Test
    fun `search returns documents for valid query`() = runTest {
        val client = MockEngineFactory.createForOpenAlex()
        val provider = OpenAlexSearchProvider(client)
        
        val results = provider.search(TestSearchIntents.DIABETES_METFORMIN).toList()
        
        assertThat(results).hasSize(1)
        assertThat(results.first()).isInstanceOf(ProviderResult.Success::class)
        val success = results.first() as ProviderResult.Success
        assertThat(success.documents).isNotEmpty()
        assertThat(success.documents.first().sourceProvider).isEqualTo("openalex")
    }
    
    @Test
    fun `handles 429 rate limit with retry header`() = runTest {
        val client = MockEngineFactory.createForOpenAlex(
            statusCode = HttpStatusCode.TooManyRequests,
            headers = headersOf("Retry-After" to "5")
        )
        val provider = OpenAlexSearchProvider(client)
        
        val results = provider.search(TestSearchIntents.DIABETES_METFORMIN).toList()
        
        val error = results.first() as ProviderResult.Error
        assertThat(error.canRetry).isTrue()
        assertThat(error.retryAfterMs).isEqualTo(5000L)
    }
    
    @Test
    fun `fetchDetails returns fully hydrated document`() = runTest { ... }
    
    @Test
    fun `getStats returns year distribution`() = runTest { ... }
    
    @Test
    fun `maps all OpenAlex fields correctly`() = runTest { ... }
    
    @Test
    fun `preserves raw JSON in sidecar`() = runTest { ... }
    
    @Test
    fun `handles missing optional fields gracefully`() = runTest { ... }
}
```

#### Similar test classes for:
- `SemanticScholarProviderTest.kt`
- `CrossrefProviderTest.kt`
- `ArxivProviderTest.kt` (XML parsing specifics)

### 3.4 Orchestrator Tests

#### `SearchOrchestratorTest.kt`
```kotlin
class SearchOrchestratorTest {

    @Test
    fun `executes search across multiple providers in parallel`() = runTest {
        val mockProviders = listOf(
            FakeSearchProvider("openalex", delay = 100.milliseconds),
            FakeSearchProvider("semanticscholar", delay = 150.milliseconds)
        )
        val orchestrator = SearchOrchestrator(
            providers = mockProviders,
            governor = ResourceGovernor()
        )
        
        val startTime = TimeSource.Monotonic.markNow()
        val results = orchestrator.executeSearch(TestSearchIntents.SIMPLE).toList()
        val elapsed = startTime.elapsedNow()
        
        // Parallel: should take ~150ms not 250ms
        assertThat(elapsed).isLessThan(200.milliseconds)
        assertThat(results).hasSize(2)
    }
    
    @Test
    fun `deduplicates documents by DOI`() = runTest { ... }
    
    @Test
    fun `merges documents from different providers`() = runTest { ... }
    
    @Test
    fun `skips providers exceeding rate limits`() = runTest { ... }
    
    @Test
    fun `handles provider failures gracefully in discovery mode`() = runTest { ... }
    
    @Test
    fun `retries enrichment failures up to config limit`() = runTest { ... }
    
    @Test
    fun `streams results immediately for responsive UI`() = runTest { ... }
    
    @Test
    fun `respects search intent mode for provider selection`() = runTest { ... }
}
```

### 3.5 Governance Tests

#### `ResourceGovernorTest.kt`
```kotlin
class ResourceGovernorTest {
    
    @Test
    fun `respects per-second rate limits`() = runTest { ... }
    
    @Test
    fun `allows burst up to capacity`() = runTest { ... }
    
    @Test
    fun `tracks daily usage`() = runTest { ... }
    
    @Test
    fun `rejects requests when daily limit exhausted`() = runTest { ... }
    
    @Test
    fun `resets counters on daily reset`() = runTest { ... }
    
    @Test
    fun `provides accurate usage statistics`() = runTest { ... }
}

class TokenBucketTest {
    
    @Test
    fun `refills at configured rate`() = runTest { ... }
    
    @Test
    fun `does not exceed max capacity`() = runTest { ... }
    
    @Test
    fun `acquire blocks when empty`() = runTest { ... }
}
```

### 3.6 API Layer Tests

#### `SearchClientTest.kt`
```kotlin
class SearchClientTest {
    
    @Test
    fun `search returns flow of documents`() = runTest { ... }
    
    @Test
    fun `enrich returns hydrated document`() = runTest { ... }
    
    @Test
    fun `exposes usage statistics`() = runTest { ... }
}

class ProbeClientTest {
    
    @Test
    fun `getSignalStrength returns valid range`() = runTest { ... }
    
    @Test
    fun `getTrendLine returns year distribution`() = runTest { ... }
}
```

### 3.7 DI Module Tests

#### `SearchModuleTest.kt`
```kotlin
class SearchModuleTest {
    
    @Test
    fun `all dependencies resolve correctly`() {
        startKoin {
            modules(searchModule)
        }
        
        val client: SearchClient = get()
        val probeClient: ProbeClient = get()
        val orchestrator: SearchOrchestrator = get()
        
        assertThat(client).isNotNull()
        assertThat(probeClient).isNotNull()
        assertThat(orchestrator).isNotNull()
        
        stopKoin()
    }
    
    @Test
    fun `httpClient is shared singleton`() { ... }
    
    @Test
    fun `providers are correctly injected`() { ... }
}
```

### 3.8 Adapter Tests

#### `LegacySearchAdapterTest.kt`
```kotlin
class LegacySearchAdapterTest {
    
    @Test
    fun `converts ScholarlyDocument to legacy Document`() { ... }
    
    @Test
    fun `converts legacy Document to ScholarlyDocument`() { ... }
    
    @Test
    fun `preserves all mapped fields`() { ... }
    
    @Test
    fun `handles null fields in both directions`() { ... }
}
```

---

## 4. Contract Tests

### 4.1 Purpose
Validate that our parsing code handles **real API response structures**.

### 4.2 Fixture Files
Store real (anonymized) API responses:
```
src/jvmTest/resources/fixtures/
├── openalex/
│   ├── work-complete.json
│   ├── work-minimal.json
│   ├── search-results.json
│   └── stats-response.json
├── semanticscholar/
│   ├── paper-complete.json
│   ├── search-results.json
│   └── batch-details.json
├── crossref/
│   ├── work-complete.json
│   └── search-results.json
└── arxiv/
    ├── atom-feed.xml
    └── single-entry.xml
```

### 4.3 Contract Test Pattern
```kotlin
@Tag("contract")
class OpenAlexContractTest {
    
    @ParameterizedTest
    @ValueSource(strings = ["work-complete.json", "work-minimal.json"])
    fun `parses real OpenAlex responses without exceptions`(fixture: String) {
        val json = loadFixture("openalex/$fixture")
        
        val result = OpenAlexMapper.mapWork(json)
        
        assertThat(result).isNotNull()
        assertThat(result.lumenId).startsWith("oa:")
    }
}
```

---

## 5. Live Smoke Tests

### 5.1 Purpose
Optional tests that hit real APIs to catch breaking changes.

### 5.2 Setup
```kotlin
@Tag("live")
@EnabledIfEnvironmentVariable(named = "LUMEN_LIVE_TESTS", matches = "true")
class LiveSmokeTests {
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }
    
    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `OpenAlex API responds with expected schema`() = runBlocking {
        val provider = OpenAlexSearchProvider(httpClient)
        val results = provider.search(
            SearchIntent(query = "machine learning", maxResults = 1)
        ).take(1).toList()
        
        assertThat(results).isNotEmpty()
        assertThat(results.first()).isInstanceOf(ProviderResult.Success::class)
    }
    
    // Similar for other providers...
}
```

### 5.3 Running Live Tests
```bash
# Not run by default
./gradlew jvmTest

# Opt-in live tests
LUMEN_LIVE_TESTS=true ./gradlew jvmTest -PincludeTags=live
```

---

## 6. Test Execution Configuration

### 6.1 Gradle Configuration
```kotlin
// build.gradle.kts
tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        excludeTags("live")  // Exclude live tests by default
    }
}

// Optional: Add a separate task for live tests
tasks.register<Test>("liveTest") {
    useJUnitPlatform {
        includeTags("live")
    }
    environment("LUMEN_LIVE_TESTS", "true")
}
```

### 6.2 Dependencies to Add
```kotlin
// build.gradle.kts - jvmTest dependencies
val jvmTest by getting {
    dependencies {
        implementation(kotlin("test-junit5"))
        implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
        implementation("io.mockk:mockk:$mockkVersion")
        implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        implementation("io.kotest:kotest-assertions-core:$kotestVersion")
        implementation("io.ktor:ktor-client-mock:$ktorVersion")  // ADD THIS
        implementation("com.h2database:h2:2.2.224")
    }
}
```

---

## 7. Coverage Goals

### 7.1 Metrics Targets

| Component | Line Coverage | Branch Coverage |
|-----------|---------------|-----------------|
| `domain/models` | 95% | 90% |
| `domain/valueobjects` | 100% | 100% |
| `domain/ports` | 90% | 85% |
| `data/providers/*` | 85% | 80% |
| `data/engine` | 90% | 85% |
| `data/governance` | 95% | 90% |
| `api/` | 85% | 80% |
| `adapter/` | 90% | 85% |
| `di/` | 80% | 75% |
| **Overall Module** | **≥85%** | **≥80%** |

### 7.2 Coverage Report
```bash
./gradlew koverHtmlReport
# Report at: build/reports/kover/html/index.html
```

---

## 8. Implementation Phases

### Phase 1: Foundation (Week 1)
1. ✅ Set up test directory structure
2. ✅ Add `ktor-client-mock` dependency
3. ✅ Create base test utilities (`MockEngineFactory`, fixtures)
4. ✅ Implement domain model unit tests
5. ✅ Implement value object tests

### Phase 2: Provider Tests (Week 2)
1. ✅ Create JSON fixtures for each provider
2. ✅ OpenAlexSearchProvider tests
3. ✅ SemanticScholarProvider tests
4. ✅ CrossrefProvider tests
5. ✅ ArxivProvider tests (XML parsing)

### Phase 3: Engine & Governance (Week 3)
1. ✅ ResultMerger tests (trust hierarchy)
2. ✅ SearchOrchestrator tests (parallelism, streaming)
3. ✅ ResourceGovernor tests (rate limiting)
4. ✅ TokenBucket tests

### Phase 4: Integration & Polish (Week 4)
1. ✅ API layer tests
2. ✅ DI module tests
3. ✅ LegacySearchAdapter tests
4. ✅ Contract tests with real fixtures
5. ✅ Live smoke tests (optional)
6. ✅ Coverage report analysis and gap filling

---

## 9. Test Quality Guidelines

### 9.1 Naming Convention
```kotlin
@Test
fun `method under test - scenario - expected outcome`() { ... }

// Examples:
fun `search - with valid query - returns documents`() { ... }
fun `search - with rate limit exceeded - returns error with retry`() { ... }
fun `merge - documents from same provider - keeps higher citation count`() { ... }
```

### 9.2 Test Structure (AAA Pattern)
```kotlin
@Test
fun `example test`() = runTest {
    // Arrange
    val mockClient = MockEngineFactory.createForOpenAlex()
    val provider = OpenAlexSearchProvider(mockClient)
    val intent = SearchIntent(query = "machine learning")
    
    // Act
    val results = provider.search(intent).toList()
    
    // Assert
    assertThat(results).hasSize(1)
    assertThat(results.first()).isInstanceOf(ProviderResult.Success::class)
}
```

### 9.3 Test Independence
- Each test must be self-contained
- No shared mutable state between tests
- Use `@BeforeEach` for common setup
- Clean up resources in `@AfterEach`

### 9.4 Assertion Library
Use Kotest assertions for expressive tests:
```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf

result shouldBe ProviderResult.Success::class
documents shouldHaveSize 10
error.shouldBeInstanceOf<ProviderResult.Error>()
```

---

## 10. CI/CD Integration

### 10.1 GitHub Actions Workflow
```yaml
name: Test Search Module

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Unit Tests
        run: ./gradlew jvmTest
      
      - name: Generate Coverage Report
        run: ./gradlew koverHtmlReport
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: build/reports/kover/report.xml
```

### 10.2 Coverage Gate
Fail CI if coverage drops below threshold:
```kotlin
// build.gradle.kts
kover {
    verify {
        rule {
            bound {
                minValue = 80
                valueType = CoverageValueType.LINE_PERCENTAGE
            }
        }
    }
}
```

---

## 11. Appendix: Test Data Examples

### 11.1 Sample SearchIntent
```kotlin
object TestSearchIntents {
    val DIABETES_METFORMIN = SearchIntent(
        query = "metformin type 2 diabetes glucose",
        mode = SearchMode.DISCOVERY,
        maxResults = 25,
        offset = 0,
        filters = SearchFilters(
            yearRange = YearRange(2018, 2024),
            documentTypes = listOf(DocumentType.JOURNAL_ARTICLE),
            openAccessOnly = false,
            hasPdf = false
        )
    )
    
    val SIMPLE = SearchIntent(query = "machine learning")
    
    val WITH_ALL_FILTERS = SearchIntent(
        query = "cancer treatment",
        filters = SearchFilters(
            yearRange = YearRange(2020, 2024),
            documentTypes = listOf(DocumentType.JOURNAL_ARTICLE, DocumentType.REVIEW),
            venues = listOf("Nature", "Science"),
            openAccessOnly = true,
            hasPdf = true
        )
    )
}
```

### 11.2 Sample Document for Assertions
```kotlin
object TestDocuments {
    val COMPLETE_DOCUMENT = ScholarlyDocument(
        lumenId = "oa:W2741809807",
        doi = "10.1038/s41586-019-1666-5",
        sourceProvider = "openalex",
        title = "The effect of metformin on type 2 diabetes",
        authors = listOf(
            Author(name = "Jane Doe", orcid = "0000-0001-2345-6789")
        ),
        publicationYear = 2019,
        venue = "Nature",
        citationCount = 150,
        pdfUrl = "https://example.com/paper.pdf",
        abstract = "Background: Metformin is...",
        tldr = null,
        concepts = listOf(Concept(name = "Diabetes", relevance = 0.95)),
        rawSourceData = emptyMap(),
        isFullyHydrated = true
    )
}
```

---

## Summary

This testing strategy ensures the Search Module is:

1. **Reliable**: Comprehensive unit tests catch logic errors
2. **Resilient**: Integration tests validate error handling
3. **Compatible**: Contract tests prevent API breaking changes
4. **Performant**: Tests verify parallel execution and streaming
5. **Maintainable**: Clear structure and naming conventions
6. **Measurable**: Coverage metrics with CI enforcement

**Next Steps**: Start implementing tests following Phase 1 of the implementation plan.

