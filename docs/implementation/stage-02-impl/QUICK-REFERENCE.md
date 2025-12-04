# Stage 2: Research Questions - Quick Reference

**Last Updated:** December 4, 2025  
**Status:** Ready for Implementation  
**Total Effort:** 16-20 hours

---

## 📋 Task Summary

| # | Task Name | Effort | Priority | Dependencies | Files |
|---|-----------|--------|----------|--------------|-------|
| 01 | Data Models | 1h | Critical | None | ResearchQuestions.kt |
| 02 | Validation Logic | 1.5h | Critical | 01 | ResearchQuestionsValidator.kt |
| 03 | Database Schema | 1h | Critical | None | init-db.sql |
| 04 | Pipeline Stage | 2.5h | Critical | 01, 02 | ResearchQuestionsStage.kt |
| 05 | Artifact Storage | 1h | Medium | 01 | Test files |
| 06 | DI Registration | 0.5h | Critical | 04 | JvmModule.kt |
| 07 | UI Stage Card | 1.5h | High | 06 | ProjectDetailScreen.kt |
| 08 | Approval Dialog | 3h | High | 01, 07 | QuestionsApprovalDialog.kt |
| 09 | Integration Tests | 3h | High | All | Test suite |
| 10 | Documentation | 2h | Medium | All | Docs updates |

**Total:** 17 hours

---

## 🚀 Quick Start

### For Implementation Lead

1. **Review all task files** in `docs/implementation/stage-02-impl/`
2. **Assign tasks** to developers based on expertise
3. **Start with Tasks 01-03** (can run in parallel)
4. **Follow dependency graph** for subsequent tasks
5. **Mark progress** in `00-IMPLEMENTATION-PLAN.md`

### For Developers

**Starting a Task:**
1. Read the full task file (`task-XX-name.md`)
2. Check dependencies are complete
3. Review acceptance criteria
4. Follow the implementation checklist
5. Run verification steps

**Completing a Task:**
1. Check all items in Definition of Done
2. Run all tests
3. Update progress tracking
4. Notify next dependent task owner

---

## 📂 File Organization

```
docs/implementation/stage-02-impl/
├── 00-IMPLEMENTATION-PLAN.md          # Master plan (this file)
├── task-01-data-models.md             # Create Kotlin data classes
├── task-02-validation-logic.md        # Validation functions + tests
├── task-03-database-schema.md         # SQL schema additions
├── task-04-pipeline-stage.md          # Core stage implementation
├── task-05-artifact-storage.md        # JSON persistence tests
├── task-06-di-registration.md         # Koin DI setup
├── task-07-ui-stage-card.md           # UI card + execution
├── task-08-approval-dialog.md         # Interactive dialog
├── task-09-integration-tests.md       # End-to-end tests
└── task-10-documentation.md           # User/dev docs

QUICK-REFERENCE.md                     # This file
```

---

## ✅ Daily Checklist

### Day 1: Foundation (Tasks 01-03)
- [ ] Create all data models
- [ ] Implement validation logic with tests
- [ ] Add database schema
- [ ] Verify all tests pass
- [ ] **Deliverable:** Compilable data layer

### Day 2: Core Logic (Tasks 04-06)
- [ ] Implement pipeline stage
- [ ] Integrate LLM and validation
- [ ] Test artifact storage
- [ ] Register in DI container
- [ ] **Deliverable:** Working stage execution

### Day 3: User Interface (Tasks 07-08)
- [ ] Add UI stage card
- [ ] Implement execution logic
- [ ] Create approval dialog
- [ ] Test end-to-end in UI
- [ ] **Deliverable:** Functional UI workflow

### Day 4: Quality & Docs (Tasks 09-10)
- [ ] Write integration tests
- [ ] Achieve >80% coverage
- [ ] Update all documentation
- [ ] Final testing and bug fixes
- [ ] **Deliverable:** Production-ready feature

---

## 🎯 Critical Success Factors

1. **Follow the Pattern:** Use `PicoExtractionStage` as reference
2. **Test Early:** Write tests alongside implementation
3. **Idempotency:** Ensure tasks can be re-run safely
4. **Communication:** Update progress daily
5. **Quality Gates:** Don't skip Definition of Done checks

---

## ⚠️ Common Pitfalls

1. **Skipping Validation:** Always validate before approval
2. **Missing Project ID:** Remember the workaround in Task 04
3. **Hard-coding Values:** Use constants and configuration
4. **Incomplete Tests:** Aim for >80% coverage
5. **Poor Error Messages:** Make errors actionable

---

## 🔧 Development Commands

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
./gradlew test --tests ResearchQuestionsStageTest
```

### Run Application
```bash
./gradlew run
```

### Check Coverage
```bash
./gradlew koverHtmlReport
# Open: build/reports/kover/html/index.html
```

### Lint/Format
```bash
./gradlew detekt
./gradlew ktlintFormat
```

---

## 📊 Progress Tracking

Update this table as you complete tasks:

```markdown
| Task | Status | Owner | Completed | Notes |
|------|--------|-------|-----------|-------|
| 01 | ⬜ | - | - | - |
| 02 | ⬜ | - | - | - |
| 03 | ⬜ | - | - | - |
| 04 | ⬜ | - | - | - |
| 05 | ⬜ | - | - | - |
| 06 | ⬜ | - | - | - |
| 07 | ⬜ | - | - | - |
| 08 | ⬜ | - | - | - |
| 09 | ⬜ | - | - | - |
| 10 | ⬜ | - | - | - |
```

Legend:
- ⬜ Not Started
- 🔄 In Progress  
- ✅ Completed
- ⚠️ Blocked
- ❌ Failed

---

## 🆘 Getting Help

### Resources
- **Implementation Spec:** `docs/implementation/stage-02-research-questions.md`
- **Status Report:** `STAGE-02-RESEARCH-QUESTIONS-REPORT.md`
- **Reference Code:** `src/.../stages/PicoExtractionStage.kt`
- **Architecture:** `docs/02-ARCHITECTURE.md`

### Questions?
1. Check the task file's "Common Issues" section
2. Review similar implemented stages (Stage 1, 3)
3. Search existing code for patterns
4. Consult the status report for design decisions

---

## 🎉 Definition of Complete

Stage 2 is considered **complete** when:

- ✅ All 10 tasks marked as completed
- ✅ All acceptance criteria met
- ✅ All tests passing (>80% coverage)
- ✅ UI functional and tested
- ✅ Documentation updated
- ✅ Code reviewed and merged
- ✅ Works in staging environment
- ✅ User acceptance testing passed

---

**Happy Coding! 🚀**

For questions or issues, refer to individual task files or the main implementation plan.

