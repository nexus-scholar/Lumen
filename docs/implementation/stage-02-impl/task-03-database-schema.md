# Task 03: Add Database Schema

**Status:** ⬜ Not Started  
**Priority:** Critical  
**Estimated Effort:** 1 hour  
**Dependencies:** None (can run parallel with Task 01)  
**Assignee:** _____

---

## Objective

Create database schema for persisting research questions with proper indexing, foreign keys, and support for versioning/audit trail.

---

## Files to Modify

### 1. init-db.sql
**Path:** `scripts/init-db.sql`

### 2. (Optional) Migration File
**Path:** `scripts/migrations/002-add-research-questions.sql`

---

## Implementation

### Update init-db.sql

Add the following after the existing tables (after `screening_decisions`):

```sql
-- ================================================================
-- Research Questions Tables
-- Stage 2: Research Questions Generation
-- ================================================================

-- Research questions master table
CREATE TABLE IF NOT EXISTS research_questions (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL, -- PRIMARY, SECONDARY, EXPLORATORY
    rationale TEXT,
    display_order INTEGER NOT NULL,
    approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_project_questions FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_question_type CHECK (question_type IN ('PRIMARY', 'SECONDARY', 'EXPLORATORY'))
);

-- PICO mapping for each research question
CREATE TABLE IF NOT EXISTS research_question_pico (
    question_id VARCHAR(255) PRIMARY KEY,
    population TEXT NOT NULL,
    intervention TEXT NOT NULL,
    comparison TEXT,
    outcome TEXT NOT NULL,
    CONSTRAINT fk_question_pico FOREIGN KEY (question_id) REFERENCES research_questions(id) ON DELETE CASCADE
);

-- Research questions metadata (generation details)
CREATE TABLE IF NOT EXISTS research_questions_metadata (
    project_id VARCHAR(255) PRIMARY KEY,
    llm_model VARCHAR(255),
    prompt TEXT,
    raw_output TEXT,
    generated_at TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    version INTEGER DEFAULT 1,
    CONSTRAINT fk_project_metadata FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Audit trail for question edits
CREATE TABLE IF NOT EXISTS research_question_history (
    id SERIAL PRIMARY KEY,
    question_id VARCHAR(255) NOT NULL,
    previous_text TEXT NOT NULL,
    new_text TEXT NOT NULL,
    edited_by VARCHAR(255),
    edited_at TIMESTAMP NOT NULL,
    edit_reason TEXT,
    CONSTRAINT fk_question_history FOREIGN KEY (question_id) REFERENCES research_questions(id) ON DELETE CASCADE
);

-- ================================================================
-- Indexes for Research Questions
-- ================================================================

CREATE INDEX IF NOT EXISTS idx_questions_project_id ON research_questions(project_id);
CREATE INDEX IF NOT EXISTS idx_questions_type ON research_questions(question_type);
CREATE INDEX IF NOT EXISTS idx_questions_approved ON research_questions(approved);
CREATE INDEX IF NOT EXISTS idx_questions_display_order ON research_questions(project_id, display_order);
CREATE INDEX IF NOT EXISTS idx_question_history_question_id ON research_question_history(question_id);
CREATE INDEX IF NOT EXISTS idx_question_history_edited_at ON research_question_history(edited_at);

-- ================================================================
-- Sample Data (Optional - for testing)
-- ================================================================

-- Example research questions for sample project
-- Uncomment for development/testing

/*
INSERT INTO research_questions (id, project_id, question_text, question_type, rationale, display_order, approved, created_at, updated_at)
VALUES
    ('rq_primary_1', 'sample-project-1', 
     'What is the diagnostic accuracy of machine learning algorithms for crop disease detection in wheat crops?',
     'PRIMARY',
     'This is the primary research question addressing the main outcome of diagnostic accuracy.',
     1, TRUE, NOW(), NOW()),
    ('rq_secondary_1', 'sample-project-1',
     'How does diagnostic accuracy vary across different machine learning algorithms (CNNs vs. traditional methods)?',
     'SECONDARY',
     'Explores heterogeneity by algorithm type to inform best practices.',
     2, TRUE, NOW(), NOW()),
    ('rq_secondary_2', 'sample-project-1',
     'What image modalities (RGB, multispectral, hyperspectral) provide the highest accuracy?',
     'SECONDARY',
     'Investigates optimal data collection methods for practitioners.',
     3, TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO research_question_pico (question_id, population, intervention, comparison, outcome)
VALUES
    ('rq_primary_1', 'Wheat crops', 'Machine learning algorithms', 'Traditional diagnostic methods', 'Diagnostic accuracy'),
    ('rq_secondary_1', 'Wheat crops', 'Different ML algorithms (CNN vs traditional)', NULL, 'Comparative diagnostic accuracy'),
    ('rq_secondary_2', 'Wheat crops', 'Different image modalities', NULL, 'Diagnostic accuracy by modality')
ON CONFLICT (question_id) DO NOTHING;

INSERT INTO research_questions_metadata (project_id, llm_model, generated_at, approved_at, version)
VALUES
    ('sample-project-1', 'gpt-4-turbo', NOW(), NOW(), 1)
ON CONFLICT (project_id) DO NOTHING;
*/
```

---

## Implementation Checklist

### Pre-Implementation
- [ ] Review existing database schema in `init-db.sql`
- [ ] Understand foreign key relationships
- [ ] Check naming conventions used in existing tables
- [ ] Review PostgreSQL/SQLite compatibility requirements

### Implementation Steps
- [ ] Open `scripts/init-db.sql` in editor
- [ ] Navigate to end of file (after last table)
- [ ] Add comment header for Research Questions section
- [ ] Create `research_questions` table
- [ ] Create `research_question_pico` table
- [ ] Create `research_questions_metadata` table
- [ ] Create `research_question_history` audit table
- [ ] Add all indexes
- [ ] Add constraints (foreign keys, check constraints)
- [ ] (Optional) Add sample data for testing

### Post-Implementation
- [ ] Verify SQL syntax
- [ ] Test on local SQLite database
- [ ] Test on PostgreSQL (if applicable)
- [ ] Verify foreign keys work correctly
- [ ] Test cascade deletes

---

## Acceptance Criteria

### Functional Requirements
✅ **AC1:** `research_questions` table created with all required fields  
✅ **AC2:** `research_question_pico` table stores PICO mapping  
✅ **AC3:** `research_questions_metadata` table stores generation details  
✅ **AC4:** `research_question_history` table for audit trail  
✅ **AC5:** Foreign key to `projects` table with CASCADE delete  
✅ **AC6:** Check constraint on `question_type` enum values  
✅ **AC7:** Indexes created for query performance  
✅ **AC8:** Compatible with both SQLite and PostgreSQL  

### Non-Functional Requirements
✅ **AC9:** Schema follows existing naming conventions  
✅ **AC10:** Proper data types for all fields  
✅ **AC11:** Timestamps use consistent format  
✅ **AC12:** No redundant data (normalized design)  

---

## Verification Steps

### Step 1: Syntax Check
```bash
# For SQLite
sqlite3 test.db < scripts/init-db.sql
```
**Expected:** No syntax errors

### Step 2: Test Schema Creation
```bash
# Create fresh database
rm -f data/test_project/project.db
sqlite3 data/test_project/project.db < scripts/init-db.sql

# Verify tables exist
sqlite3 data/test_project/project.db ".tables"
```
**Expected:** All new tables listed

### Step 3: Test Foreign Key Constraints
```sql
-- Insert test project
INSERT INTO projects (id, name, description, status, created_at, updated_at)
VALUES ('test_proj_123', 'Test Project', 'Test', 'CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert research question (should succeed)
INSERT INTO research_questions (id, project_id, question_text, question_type, rationale, display_order, approved, created_at, updated_at)
VALUES ('rq_test_1', 'test_proj_123', 'Test question?', 'PRIMARY', 'Test', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert with invalid project_id (should fail)
INSERT INTO research_questions (id, project_id, question_text, question_type, rationale, display_order, approved, created_at, updated_at)
VALUES ('rq_test_2', 'nonexistent', 'Test question?', 'PRIMARY', 'Test', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```
**Expected:** First insert succeeds, second fails with FK constraint error

### Step 4: Test Cascade Delete
```sql
-- Delete project
DELETE FROM projects WHERE id = 'test_proj_123';

-- Check questions deleted
SELECT COUNT(*) FROM research_questions WHERE project_id = 'test_proj_123';
```
**Expected:** Count = 0 (cascaded delete worked)

### Step 5: Test Check Constraint
```sql
-- Insert with invalid question_type (should fail)
INSERT INTO research_questions (id, project_id, question_text, question_type, rationale, display_order, approved, created_at, updated_at)
VALUES ('rq_test_3', 'test_proj_123', 'Test?', 'INVALID_TYPE', 'Test', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```
**Expected:** Constraint violation error

### Step 6: Test Indexes
```sql
-- Check indexes created
.indexes research_questions
```
**Expected:** All defined indexes listed

---

## Database Diagram

```
┌─────────────────┐
│   projects      │
│  (id, name...)  │
└────────┬────────┘
         │ 1
         │
         │ *
┌────────┴───────────────────────┐
│  research_questions_metadata   │
│  (project_id, llm_model...)    │
└────────────────────────────────┘
         │ 1
         │
         │ *
┌────────┴────────────────────────┐
│   research_questions            │
│   (id, project_id, text...)     │
└────────┬────────────────────────┘
         │ 1                │ 1
         │                  │
         │ 1                │ *
┌────────┴─────────────┐  ┌┴─────────────────────────┐
│ research_question_   │  │ research_question_       │
│ pico                 │  │ history                  │
│ (question_id, pop..  │  │ (id, question_id, ...)   │
└──────────────────────┘  └──────────────────────────┘
```

---

## Migration Strategy (Optional)

If project already has production data, create migration file:

### Create Migration File
**Path:** `scripts/migrations/002-add-research-questions.sql`

```sql
-- Migration: Add Research Questions Schema
-- Version: 002
-- Date: 2025-12-04
-- Description: Adds tables for Stage 2 (Research Questions Generation)

BEGIN TRANSACTION;

-- Create tables (same as above)
CREATE TABLE IF NOT EXISTS research_questions (...);
-- ... etc

-- Add version tracking
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL,
    description TEXT
);

INSERT INTO schema_version (version, applied_at, description)
VALUES (2, CURRENT_TIMESTAMP, 'Add Research Questions Schema');

COMMIT;
```

### Rollback Script
**Path:** `scripts/migrations/002-rollback.sql`

```sql
-- Rollback Migration 002

BEGIN TRANSACTION;

DROP TABLE IF EXISTS research_question_history;
DROP TABLE IF EXISTS research_questions_metadata;
DROP TABLE IF EXISTS research_question_pico;
DROP TABLE IF EXISTS research_questions;

DELETE FROM schema_version WHERE version = 2;

COMMIT;
```

---

## Performance Considerations

### Index Strategy
- **project_id:** Most common lookup (by project)
- **question_type:** Filter by type (PRIMARY vs SECONDARY)
- **approved:** Filter approved vs pending
- **display_order:** Maintain question ordering

### Query Patterns

**Common Query 1: Get all questions for project**
```sql
SELECT q.*, p.* 
FROM research_questions q
LEFT JOIN research_question_pico p ON q.id = p.question_id
WHERE q.project_id = ?
ORDER BY q.display_order;
```
Uses: `idx_questions_display_order`

**Common Query 2: Get metadata**
```sql
SELECT * FROM research_questions_metadata
WHERE project_id = ?;
```
Uses: Primary key lookup (fast)

**Common Query 3: Get edit history**
```sql
SELECT * FROM research_question_history
WHERE question_id = ?
ORDER BY edited_at DESC;
```
Uses: `idx_question_history_question_id`

---

## Integration Points

### Used By
- Task 05: Artifact Storage (saves to DB)
- Task 04: Pipeline Stage (reads/writes questions)
- Task 08: Approval Dialog (updates approved status)

### Uses
- Existing `projects` table (foreign key)

---

## Rollback Procedure

### Option 1: Drop Tables
```sql
DROP TABLE IF EXISTS research_question_history;
DROP TABLE IF EXISTS research_questions_metadata;
DROP TABLE IF EXISTS research_question_pico;
DROP TABLE IF EXISTS research_questions;
```

### Option 2: Full Database Reset
```bash
rm -f data/*/project.db
./gradlew run  # Will recreate from init-db.sql
```

---

## Common Issues & Solutions

### Issue 1: Foreign Key Not Enforced
**Symptom:** Can insert invalid project_id  
**Solution:** Enable foreign keys in SQLite:
```sql
PRAGMA foreign_keys = ON;
```

### Issue 2: Check Constraint Not Working (SQLite <3.8.3)
**Symptom:** Invalid question_type allowed  
**Solution:** Add application-level validation (already in Task 02)

### Issue 3: Timestamp Format Issues
**Symptom:** Date parsing errors  
**Solution:** Use consistent ISO 8601 format:
```sql
datetime('now')  -- SQLite
CURRENT_TIMESTAMP -- PostgreSQL
```

---

## Testing Checklist

- [ ] Schema creates without errors on SQLite
- [ ] Schema creates without errors on PostgreSQL
- [ ] Foreign keys enforce referential integrity
- [ ] Cascade deletes work correctly
- [ ] Check constraints prevent invalid data
- [ ] Indexes are created
- [ ] Sample data inserts successfully
- [ ] Can query across joined tables
- [ ] Update operations work
- [ ] Delete operations cascade properly

---

## Definition of Done

- [ ] SQL script added to `init-db.sql`
- [ ] All tables created correctly
- [ ] All indexes created
- [ ] Foreign keys enforced
- [ ] Check constraints working
- [ ] Tested on SQLite
- [ ] Tested on PostgreSQL (if applicable)
- [ ] Migration scripts created (if needed)
- [ ] Rollback procedure documented
- [ ] No breaking changes to existing schema

---

## Time Tracking

| Activity | Estimated | Actual | Notes |
|----------|-----------|--------|-------|
| Planning | 10 min | | |
| SQL Development | 30 min | | |
| Testing | 15 min | | |
| Documentation | 5 min | | |
| **Total** | **60 min** | | |

---

**Next Task:** ➡️ Task 04: Implement Pipeline Stage

---

**Task Created:** December 4, 2025  
**Last Updated:** December 4, 2025  
**Version:** 1.0

