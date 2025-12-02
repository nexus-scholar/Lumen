# Stage 5: Screening Criteria Definition

**Purpose:** Generate structured inclusion/exclusion criteria from PICO framework.

**Approach:** Deterministic generation with user review/edit.

---

## Data Models

```kotlin
@Serializable
data class ScreeningCriteria(
    val inclusionCriteria: List<Criterion>,
    val exclusionCriteria: List<Criterion>,
    val approved: Boolean = false
)

@Serializable
data class Criterion(
    val id: String,
    val category: CriterionCategory,
    val description: String,
    val rationale: String,
    val enabled: Boolean = true
)

@Serializable
enum class CriterionCategory {
    POPULATION,
    INTERVENTION,
    COMPARATOR,
    OUTCOME,
    STUDY_DESIGN,
    LANGUAGE,
    PUBLICATION_TYPE,
    TIME_PERIOD,
    OTHER
}
```

---

## Implementation

```kotlin
class ScreeningCriteriaStage : PipelineStage<ProblemFraming, ScreeningCriteria> {
    
    override suspend fun execute(input: ProblemFraming): StageResult<ScreeningCriteria> {
        val inclusion = generateInclusionCriteria(input)
        val exclusion = generateExclusionCriteria(input)
        
        val criteria = ScreeningCriteria(
            inclusionCriteria = inclusion,
            exclusionCriteria = exclusion,
            approved = false
        )
        
        return StageResult.RequiresApproval(
            data = criteria,
            reason = "Review and customize screening criteria"
        )
    }
    
    private fun generateInclusionCriteria(pico: ProblemFraming): List<Criterion> {
        val criteria = mutableListOf<Criterion>()
        
        // Population
        criteria.add(
            Criterion(
                id = "inc_pop_1",
                category = CriterionCategory.POPULATION,
                description = "Studies focusing on ${pico.population}",
                rationale = "Matches target population"
            )
        )
        
        // Intervention
        criteria.add(
            Criterion(
                id = "inc_int_1",
                category = CriterionCategory.INTERVENTION,
                description = "Studies evaluating ${pico.intervention}",
                rationale = "Matches intervention of interest"
            )
        )
        
        // Comparison (if applicable)
        if (pico.comparison != null) {
            criteria.add(
                Criterion(
                    id = "inc_comp_1",
                    category = CriterionCategory.COMPARATOR,
                    description = "Studies comparing with ${pico.comparison}",
                    rationale = "Relevant comparator"
                )
            )
        }
        
        // Outcome
        criteria.add(
            Criterion(
                id = "inc_out_1",
                category = CriterionCategory.OUTCOME,
                description = "Studies reporting ${pico.outcome}",
                rationale = "Matches primary outcome"
            )
        )
        
        // Study designs
        if (pico.studyDesigns.isNotEmpty()) {
            criteria.add(
                Criterion(
                    id = "inc_design_1",
                    category = CriterionCategory.STUDY_DESIGN,
                    description = "Study designs: ${pico.studyDesigns.joinToString(", ")}",
                    rationale = "Appropriate methodologies for research question"
                )
            )
        }
        
        // Language
        criteria.add(
            Criterion(
                id = "inc_lang_1",
                category = CriterionCategory.LANGUAGE,
                description = "Published in English",
                rationale = "Language feasibility for extraction"
            )
        )
        
        // Timeframe
        if (pico.timeframe != null) {
            criteria.add(
                Criterion(
                    id = "inc_time_1",
                    category = CriterionCategory.TIME_PERIOD,
                    description = "Published ${pico.timeframe}",
                    rationale = "Temporal scope of review"
                )
            )
        }
        
        return criteria
    }
    
    private fun generateExclusionCriteria(pico: ProblemFraming): List<Criterion> {
        return listOf(
            Criterion(
                id = "exc_pub_1",
                category = CriterionCategory.PUBLICATION_TYPE,
                description = "Conference abstracts without full text",
                rationale = "Insufficient methodological detail"
            ),
            Criterion(
                id = "exc_pub_2",
                category = CriterionCategory.PUBLICATION_TYPE,
                description = "Editorials, commentaries, opinion pieces",
                rationale = "Not primary research"
            ),
            Criterion(
                id = "exc_pub_3",
                category = CriterionCategory.PUBLICATION_TYPE,
                description = "Review articles (systematic or narrative)",
                rationale = "Secondary sources (but may mine references)"
            ),
            Criterion(
                id = "exc_pop_1",
                category = CriterionCategory.POPULATION,
                description = "Studies not primarily focused on ${pico.population}",
                rationale = "Out of population scope"
            ),
            Criterion(
                id = "exc_int_1",
                category = CriterionCategory.INTERVENTION,
                description = "Studies not evaluating ${pico.intervention}",
                rationale = "Intervention mismatch"
            )
        )
    }
}
```

---

## Desktop UI

```kotlin
@Composable
fun ScreeningCriteriaScreen(viewModel: CriteriaViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Screening Criteria", style = MaterialTheme.typography.headlineMedium)
        
        // Inclusion criteria
        Text("Inclusion Criteria", style = MaterialTheme.typography.titleMedium)
        viewModel.inclusionCriteria.forEach { criterion ->
            CriterionCard(
                criterion = criterion,
                onToggle = { viewModel.toggleCriterion(criterion.id) },
                onEdit = { viewModel.editCriterion(criterion.id) },
                onDelete = { viewModel.deleteCriterion(criterion.id) }
            )
        }
        
        Button(onClick = { viewModel.addInclusion() }) {
            Text("+ Add Inclusion Criterion")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Exclusion criteria
        Text("Exclusion Criteria", style = MaterialTheme.typography.titleMedium)
        viewModel.exclusionCriteria.forEach { criterion ->
            CriterionCard(
                criterion = criterion,
                onToggle = { viewModel.toggleCriterion(criterion.id) },
                onEdit = { viewModel.editCriterion(criterion.id) },
                onDelete = { viewModel.deleteCriterion(criterion.id) }
            )
        }
        
        Button(onClick = { viewModel.addExclusion() }) {
            Text("+ Add Exclusion Criterion")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { viewModel.approveAndContinue() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("✓ Approve & Continue")
        }
    }
}
```

---

## CLI

```bash
# Generate criteria
lumen criteria --project <id>

# Output:
# Inclusion Criteria:
#   1. [POPULATION] Studies on crops with fungal diseases
#   2. [INTERVENTION] Studies evaluating ML detection
#   3. [OUTCOME] Studies reporting diagnostic accuracy
#   ...
# 
# Exclusion Criteria:
#   1. [PUBLICATION_TYPE] Conference abstracts only
#   2. [PUBLICATION_TYPE] Editorials, commentaries
#   ...
```

---

## Tests

```kotlin
class ScreeningCriteriaStageTest {
    
    @Test
    fun `generates criteria from PICO`() = runTest {
        val pico = ProblemFraming(
            population = "Wheat crops",
            intervention = "ML detection",
            outcome = "Diagnostic accuracy",
            studyDesigns = listOf("cohort", "case-control"),
            timeframe = "2019-2024",
            approved = true
        )
        
        val stage = ScreeningCriteriaStage()
        val result = stage.execute(pico)
        
        assertIs<StageResult.RequiresApproval>(result)
        val criteria = result.data
        
        assertTrue(criteria.inclusionCriteria.any { it.category == CriterionCategory.POPULATION })
        assertTrue(criteria.exclusionCriteria.any { it.category == CriterionCategory.PUBLICATION_TYPE })
    }
}
```

---

## Next Stage

→ [Stage 6: Protocol Registration](stage-06-protocol-registration.md)
