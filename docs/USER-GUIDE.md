# Lumen User Guide

Lumen is an AI‑assisted, PRISMA‑aligned platform for systematic reviews.  
This guide walks you through a complete review using the planned 15‑stage pipeline.

> Note: Some stages are not implemented yet; this document is the functional spec and user‑facing design.

---

## 1. Basic Concepts

- **Project**: One systematic review (e.g., "AI for crop disease detection")
- **Pipeline**: The sequence of stages from idea → PRISMA‑compliant outputs
- **Artifacts**: JSON files representing each stage's output (Git‑friendly)
- **Operational Data**: Papers, screening decisions, extractions (stored in SQLite)

---

## 2. Create Your First Project (Stage 0)

1. Open the **Lumen desktop app** (future) or use the CLI:

   ```bash
   lumen new "AI methods for crop disease detection"
   ```

2. In the desktop UI:
   - Click **"New Project"**
   - Enter your research question:
     - _"What are AI methods for crop disease detection?"_
   - Choose review type:
     - Scoping / Intervention / Diagnostic (etc.)
   - Click **"Start Review"**

Lumen creates a project folder under `data/project_<id>/` and initializes Stage 0.

---

## 3. PICO Framing (Stage 1)

Lumen uses an LLM to extract PICO from your research idea:

- **Population:** Crops with fungal diseases
- **Intervention:** Machine learning‑based detection methods
- **Comparison:** Traditional visual inspection (optional)
- **Outcome:** Diagnostic accuracy (sensitivity/specificity)

You can:

- Edit each field manually
- Re‑generate with a refined prompt
- Approve once satisfied

Result is stored as `artifacts/ProblemFraming.json`.

---

## 4. Research Questions (Stage 2)

Lumen proposes:

- 1 Primary Question
- 2–5 Secondary Questions

Example:

- Primary: "What is the diagnostic accuracy of ML methods for crop disease detection?"
- Secondary:
  - "How do different ML algorithms compare?"
  - "What image modalities are most effective?"

You can:

- Accept/reject individual questions
- Add your own

---

## 5. Concept Expansion (Stage 3)

Lumen expands your PICO terms into concept blocks:

- Population terms: `crop*`, `plant*`, `wheat`, `maize`, …
- Intervention terms: `"machine learning"`, `"deep learning"`, `CNN`, `SVM`, …
- Outcome terms: `"diagnostic accuracy"`, `sensitivity`, `specificity`, …

Sources:

- LLM synonyms
- MeSH terms (future)
- Term frequency from pilot searches (future)

You can:

- Toggle terms on/off
- Add custom synonyms

---

## 6. Query Plan (Stage 4) + Test & Refine (Stage 4.5)

Lumen generates **database‑specific Boolean queries** for:

- OpenAlex
- Crossref
- arXiv
- Semantic Scholar  
(and later: PubMed, Scopus, Web of Science)

### Anti‑Hallucination

- LLM proposes queries
- Validator scans for:
  - Unsupported operators (e.g., `NEAR`, `ADJ`, `PROX`)
  - Invalid field tags for that database
- If invalid, Lumen falls back to deterministic templates

### Test & Refine Loop (4.5)

Before you commit to a huge search:

1. Run a **test search** (e.g., top 50 papers per database)
2. Lumen shows:
   - Estimated total results (e.g., 8,543)
   - Sample papers (titles/abstracts)
   - Precision estimate (what % look relevant)
   - Year distribution

3. Lumen suggests refinements, e.g.:

- "Results too broad. Suggest:
  - limit to 2019–2024
  - add `NOT blockchain`"

You can:

- Apply suggestions with one click
- Manually edit queries
- Re‑test until queries look right

When satisfied, approve the query plan.

---

## 7. Screening Criteria (Stage 5)

Lumen generates structured inclusion/exclusion criteria from PICO:

- Inclusion examples:
  - Studies on crops (wheat, maize, etc.)
  - ML‑based disease detection methods
  - Diagnostic accuracy outcomes
  - Peer‑reviewed, English, 2019–2024

- Exclusion examples:
  - Animal or human disease studies
  - Non‑ML methods only
  - Conference abstracts only

You can customize and approve the criteria.

---

## 8. Protocol Registration (Stage 6)

Lumen prepares a protocol:

- For PROSPERO, OSF, or internal pre‑registration.
- Exports:
  - Markdown protocol
  - PDF for sharing
  - Future: PROSPERO‑compatible XML

You can submit this to registries before running full searches.

---

## 9. Run Searches (Stage 7)

Lumen executes live API calls for each database:

- OpenAlex, Crossref, arXiv, Semantic Scholar (initially)
- Applies your refined queries
- Handles:
  - Rate limiting, retries
  - JSON/XML parsing
  - Response normalization into `Document` model

You see live progress:

- "OpenAlex: 347 papers"
- "Crossref: 215 papers"
- "arXiv: 89 papers"
- "Semantic Scholar: 125 papers"

Results are stored in `artifacts/SearchResult.json` and in SQLite.

---

## 10. Citation Expansion / Snowballing (Stage 8)

From your initial results:

- Lumen suggests **seed papers** (e.g., top 20 most cited)
- You choose:
  - Forward citations (papers that cite the seeds)
  - Backward citations (papers the seeds cite)
  - Depth (1–3 iterations)

Lumen fetches citing/cited papers via OpenAlex (and later Semantic Scholar), merges them with initial results, and deduplicates.

This covers PRISMA's requirement for citation chaining / snowballing.

---

## 11. Deduplication (Stage 9)

Lumen removes duplicates across all databases and citation expansion:

- DOI exact match
- arXiv ID match
- Title similarity (Levenshtein ≥ 97%)
- Year gap ≤ 1
- Author overlap

You see:

- Total before: e.g., 1,023
- Duplicates removed: 131
- Unique papers: 892

You can inspect uncertain clusters manually.

---

## 12. Title/Abstract Screening (Stage 10)

Lumen provides a screening UI:

- Shows title, abstract, basic metadata
- You click:
  - **Include**, **Exclude**, or **Maybe**
- If you exclude, you select a **reason** (for PRISMA):

  - Wrong population
  - Wrong intervention
  - No relevant outcome
  - Etc.

For teams:

- Two reviewers can screen independently
- Lumen computes Cohen's kappa
- Conflicts are flagged for resolution

---

## 13. Full‑Text Screening (Stage 11)

For papers included at abstract level:

- Lumen helps track full‑text retrieval:
  - Retrieved / Not found / Paywalled / Wrong language
- Embedded PDF viewing (future):
  - Highlighting
  - Notes
- Eligibility checklist:
  - Same criteria as Stage 5, but at full‑text level
- You decide:
  - Include in final review
  - Exclude with reason (documented)

---

## 14. Data Extraction (Stage 12)

For included studies:

- Use templates:
  - Intervention reviews
  - Diagnostic accuracy
  - Custom templates
- Extract:
  - Study design, N
  - Population details
  - Intervention/comparator parameters
  - Outcomes and numeric results
- Data stored in SQLite, exportable to CSV/JSON/Excel

Future: semi‑automated extraction with LLM assistance.

---

## 15. Risk of Bias (Stage 13)

Use standard tools:

- RoB 2.0, ROBINS‑I, QUADAS‑2, etc.
- Domain‑based judgments:
  - Low / Some concerns / High
- Lumen generates:
  - Traffic‑light plots
  - Summary tables

---

## 16. Synthesis & Analytics (Stage 14)

Lumen supports:

- Narrative Synthesis (LLM):
  - 400–600 word summary
  - Contradictions and gaps
- Optional Meta‑Analysis (future):
  - Uses exported CSV + R scripts
  - Forest plots, funnel plots
- Graph analytics (future):
  - Citation networks
  - Co‑authorship networks
  - Concept clusters over time

---

## 17. Export & PRISMA Reporting (Stage 15)

Final exports include:

- CSV of all screened papers
- BibTeX / RIS / EndNote XML
- PRISMA flow diagram (PNG/SVG)
- PRISMA‑S search strategy tables
- Protocol document (Markdown + PDF)
- Optional:
  - R script for meta‑analysis
  - Citation network HTML

These are all designed to be ready to drop into your manuscript's Methods + Supplementary Materials sections.

---

## 18. CLI Usage (Planned)

Once the CLI is in place, typical commands will look like:

```bash
# Start a new project
lumen new "AI methods for crop disease detection" --type scoping

# Run up to Stage 7 (search execution)
lumen run --project project_20251202_030000 --until search

# Export PRISMA artifacts
lumen export --project project_20251202_030000 --format prisma
```

---

For implementation details of each stage, see:  
`docs/03-PIPELINE-STAGES.md`.
