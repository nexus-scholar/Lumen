# Lumen: Architecture & Tech Stack

## High-Level Model
- **Kotlin Multiplatform Project (KMP):** Unified code for JVM, JS/Web, and native
- **Desktop App (Compose Multiplatform):** Offline-first, high productivity
- **Web App:** Kotlin/JS, React for later migration
- **CLI:** Power user automation, headless scripting

## Data Storage
- **Hybrid approach:**
  - Artifacts (pipeline stages): JSON files (Git versioned)
  - Operational data (papers, screening): SQLite
  - Collaboration (cloud/team): PostgreSQL

## Architecture Breakdown
Layered for maximal portability and version control:
```
data/project_<id>/
├── artifacts/              # JSON pipeline outputs
├── project.db              # SQLite (screening, extraction)
└── export/                 # Data, report outputs
```

- **APIs:** OpenAlex, Crossref, arXiv, Semantic Scholar, PubMed (future)
- **Graph Analysis:** JGraphT
- **LLM Integration:** OpenAI, Anthropic (structured out)
- **CI/CD:** GitHub Actions
- **Export:** CSV, BibTeX, RIS, PRISMA protocols

## Key Principles
- **Offline-first:** Always works locally, team sync is optional
- **Git-native:** All stage artifacts can be diffed, rolled back, and audited
- **Determinism first:** LLMs are double-checked by domain constraints

