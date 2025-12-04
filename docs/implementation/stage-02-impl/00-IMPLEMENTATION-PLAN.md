# Stage 2: Research Questions Generation - Implementation Plan

**Status:** Not Started  
**Priority:** Critical  
**Estimated Total Effort:** 16-20 hours  
**Target Completion:** Week of December 9, 2025

---

## Overview

This implementation plan breaks down Stage 2 (Research Questions Generation) into **idempotent tasks** that can be executed independently, tested in isolation, and verified against clear acceptance criteria.

Each task:
- ✅ Can be completed independently
- ✅ Has clear input/output requirements
- ✅ Includes acceptance criteria
- ✅ Provides verification steps
- ✅ Is safe to re-run (idempotent)

---

## Task Dependencies Graph

```
[Task 1: Data Models]
    ↓
[Task 2: Validation Logic] ← [Task 3: Database Schema]
    ↓                              ↓
[Task 4: Pipeline Stage] ← [Task 5: Artifact Storage]
    ↓
[Task 6: DI Registration]
    ↓
[Task 7: UI Stage Card] → [Task 8: Approval Dialog]
    ↓
[Task 9: Integration Tests]
    ↓
[Task 10: Documentation]
```

---

## Implementation Phases

### Phase 1: Foundation (Tasks 1-3)
**Effort:** 3-4 hours  
**Goal:** Establish data structures and persistence

- Task 1: Create Data Models ✓ Independent
- Task 2: Implement Validation Logic (depends on Task 1)
- Task 3: Add Database Schema ✓ Independent

### Phase 2: Core Logic (Tasks 4-6)
**Effort:** 4-5 hours  
**Goal:** Implement pipeline stage with LLM integration

- Task 4: Implement Pipeline Stage (depends on Tasks 1, 2)
- Task 5: Implement Artifact Storage (depends on Task 1)
- Task 6: Register in DI Container (depends on Task 4)

### Phase 3: User Interface (Tasks 7-8)
**Effort:** 4-5 hours  
**Goal:** Create UI components for interaction

- Task 7: Add UI Stage Card (depends on Task 6)
- Task 8: Create Approval Dialog (depends on Tasks 1, 7)

### Phase 4: Quality Assurance (Tasks 9-10)
**Effort:** 4-6 hours  
**Goal:** Testing and documentation

- Task 9: Write Integration Tests (depends on all previous)
- Task 10: Update Documentation (depends on all previous)

---

## Task Breakdown

### ✓ Task 1: Create Data Models
- **File:** `task-01-data-models.md`
- **Effort:** 1 hour
- **Dependencies:** None
- **Output:** 4 Kotlin files with serializable data classes

### ✓ Task 2: Implement Validation Logic
- **File:** `task-02-validation-logic.md`
- **Effort:** 1.5 hours
- **Dependencies:** Task 1
- **Output:** Validation functions with unit tests

### ✓ Task 3: Add Database Schema
- **File:** `task-03-database-schema.md`
- **Effort:** 1 hour
- **Dependencies:** None
- **Output:** SQL migration + indexes

### ✓ Task 4: Implement Pipeline Stage
- **File:** `task-04-pipeline-stage.md`
- **Effort:** 2.5 hours
- **Dependencies:** Tasks 1, 2
- **Output:** ResearchQuestionsStage class with LLM integration

### ✓ Task 5: Implement Artifact Storage
- **File:** `task-05-artifact-storage.md`
- **Effort:** 1 hour
- **Dependencies:** Task 1
- **Output:** JSON persistence with versioning

### ✓ Task 6: Register in DI Container
- **File:** `task-06-di-registration.md`
- **Effort:** 0.5 hours
- **Dependencies:** Task 4
- **Output:** Koin module configuration

### ✓ Task 7: Add UI Stage Card
- **File:** `task-07-ui-stage-card.md`
- **Effort:** 1.5 hours
- **Dependencies:** Task 6
- **Output:** Stage card in ProjectDetailScreen

### ✓ Task 8: Create Approval Dialog
- **File:** `task-08-approval-dialog.md`
- **Effort:** 3 hours
- **Dependencies:** Tasks 1, 7
- **Output:** Interactive dialog for question review/edit

### ✓ Task 9: Write Integration Tests
- **File:** `task-09-integration-tests.md`
- **Effort:** 3 hours
- **Dependencies:** All previous tasks
- **Output:** Test suite with >80% coverage

### ✓ Task 10: Update Documentation
- **File:** `task-10-documentation.md`
- **Effort:** 2 hours
- **Dependencies:** All previous tasks
- **Output:** User guide + screenshots

---

## Quality Gates

Each task must pass these gates before proceeding:

### Gate 1: Code Quality
- [ ] Follows Kotlin coding conventions
- [ ] No compiler warnings
- [ ] Passes detekt linting
- [ ] Code reviewed (self or peer)

### Gate 2: Functionality
- [ ] All acceptance criteria met
- [ ] Manual testing passed
- [ ] Error cases handled
- [ ] Edge cases considered

### Gate 3: Integration
- [ ] Works with existing code
- [ ] No breaking changes
- [ ] Dependencies resolved
- [ ] DI injection works

### Gate 4: Documentation
- [ ] Code comments added
- [ ] KDoc for public APIs
- [ ] Implementation notes updated
- [ ] Checklist marked complete

---

## Progress Tracking

| Task | Status | Assignee | Started | Completed | Notes |
|------|--------|----------|---------|-----------|-------|
| 01 - Data Models | ⬜ Not Started | - | - | - | - |
| 02 - Validation | ⬜ Not Started | - | - | - | - |
| 03 - Database | ⬜ Not Started | - | - | - | - |
| 04 - Pipeline Stage | ⬜ Not Started | - | - | - | - |
| 05 - Artifact Storage | ⬜ Not Started | - | - | - | - |
| 06 - DI Registration | ⬜ Not Started | - | - | - | - |
| 07 - UI Stage Card | ⬜ Not Started | - | - | - | - |
| 08 - Approval Dialog | ⬜ Not Started | - | - | - | - |
| 09 - Integration Tests | ⬜ Not Started | - | - | - | - |
| 10 - Documentation | ⬜ Not Started | - | - | - | - |

**Status Legend:**
- ⬜ Not Started
- 🔄 In Progress
- ✅ Completed
- ⚠️ Blocked
- ❌ Failed

---

## Risk Mitigation

### Risk 1: LLM Service Unavailability
**Probability:** Medium  
**Impact:** High  
**Mitigation:** 
- Implement graceful fallback to manual entry
- Add offline mode support
- Mock LLM service for testing

### Risk 2: UI Complexity
**Probability:** Low  
**Impact:** Medium  
**Mitigation:**
- Reuse PicoApprovalDialog pattern
- Start with simple MVP UI
- Iterate based on feedback

### Risk 3: Integration with Stage 3
**Probability:** Medium  
**Impact:** High  
**Mitigation:**
- Update ConceptExpansionStage to load questions from artifacts
- Add precondition checks
- Write integration tests early

### Risk 4: Data Migration
**Probability:** Low  
**Impact:** Low  
**Mitigation:**
- No existing data to migrate (new stage)
- Schema designed for backward compatibility

---

## Success Criteria

### Minimum Viable Implementation (MVP)
- [ ] Can generate questions from PICO using LLM
- [ ] Questions stored in database and JSON artifacts
- [ ] UI displays stage card and basic approval
- [ ] Pipeline progresses: Stage 1 → 2 → 3
- [ ] Manual testing successful

### Production Ready
- [ ] All 10 tasks completed with quality gates passed
- [ ] Integration tests passing with >80% coverage
- [ ] UI allows editing and refinement
- [ ] Documentation complete with screenshots
- [ ] Performance acceptable (<2s for generation)
- [ ] Error handling covers all edge cases

### Excellence
- [ ] Advanced validation with quality scoring
- [ ] Question editing history and versioning
- [ ] Examples and guidance for users
- [ ] A/B testing of different prompts
- [ ] Metrics and analytics for question quality

---

## Rollback Plan

If implementation needs to be reverted:

1. **Database Rollback:**
   ```sql
   DROP TABLE IF EXISTS research_questions;
   ```

2. **Code Rollback:**
   - Remove files created in tasks 1-8
   - Revert changes to existing files
   - Remove DI registration

3. **UI Rollback:**
   - Remove StageType.RESEARCH_QUESTIONS
   - Remove stage card from ProjectDetailScreen

4. **Verification:**
   - Run existing tests to ensure no breakage
   - Verify Stage 1 → 3 still works
   - Check no orphaned references

---

## Next Steps

1. **Review this plan** with team/stakeholders
2. **Assign tasks** to developers
3. **Set up tracking** (GitHub issues, Jira, etc.)
4. **Begin with Task 1** (Data Models)
5. **Daily standups** to track progress
6. **Weekly demos** to show incremental progress

---

## Resources

- **Documentation:** `docs/implementation/stage-02-research-questions.md`
- **Status Report:** `STAGE-02-RESEARCH-QUESTIONS-REPORT.md`
- **Reference Implementation:** `src/commonMain/kotlin/com/lumen/core/domain/stages/PicoExtractionStage.kt`
- **Similar UI Pattern:** `src/jvmMain/kotlin/com/lumen/desktop/ui/PicoApprovalDialog.kt`

---

**Last Updated:** December 4, 2025  
**Plan Version:** 1.0  
**Next Review:** After Task 5 completion

