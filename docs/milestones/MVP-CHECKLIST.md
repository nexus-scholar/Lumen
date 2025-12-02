# Lumen MVP Checklist (Months 1–6)

Goal: Ship a **desktop + CLI MVP** that covers the core search pipeline (Stages 0–7) and basic exports, good enough for early adopters and beta testing.

Target: end of Month 6.

---

## 1. Core Engine (KMP Shared Module)

- [ ] **Models**
  - [ ] `Document`
  - [ ] `Author`
  - [ ] `ExternalIds`
  - [ ] `Query`
  - [ ] `ProjectContext`
  - [ ] `ProblemFraming`
  - [ ] `DatabaseQueryPlan`
- [ ] **Utilities**
  - [ ] Text normalization (titles/abstracts)
  - [ ] Boolean query parser
  - [ ] String similarity (Levenshtein)
  - [ ] Rate limiter
  - [ ] HTTP client factory (Ktor)

---

## 2. Providers & Search

- [ ] **Search Providers**
  - [ ] OpenAlex provider (works, filters, pagination)
  - [ ] Crossref provider (query + post‑filtering)
  - [ ] arXiv provider (Atom XML parsing)
  - [ ] Semantic Scholar provider (graph API)
- [ ] **Provider Registry**
  - [ ] `ProviderRegistry.createAll()` from config
- [ ] **Search Engine**
  - [ ] Multi‑provider search orchestrator
  - [ ] Aggregate errors and results

---

## 3. Pipeline Stages (MVP Scope)

For MVP, focus on:

- [ ] Stage 0: Project setup (CLI + basic desktop UI)
- [ ] Stage 1: Problem framing (LLM stub or simple form)
- [ ] Stage 3: Concept expansion (manual for MVP, LLM later)
- [ ] Stage 4: Database query plan (deterministic templates)
- [ ] Stage 4.5: **Test & refine** (core value)
- [ ] Stage 7: Query execution (search providers)
- [ ] Stage 9: Deduplication (exact DOI + basic fuzzy)

The rest (screening, synthesis, etc.) can be stubs in MVP.

---

## 4. Persistence

- [ ] **JSON Artifacts**
  - [ ] Save/load `ProjectContext.json`
  - [ ] Save/load `DatabaseQueryPlan.json`
  - [ ] Save/load `SearchResult.json` (may be large)
- [ ] **SQLite (Minimal)**
  - [ ] `documents` table
  - [ ] Simple indexing on `project_id`, `doi`

---

## 5. Desktop App (Compose)

- [ ] Project dashboard:
  - [ ] Create new project
  - [ ] List existing projects
- [ ] Minimal stage views:
  - [ ] Stage 0: project setup screen
  - [ ] Stage 4 + 4.5: query & test/refine screen
  - [ ] Stage 7: search progress screen
  - [ ] Results list view (titles + basic metadata)

No screening UI yet in MVP.

---

## 6. CLI Tool

- [ ] Binary: `lumen`
- [ ] Commands:
  - [ ] `lumen new "<question>"` – create project
  - [ ] `lumen run --project <id> --until search` – run Stages 0–7
  - [ ] `lumen export --project <id> --format jsonl` – export raw results
- [ ] `--help` output clear and consistent

---

## 7. Exports

- [ ] JSONL/CSV of search results
- [ ] Basic PRISMA search report in Markdown:
  - [ ] Databases
  - [ ] Queries
  - [ ] Result counts
  - [ ] Execution date

Flowchart can wait until Phase 2.

---

## 8. Quality & Tooling

- [ ] Tests:
  - [ ] 60–70% coverage on shared module
  - [ ] Provider integration tests (stubbed where needed)
- [ ] CI:
  - [ ] GitHub Actions: `./gradlew test`
- [ ] Static analysis:
  - [ ] detekt / ktlint (later if time)
- [ ] Basic docs:
  - [ ] Updated `README.md`
  - [ ] `docs/ONBOARDING.md` (this file is part of the set)
  - [ ] `docs/02-ARCHITECTURE.md` & `docs/03-PIPELINE-STAGES.md` aligned with implementation

---

## 9. Beta Prep

- [ ] Identify 3–5 friendly users (PhD students, collaborators)
- [ ] Prepare short usage guide (link to `USER-GUIDE.md`)
- [ ] Collect feedback via GitHub issues or a short form

If all boxes above are checked, Lumen MVP is ready to be put in front of real users.
