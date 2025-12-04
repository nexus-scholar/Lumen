# Task 10: Update Documentation

**Status:** ⬜ Not Started  
**Priority:** Medium  
**Estimated Effort:** 2 hours  
**Dependencies:** All previous tasks  
**Assignee:** _____

---

## Objective

Update all relevant documentation to reflect the completed Stage 2 implementation, including user guides, API documentation, and implementation notes.

---

## Files to Update

1. **USER-GUIDE.md** - Add Stage 2 usage instructions
2. **ONBOARDING.md** - Update pipeline overview
3. **stage-02-research-questions.md** - Mark as implemented
4. **README.md** - Update status/features
5. **CHANGELOG.md** - Add Stage 2 entry

---

## Documentation Updates

### 1. USER-GUIDE.md

Add section after Stage 1:

````markdown
### Stage 2: Research Questions Generation

After approving your PICO framework, Lumen generates focused research questions.

#### Running the Stage

1. Navigate to your project
2. Ensure Stage 1 (PICO) is completed and approved
3. Click "Run" on the "2. Research Questions" card
4. Wait for LLM to generate questions (typically 10-30 seconds)

#### Reviewing Questions

The generated questions will include:
- **One primary question**: The main question your review answers
- **2-4 secondary questions**: Supporting questions exploring subgroups or moderators

Review each question for:
- ✅ Clarity and specificity
- ✅ Incorporation of PICO elements
- ✅ Answerability through systematic review
- ✅ Appropriate scope

#### Editing Questions

You can edit any generated question:
1. Click the question text to edit
2. Modify the wording to improve clarity
3. Update the rationale explaining its importance
4. Add or remove secondary questions as needed

#### Best Practices

- Keep primary question focused and specific
- Ensure secondary questions don't duplicate the primary
- Limit to 3-5 total secondary questions
- Each question should end with "?"
- Rationales should explain why the question matters

#### Approval

Once satisfied:
1. Click "Approve & Continue"
2. Questions are locked and saved
3. Stage 3 (Concept Expansion) becomes available
````

### 2. Update stage-02-research-questions.md

Add implementation status:

```markdown
# Stage 2: Research Questions Generation

**Status:** ✅ **IMPLEMENTED**  
**Implementation Date:** December 2025  
**Version:** 1.0

## Implementation Notes

- Located in: `src/commonMain/kotlin/com/lumen/core/domain/stages/ResearchQuestionsStage.kt`
- Data models: `src/commonMain/kotlin/com/lumen/core/domain/model/ResearchQuestions.kt`
- UI: `src/jvmMain/kotlin/com/lumen/desktop/ui/QuestionsApprovalDialog.kt`
- Database schema: `scripts/init-db.sql` (research_questions tables)
- Tests: `src/jvmTest/kotlin/com/lumen/core/domain/stages/ResearchQuestionsStageTest.kt`

## Changes from Original Specification

1. Added `llmModel`, `prompt`, `rawOutput` fields for auditability
2. Added `approvedAt` and `approvedBy` for compliance tracking
3. Implemented edit history tracking in database
4. Added validation warnings for quality control

## Known Limitations

1. Project ID must be passed through artifact filename (workaround until ProblemFraming includes it)
2. Review type defaults to "intervention" (to be fixed with project context passing)
3. Question similarity detection uses simple Jaccard similarity (could be enhanced with embeddings)
```

### 3. Update README.md

Update features list:

```markdown
## Features

### Pipeline Stages
- ✅ **Stage 1: PICO Extraction** - AI-powered extraction of Population, Intervention, Comparison, Outcome
- ✅ **Stage 2: Research Questions** - Generate primary and secondary research questions
- ✅ **Stage 3: Concept Expansion** - Synonym and related term generation
- ✅ **Stage 4: Query Generation** - Boolean query creation
...
```

### 4. Create CHANGELOG.md Entry

```markdown
## [Unreleased]

### Added
- Stage 2: Research Questions Generation
  - Automated generation of primary and secondary research questions from PICO
  - Interactive approval dialog with editing capabilities
  - Validation of question quality and completeness
  - Database persistence with audit trail
  - JSON artifact storage for version control
  - Integration with pipeline (Stage 1 → 2 → 3 flow)

### Changed
- Updated Stage 3 (Concept Expansion) to depend on Stage 2 completion
- Modified UI to show Stage 2 card between PICO and Concept Expansion
- Enhanced database schema with research_questions tables

### Fixed
- Pipeline now follows correct PRISMA 2020 sequence
```

### 5. Update API Documentation (KDoc)

Ensure all public APIs have KDoc:

```kotlin
/**
 * Generates focused research questions from an approved PICO framework.
 *
 * This stage is part of the PRISMA 2020-compliant systematic review pipeline,
 * positioned after PICO extraction (Stage 1) and before concept expansion (Stage 3).
 *
 * The stage uses an LLM to generate:
 * - One primary research question that directly incorporates all PICO elements
 * - 2-4 secondary research questions exploring subgroups, moderators, or mechanisms
 *
 * All generated questions undergo validation for quality, clarity, and answerability.
 * Human approval is required before proceeding to the next stage.
 *
 * @property llmService Service for LLM-based question generation
 * @property artifactStore Storage for persisting questions as JSON artifacts
 *
 * @see ProblemFraming Input data from Stage 1
 * @see ResearchQuestions Output data structure
 * @see ResearchQuestionsValidator Validation logic
 */
class ResearchQuestionsStage(...)
```

---

## Checklist

### Documentation Writing
- [ ] Write user guide section
- [ ] Update implementation status
- [ ] Create changelog entry
- [ ] Update README features
- [ ] Add KDoc to all public APIs
- [ ] Update architecture diagrams if needed

### Screenshots (Optional)
- [ ] Capture Stage 2 card in UI
- [ ] Capture approval dialog
- [ ] Capture generated questions example
- [ ] Add to docs/images/

### Review
- [ ] Check for typos and grammar
- [ ] Verify all links work
- [ ] Ensure consistency with existing docs
- [ ] Test code examples compile

### Publication
- [ ] Commit documentation updates
- [ ] Update GitHub wiki (if applicable)
- [ ] Update project website (if applicable)
- [ ] Notify team of updates

---

## Acceptance Criteria

✅ User guide includes Stage 2 instructions  
✅ Implementation notes added to spec  
✅ README updated with Stage 2 feature  
✅ Changelog entry created  
✅ All public APIs have KDoc comments  
✅ Documentation is clear and accurate  
✅ No broken links or references  
✅ Consistent terminology throughout  

---

## Additional Documentation Tasks

### For Future Phases
- [ ] Add video tutorial
- [ ] Create troubleshooting guide
- [ ] Document prompt engineering tips
- [ ] Add FAQ section
- [ ] Create developer guide for customization

---

## Time: 120 minutes

---

## 🎉 Stage 2 Implementation Complete!

Upon completion of this task, Stage 2 (Research Questions Generation) will be fully implemented, tested, documented, and ready for production use.

**Next Steps:**
- Deploy to staging environment
- Conduct user acceptance testing
- Gather feedback
- Plan enhancements for next iteration

