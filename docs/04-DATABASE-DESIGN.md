# Lumen: Database Design

## Hybrid Model
- **Artifacts:** Each pipeline stage output is a Git-versioned JSON file: human/auditor readable, diffable, portable.
- **Operational:** Fast, transactional reads/writes in SQLite, with keys referencing JSON artifacts.
- **Collaboration:** For teams/institutions, PostgreSQL backend (schema mirrors SQLite, with additional sync/versioning logic).

## Key Tables/Files
| JSON Artifact | SQLite Table | Description |
|---------------|-------------|-------------|
| Pipeline stage results | documents | Most paper and extraction data |
| Search results | screening_decisions | Reviewer inclusion/exclusion |
| Query plans | audit_log | Full history of user actions |

## Export/Import
- Export: ZIP folder with all JSON artifacts, SQLite, and CSV/markdown outputs
- Import: Directly open ZIP, or migrate from Covidence/EndNote/Zotero

## Collaborator Model
- Superuser (owner) + contributors (screen+extract) + observers (read-only)
- Tracks conflict/disagreement and logs all protocol deviations