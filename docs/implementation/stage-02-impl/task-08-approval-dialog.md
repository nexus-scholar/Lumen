# Task 08: Create Approval Dialog

**Status:** ⬜ Not Started  
**Priority:** High  
**Estimated Effort:** 3 hours  
**Dependencies:** Task 01, Task 07  
**Assignee:** _____

---

## Objective

Create an interactive dialog for reviewing, editing, and approving generated research questions, similar to `PicoApprovalDialog`.

---

## File to Create

**Path:** `src/jvmMain/kotlin/com/lumen/desktop/ui/QuestionsApprovalDialog.kt`

---

## Implementation

### QuestionsApprovalDialog.kt (Abbreviated)

```kotlin
package com.lumen.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.core.domain.model.*
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun QuestionsApprovalDialog(
    projectId: String,
    questions: ResearchQuestions,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onRegenerate: () -> Unit
) {
    var primaryText by remember { mutableStateOf(questions.primary.text) }
    var primaryRationale by remember { mutableStateOf(questions.primary.rationale ?: "") }
    var secondaryQuestions by remember { mutableStateOf(questions.secondary.toMutableList()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Research Questions") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Primary Question Section
                Text("Primary Research Question", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = primaryText,
                    onValueChange = { primaryText = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = primaryRationale,
                    onValueChange = { primaryRationale = it },
                    label = { Text("Rationale") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Secondary Questions Section
                Text("Secondary Research Questions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                secondaryQuestions.forEachIndexed { index, question ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Secondary #${index + 1}", style = MaterialTheme.typography.labelLarge)
                                IconButton(onClick = {
                                    secondaryQuestions = secondaryQuestions.toMutableList().apply {
                                        removeAt(index)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Remove")
                                }
                            }
                            
                            // Editable fields for this secondary question
                            // ... similar to primary
                        }
                    }
                }
                
                // Add Secondary Question Button
                Button(
                    onClick = {
                        val newQuestion = ResearchQuestion(
                            id = "secondary_${secondaryQuestions.size + 1}",
                            text = "",
                            type = QuestionType.SECONDARY,
                            rationale = null,
                            picoMapping = questions.primary.picoMapping
                        )
                        secondaryQuestions = (secondaryQuestions + newQuestion).toMutableList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Secondary Question")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Save edited questions
                val edited = questions.copy(
                    primary = questions.primary.copy(
                        text = primaryText,
                        rationale = primaryRationale
                    ),
                    secondary = secondaryQuestions,
                    approved = true,
                    approvedAt = Clock.System.now()
                )
                
                // Save to file
                val file = File("data/$projectId/ResearchQuestions.json")
                file.writeText(Json.encodeToString(ResearchQuestions.serializer(), edited))
                
                onApprove()
            }) {
                Text("Approve & Continue")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRegenerate) {
                    Text("Regenerate")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
```

---

## Checklist

- [ ] Create QuestionsApprovalDialog.kt
- [ ] Implement primary question editing
- [ ] Implement secondary questions list
- [ ] Add ability to edit each secondary question
- [ ] Add ability to remove secondary questions
- [ ] Add ability to add new secondary questions
- [ ] Implement Approve button (saves and marks approved)
- [ ] Implement Regenerate button (calls stage again)
- [ ] Implement Cancel button
- [ ] Add validation feedback
- [ ] Add character counters
- [ ] Test with real data

---

## Acceptance Criteria

✅ Dialog displays all questions  
✅ Can edit primary question text and rationale  
✅ Can edit each secondary question  
✅ Can add new secondary questions  
✅ Can remove secondary questions  
✅ Approve button saves changes and marks approved  
✅ Regenerate button re-runs LLM  
✅ Cancel button closes without saving  
✅ Validation shows errors inline  
✅ UI is responsive and accessible  

---

## Time: 180 minutes

**Next:** ➡️ Task 09: Write Integration Tests

