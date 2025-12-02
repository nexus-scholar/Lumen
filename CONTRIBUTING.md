# Contributing to Lumen

Thank you for your interest in contributing to Lumen! This project is built by researchers, for researchers, and we welcome contributions from the community.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [How Can I Contribute?](#how-can-i-contribute)
3. [Development Setup](#development-setup)
4. [Pull Request Process](#pull-request-process)
5. [Coding Standards](#coding-standards)
6. [Documentation](#documentation)
7. [Testing](#testing)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of experience level, background, or identity.

### Expected Behavior

- Be respectful and considerate
- Use welcoming and inclusive language
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- Harassment, discrimination, or derogatory comments
- Trolling, insulting, or personal attacks
- Publishing others' private information without permission
- Any conduct that would be considered inappropriate in a professional setting

---

## How Can I Contribute?

### Reporting Bugs

**Before submitting a bug report:**
- Check the [existing issues](https://github.com/nexus-scholar/Lumen/issues) to avoid duplicates
- Update to the latest version to see if the issue persists
- Collect relevant information (error messages, logs, screenshots)

**Bug Report Template:**
```markdown
### Description
Clear description of the bug

### Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Observe error

### Expected Behavior
What should happen

### Actual Behavior
What actually happens

### Environment
- OS: [e.g., Windows 11, macOS 14]
- Lumen Version: [e.g., v0.1.0]
- Kotlin Version: [e.g., 2.1.0]

### Screenshots/Logs
Attach relevant media
```

### Suggesting Enhancements

**Enhancement Request Template:**
```markdown
### Feature Description
Clear description of the proposed feature

### Use Case
Why this feature is needed (provide examples)

### Proposed Solution
How you envision this working

### Alternatives Considered
Other approaches you've thought about

### PRISMA Compliance
How does this relate to PRISMA 2020 requirements?
```

### Contributing Code

We welcome contributions in these areas:

#### 1. **Core Pipeline (High Priority)**
- Implementing pipeline stages (0-15)
- LLM integration (anti-hallucination validation)
- Database query generation
- Search API integrations (OpenAlex, Crossref, etc.)

#### 2. **UI/UX (Medium Priority)**
- Compose Multiplatform desktop app
- Screening interfaces (title/abstract, full-text)
- Data extraction forms
- PRISMA flowchart visualization

#### 3. **Analytics & Visualization (Medium Priority)**
- Citation network graphs (JGraphT)
- LLM-powered narrative synthesis
- Meta-analysis integration (R scripts)
- Study design analysis

#### 4. **Infrastructure (Low Priority)**
- CI/CD pipeline setup
- Docker containerization
- Cloud sync (PostgreSQL)
- Multi-user collaboration

#### 5. **Documentation (Always Welcome)**
- Code comments
- User guides
- API documentation
- Video tutorials

---

## Development Setup

### Prerequisites

- **JDK 17 or higher** (recommended: JDK 21)
- **Kotlin 2.1.0+**
- **Gradle 8.5+** (wrapper included)
- **Git**
- **IDE:** IntelliJ IDEA (recommended) or Android Studio

### Clone Repository

```bash
git clone https://github.com/nexus-scholar/Lumen.git
cd Lumen
```

### Install Dependencies

```bash
# Gradle wrapper handles dependencies
./gradlew build
```

### Project Structure

```
Lumen/
├── buildSrc/                 # Build configuration
├── common/                   # Shared KMP code
│   ├── src/commonMain/       # Platform-agnostic code
│   ├── src/jvmMain/          # JVM-specific
│   └── src/jsMain/           # JS-specific
├── desktop/                  # Compose Multiplatform desktop app
├── cli/                      # Command-line tool
├── web/                      # Kotlin/JS web app
├── docs/                     # Documentation
├── gradle/                   # Gradle wrapper
└── build.gradle.kts          # Root build file
```

### Run Development Server

```bash
# Desktop app
./gradlew :desktop:run

# CLI tool
./gradlew :cli:run --args="start --help"

# Web app (dev server)
./gradlew :web:jsBrowserDevelopmentRun
```

### Run Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :common:test

# With coverage
./gradlew test jacocoTestReport
```

---

## Pull Request Process

### 1. Fork & Branch

```bash
# Fork the repo on GitHub, then:
git clone https://github.com/YOUR_USERNAME/Lumen.git
cd Lumen

# Create feature branch
git checkout -b feature/your-feature-name
# OR
git checkout -b fix/bug-description
```

### 2. Make Changes

- Write clean, readable code
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Add tests for new features
- Update documentation

### 3. Commit

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```bash
# Feature
git commit -m "feat(stage4): add anti-hallucination query validation"

# Bug fix
git commit -m "fix(screening): resolve duplicate detection false positives"

# Documentation
git commit -m "docs(readme): update installation instructions"

# Types: feat, fix, docs, style, refactor, test, chore
```

### 4. Push & Create PR

```bash
git push origin feature/your-feature-name
```

Then open a Pull Request on GitHub with:

**PR Template:**
```markdown
### Description
Clear description of changes

### Related Issue
Closes #123

### Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

### Testing
- [ ] All tests pass locally
- [ ] Added new tests for changes
- [ ] Manual testing completed

### Checklist
- [ ] Code follows project style guidelines
- [ ] Self-reviewed the code
- [ ] Commented complex logic
- [ ] Updated documentation
- [ ] No new warnings introduced
```

### 5. Code Review

- Address reviewer feedback promptly
- Keep PRs focused (one feature/fix per PR)
- Squash commits before merge if requested

---

## Coding Standards

### Kotlin Style

```kotlin
// ✅ GOOD: Clear naming, documented
/**
 * Validates database query syntax to prevent hallucinated operators.
 * 
 * @param query The database-specific query string
 * @param database Target database (e.g., "pubmed", "scopus")
 * @return ValidationResult with status and error messages
 */
fun validateQuerySyntax(
    query: String,
    database: Database
): ValidationResult {
    val allowedOperators = database.getSupportedOperators()
    // ...
}

// ❌ BAD: Vague naming, no docs
fun check(q: String, db: String): Boolean {
    // ...
}
```

### Naming Conventions

- **Classes:** PascalCase (`DatabaseQueryPlan`)
- **Functions:** camelCase (`validateQuerySyntax`)
- **Constants:** SCREAMING_SNAKE_CASE (`MAX_RETRIES`)
- **Packages:** lowercase (`com.lumen.pipeline.stage4`)

### Code Organization

```kotlin
package com.lumen.pipeline.stage4

import kotlinx.serialization.Serializable
import com.lumen.common.*

// 1. Data classes
@Serializable
data class DatabaseQueryPlan(
    val queries: Map<String, Query>,
    val validation: ValidationStatus
)

// 2. Interfaces
interface QueryValidator {
    fun validate(query: String): ValidationResult
}

// 3. Implementation classes
class AntiHallucinationValidator : QueryValidator {
    override fun validate(query: String): ValidationResult {
        // Implementation
    }
}

// 4. Extension functions
fun DatabaseQueryPlan.toJson(): String = /* ... */
```

### Error Handling

```kotlin
// ✅ GOOD: Specific exceptions, descriptive messages
sealed class PipelineException(message: String) : Exception(message) {
    class QueryGenerationFailed(
        val stage: Int,
        val reason: String
    ) : PipelineException("Stage $stage failed: $reason")
    
    class DatabaseConnectionError(
        val database: String,
        cause: Throwable
    ) : PipelineException("Failed to connect to $database", cause)
}

// ❌ BAD: Generic exceptions
throw Exception("Error")
```

---

## Documentation

### Code Documentation

```kotlin
/**
 * Executes Stage 4.5: Test & Refine Protocol.
 * 
 * Runs test searches with limited results (50 papers per database),
 * analyzes query broadness, and generates LLM-powered refinement suggestions.
 * 
 * @param input The validated DatabaseQueryPlan from Stage 4
 * @return RefinedQueryPlan with test results and suggested improvements
 * @throws PipelineException.DatabaseConnectionError if API calls fail
 * 
 * ## Example
 * ```kotlin
 * val plan = DatabaseQueryPlan(queries = mapOf(
 *     "pubmed" to Query("(machine learning) AND (crops)")
 * ))
 * val refined = testAndRefineStage.execute(plan)
 * println("Broadness: ${refined.analysis.broadness}") // TOO_BROAD
 * ```
 * 
 * @see Stage4 for initial query generation
 * @see Stage7 for full query execution
 */
suspend fun execute(input: DatabaseQueryPlan): RefinedQueryPlan
```

### User Documentation

- Update `docs/` folder for major features
- Add examples to README for common use cases
- Create video tutorials for complex workflows

---

## Testing

### Unit Tests

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class QueryValidatorTest {
    
    @Test
    fun `validate PubMed query - no hallucinations`() {
        val validator = AntiHallucinationValidator()
        val query = "(machine learning[Title/Abstract]) AND (crops[Title/Abstract])"
        
        val result = validator.validate(query, Database.PUBMED)
        
        assertEquals(ValidationStatus.VALID, result.status)
        assertEquals(0, result.errors.size)
    }
    
    @Test
    fun `validate PubMed query - detects NEAR operator`() {
        val validator = AntiHallucinationValidator()
        val query = "machine NEAR/3 learning"
        
        val result = validator.validate(query, Database.PUBMED)
        
        assertEquals(ValidationStatus.INVALID, result.status)
        assertTrue(result.errors.any { it.contains("NEAR") })
    }
    
    @Test
    fun `execute Stage 4_5 - generates refinements`() = runTest {
        val stage = TestAndRefineStage(mockSearchEngine, mockLlmService)
        val input = DatabaseQueryPlan(/* ... */)
        
        val output = stage.execute(input)
        
        assertEquals(QueryBroadness.TOO_BROAD, output.analysis.broadness)
        assertTrue(output.refinements.changes.isNotEmpty())
    }
}
```

### Integration Tests

```kotlin
@Test
fun `end-to-end pipeline - stages 0 to 7`() = runTest {
    // Stage 0: Project setup
    val context = ProjectContext(
        id = "test_project",
        rawIdea = "AI for crop disease detection"
    )
    
    // Stage 1-4: PICO → Queries
    val queries = pipeline.executeStages(0..4, context)
    
    // Stage 7: Execute search
    val results = searchEngine.execute(queries)
    
    assertTrue(results.documents.size in 100..1000)
    assertTrue(results.documents.all { it.title.isNotBlank() })
}
```

### Test Coverage

- **Goal:** 80%+ code coverage
- Focus on critical paths (query validation, search execution)
- Mock external APIs (OpenAlex, LLM services)

---

## Community

### Getting Help

- **GitHub Discussions:** Ask questions, share ideas
- **Issues:** Report bugs, request features
- **Discord:** Real-time chat (coming soon)

### Recognition

Contributors will be:
- Listed in `README.md`
- Credited in release notes
- Acknowledged in academic publications (if significant contribution)

---

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for helping make systematic reviews faster and more accessible!** 🚀
