package com.lumen.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.core.domain.model.ReviewType
import com.lumen.core.domain.stages.ProjectSetupInput
import com.lumen.core.domain.stages.ProjectSetupStage
import com.lumen.core.domain.pipeline.StageResult
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// Helper object to access Koin from composables
private object KoinHelper : KoinComponent {
    fun getProjectSetupStage(): ProjectSetupStage = get()
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    var researchQuestion by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var selectedReviewType by remember { mutableStateOf(ReviewType.INTERVENTION) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Text("Create New Systematic Review")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Research Question
                OutlinedTextField(
                    value = researchQuestion,
                    onValueChange = { researchQuestion = it },
                    label = { Text("Research Question *") },
                    placeholder = { Text("e.g., What is the efficacy of...?") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        Text("The main research question for this systematic review")
                    }
                )

                // Project Name (Optional)
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name (Optional)") },
                    placeholder = { Text("Leave empty to use research question") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    singleLine = true
                )

                // Review Type
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Review Type *",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ReviewType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedReviewType == type,
                                onClick = { if (!isCreating) selectedReviewType = type },
                                enabled = !isCreating
                            )
                            Column {
                                Text(
                                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = getReviewTypeDescription(type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Loading indicator
                if (isCreating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Creating project...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        if (researchQuestion.isBlank()) {
                            errorMessage = "Please enter a research question"
                            return@launch
                        }

                        isCreating = true
                        errorMessage = null

                        try {
                            val projectSetupStage = KoinHelper.getProjectSetupStage()

                            val input = ProjectSetupInput(
                                researchIdea = researchQuestion,
                                title = projectName.ifBlank { null },
                                reviewType = selectedReviewType
                            )

                            when (val result = projectSetupStage.execute(input)) {
                                is StageResult.Success -> {
                                    onProjectCreated(result.data.id)
                                }
                                is StageResult.Failure -> {
                                    errorMessage = "Failed to create project: ${result.error.message}"
                                    isCreating = false
                                }
                                else -> {
                                    errorMessage = "Unexpected result from project creation"
                                    isCreating = false
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                            isCreating = false
                        }
                    }
                },
                enabled = !isCreating && researchQuestion.isNotBlank()
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun getReviewTypeDescription(type: ReviewType): String {
    return when (type) {
        ReviewType.INTERVENTION -> "Evaluates treatment or intervention effectiveness"
        ReviewType.DIAGNOSTIC -> "Assesses diagnostic test accuracy"
        ReviewType.PROGNOSTIC -> "Examines outcome predictors"
        ReviewType.SCOPING -> "Maps available evidence on a topic"
        ReviewType.QUALITATIVE -> "Synthesizes qualitative research findings"
        ReviewType.RAPID -> "Accelerated review with streamlined methods"
    }
}

