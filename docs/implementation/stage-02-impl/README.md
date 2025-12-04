# Stage 2: Research Questions Generation - Implementation Guide

> **Complete, idempotent implementation plan for Stage 2 of the Lumen systematic review pipeline**

---

## 📚 Documentation Structure

This folder contains everything needed to implement Stage 2 (Research Questions Generation) from scratch:

### 📖 Core Documents

1. **[00-IMPLEMENTATION-PLAN.md](./00-IMPLEMENTATION-PLAN.md)** ⭐ START HERE
   - Overall strategy and phases
   - Task dependency graph
   - Progress tracking table
   - Risk mitigation
   - Success criteria

2. **[QUICK-REFERENCE.md](./QUICK-REFERENCE.md)** ⚡ QUICK GUIDE
   - Task summary table
   - Daily checklists
   - Common commands
   - Progress tracking
   - Getting help

### 🎯 Task Files (Detailed Instructions)

| Task | File | Effort | Priority |
|------|------|--------|----------|
| **01** | [task-01-data-models.md](./task-01-data-models.md) | 1h | 🔴 Critical |
| **02** | [task-02-validation-logic.md](./task-02-validation-logic.md) | 1.5h | 🔴 Critical |
| **03** | [task-03-database-schema.md](./task-03-database-schema.md) | 1h | 🔴 Critical |
| **04** | [task-04-pipeline-stage.md](./task-04-pipeline-stage.md) | 2.5h | 🔴 Critical |
| **05** | [task-05-artifact-storage.md](./task-05-artifact-storage.md) | 1h | 🟡 Medium |
| **06** | [task-06-di-registration.md](./task-06-di-registration.md) | 0.5h | 🔴 Critical |
| **07** | [task-07-ui-stage-card.md](./task-07-ui-stage-card.md) | 1.5h | 🟠 High |
| **08** | [task-08-approval-dialog.md](./task-08-approval-dialog.md) | 3h | 🟠 High |
| **09** | [task-09-integration-tests.md](./task-09-integration-tests.md) | 3h | 🟠 High |
| **10** | [task-10-documentation.md](./task-10-documentation.md) | 2h | 🟡 Medium |

**Total Effort:** 17 hours

---

## 🚀 How to Use This Guide

### For Project Managers

1. **Review** `00-IMPLEMENTATION-PLAN.md` for overall strategy
2. **Assign** tasks from the plan to developers
3. **Track** progress using the tracking table
4. **Monitor** via daily checklist in `QUICK-REFERENCE.md`

### For Developers

1. **Start** with `QUICK-REFERENCE.md` for overview
2. **Read** the relevant `task-XX-*.md` file completely
3. **Follow** the implementation checklist step-by-step
4. **Verify** using the acceptance criteria
5. **Test** using verification steps
6. **Update** progress tracking when complete

### For Code Reviewers

Each task file includes:
- Acceptance criteria to check against
- Definition of Done checklist
- Expected test coverage
- Quality metrics

---

## 📋 Task Structure

Every task file follows the same structure:

```markdown
# Task XX: Name

**Status:** ⬜ Not Started
**Effort:** X hours
**Dependencies:** Previous tasks
**Assignee:** ___

## Objective
Clear goal statement

## Files to Create/Modify
Exact file paths and changes

## Implementation
Complete code with comments

## Checklist
Step-by-step implementation guide

## Acceptance Criteria
What "done" looks like

## Verification Steps
How to test it works

## Integration Points
What it connects to

## Definition of Done
Final checklist before marking complete
```

---

## 🎯 Key Features

### ✅ Idempotent Tasks
- Each task can be safely re-run
- No destructive operations without backups
- Clear rollback procedures

### ✅ Complete Code Included
- All code provided, ready to copy
- No "implement this yourself" gaps
- Tested patterns from existing code

### ✅ Quality Control
- Acceptance criteria for every task
- Test coverage requirements
- Performance benchmarks
- Code review checklists

### ✅ Self-Contained
- Each task is independent where possible
- Clear dependency tracking
- Can work on multiple tasks in parallel

---

## 📊 Implementation Phases

```
Phase 1: Foundation (3-4 hours)
├── Task 01: Data Models
├── Task 02: Validation Logic
└── Task 03: Database Schema

Phase 2: Core Logic (4-5 hours)
├── Task 04: Pipeline Stage
├── Task 05: Artifact Storage
└── Task 06: DI Registration

Phase 3: User Interface (4-5 hours)
├── Task 07: UI Stage Card
└── Task 08: Approval Dialog

Phase 4: Quality Assurance (4-6 hours)
├── Task 09: Integration Tests
└── Task 10: Documentation
```

---

## 🔄 Workflow Example

### Day 1: Foundation
```bash
# Developer A: Data Models (Task 01)
# Developer B: Database Schema (Task 03)
# Developer C: Validation Logic (Task 02, after Task 01)

# End of day: Have compilable data layer
```

### Day 2: Core Logic
```bash
# Developer A: Pipeline Stage (Task 04)
# Developer B: Artifact Storage (Task 05)
# Developer C: DI Registration (Task 06, after Task 04)

# End of day: Stage executes successfully
```

### Day 3: User Interface
```bash
# Developer A: UI Stage Card (Task 07)
# Developer B: Approval Dialog (Task 08)

# End of day: Full UI workflow working
```

### Day 4: Quality & Polish
```bash
# Developer A: Integration Tests (Task 09)
# Developer B: Documentation (Task 10)
# All: Bug fixes, edge cases, polish

# End of day: Production ready
```

---

## ✅ Quality Gates

Before marking a task complete:

1. **Code Quality**
   - [ ] Compiles without errors or warnings
   - [ ] Follows Kotlin conventions
   - [ ] Passes detekt linting
   - [ ] Code reviewed

2. **Functionality**
   - [ ] All acceptance criteria met
   - [ ] Manual testing passed
   - [ ] Error cases handled
   - [ ] Edge cases considered

3. **Testing**
   - [ ] Unit tests written
   - [ ] Tests passing
   - [ ] Coverage >80% (for logic)
   - [ ] Integration tested

4. **Documentation**
   - [ ] KDoc on public APIs
   - [ ] Implementation notes added
   - [ ] Checklist completed
   - [ ] Progress updated

---

## 🆘 Troubleshooting

### Task Blocked?
- Check dependencies completed
- Review integration points
- Check "Common Issues" section in task file
- Consult `STAGE-02-RESEARCH-QUESTIONS-REPORT.md`

### Tests Failing?
- Review verification steps in task file
- Check test coverage requirements
- Verify mock data setup
- Review similar tests in existing code

### Unclear Requirements?
- Read original specification: `docs/implementation/stage-02-research-questions.md`
- Review architecture: `docs/02-ARCHITECTURE.md`
- Study reference implementation: `PicoExtractionStage.kt`
- Check status report for design decisions

---

## 📈 Success Metrics

### Completion Criteria
- ✅ All 10 tasks completed
- ✅ All acceptance criteria met
- ✅ Test coverage >80%
- ✅ All tests passing
- ✅ Documentation updated
- ✅ Code reviewed
- ✅ Deployed to staging
- ✅ User acceptance testing passed

### Expected Outcomes
- Users can generate research questions from PICO
- Questions are validated for quality
- Users can edit and approve questions
- Pipeline flows: Stage 1 → 2 → 3
- PRISMA 2020 compliance achieved

---

## 📞 Support

### Resources
- **Main Status Report:** `../../STAGE-02-RESEARCH-QUESTIONS-REPORT.md`
- **Original Spec:** `../stage-02-research-questions.md`
- **Architecture Guide:** `../../docs/02-ARCHITECTURE.md`
- **User Guide:** `../../docs/USER-GUIDE.md`

### Getting Help
1. Read the task file completely
2. Check "Common Issues" section
3. Review reference implementations
4. Search codebase for similar patterns
5. Consult project lead

---

## 🎉 Ready to Begin?

Start with **[QUICK-REFERENCE.md](./QUICK-REFERENCE.md)** for a high-level overview, then dive into **[00-IMPLEMENTATION-PLAN.md](./00-IMPLEMENTATION-PLAN.md)** for the detailed strategy.

Good luck! 🚀

---

**Created:** December 4, 2025  
**Version:** 1.0  
**Maintainer:** Lumen Development Team

