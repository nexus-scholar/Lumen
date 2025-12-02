# Lumen Phase 2 Checklist (Months 7–12)

Goal: Turn MVP into a **usable PRISMA‑compliant workflow** with screening, citation expansion, and basic PRISMA exports. Target timeframe: Months 7–12.

---

## 1. Complete PRISMA‑Critical Stages

- [ ] Stage 8: Citation expansion / snowballing
  - [ ] OpenAlex citation client
  - [ ] Forward citations
  - [ ] Backward citations
  - [ ] Depth control (1–3)
  - [ ] Integration with deduplication
- [ ] Stage 9: Deduplication (full)
  - [ ] Conservative algorithm (DOI + title + year)
  - [ ] Cluster representation logic

---

## 2. Screening UI (Title/Abstract – Stage 10)

- [ ] Single‑reviewer flow:
  - [ ] Include / Exclude / Maybe
  - [ ] Reason for exclusion (mandatory on exclude)
- [ ] Paging:
  - [ ] Load in batches (e.g., 50 at a time)
- [ ] Progress:
  - [ ] `X / N` screened, percentage
- [ ] Persistence:
  - [ ] Store decisions in SQLite
- [ ] Basic filters (optional):
  - [ ] Year
  - [ ] Database
  - [ ] Keyword search

---

## 3. Full‑Text Screening (Stage 11 – Minimal)

- [ ] Track full‑text status:
  - [ ] Retrieved
  - [ ] Not found
  - [ ] Paywalled
- [ ] Allow manual marking as included/excluded
- [ ] Record reasons for exclusion (PRISMA)

PDF viewer and annotations can be Phase 3.

---

## 4. PRISMA Flow Diagram (Initial)

- [ ] Compute counts:
  - [ ] Records identified (databases + citations)
  - [ ] Duplicates removed
  - [ ] Records screened
  - [ ] Records excluded (title/abstract)
  - [ ] Full‑text assessed
  - [ ] Full‑text excluded (with reasons)
  - [ ] Studies included
- [ ] Generate:
  - [ ] Mermaid / textual representation
  - [ ] Simple SVG flowchart
- [ ] Export:
  - [ ] PNG/SVG flow diagram

---

## 5. PRISMA‑S Search Reporting

- [ ] Log for each database:
  - [ ] Query **as run**
  - [ ] Translation (internal)
  - [ ] Date/time
  - [ ] Filters applied
  - [ ] Results count
- [ ] Generate:
  - [ ] Markdown table summarizing all searches

---

## 6. Data Model & Persistence Enhancements

- [ ] SQLite schema:
  - [ ] `screening_decisions` (fully fleshed out)
  - [ ] `audit_log` (basic actions logged)
- [ ] JSON artifacts in sync with new stages:
  - [ ] `ExpandedSearchResult.json`
  - [ ] `ScreeningCriteria.json`

---

## 7. UX Polish

- [ ] Desktop:
  - [ ] Navigation between stages
  - [ ] Clearly indicate current stage and next step
- [ ] CLI:
  - [ ] `lumen status --project <id>`
  - [ ] Show which stages are complete / pending
- [ ] Error messages:
  - [ ] Clear, actionable (no raw stack traces in UI)

---

## 8. Documentation

- [ ] Update `USER-GUIDE.md` to match implemented behaviour
- [ ] Update `03-PIPELINE-STAGES.md` to mark which stages are implemented
- [ ] Add GIFs/screenshots of screening UI to `README.md` or `docs/01-OVERVIEW.md`

---

## 9. Early Monetization (Optional in Phase 2)

- [ ] Basic licensing:
  - [ ] Free tier: enforcement (projects/papers limit)
  - [ ] Pro tier: config flag / license key placeholder (even if not charging yet)
- [ ] Landing page section:
  - [ ] Clear value proposition
  - [ ] Email capture / waitlist

---

When all of the above are done (or consciously deferred), Lumen moves from "MVP search engine" to a **true PRISMA‑aligned systematic review tool** suitable for real PhD projects.
