# Stage 15: Export & PRISMA Reporting

**Purpose:** Generate all publication-ready outputs.

## Exports

### 1. PRISMA Flow Diagram
- SVG/PNG flowchart
- Automatically computed counts

### 2. Data Exports
- CSV (all papers + extracted data)
- BibTeX, RIS, EndNote XML
- JSON (complete project)

### 3. Protocol Document
- Markdown + PDF
- PRISMA-P compliant

### 4. Search Strategy Tables
- PRISMA-S format
- One row per database

## Implementation

```kotlin
class ExportStage {
    fun exportPrismaFlowchart(): File
    fun exportDataCsv(): File
    fun exportBibTeX(): File
    fun exportProtocol(): File
}
```

See full implementation in repo.

---

**End of Implementation Guides**

You now have complete technical specifications for all 15 stages!
