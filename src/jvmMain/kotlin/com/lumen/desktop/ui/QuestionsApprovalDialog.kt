package com.lumen.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.core.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun QuestionsApprovalDialog(
    projectId: String,
    questions: ResearchQuestions,
    onDismiss: () -> Unit,
    onApprove: () -> Unit
) {
    var primaryText by remember { mutableStateOf(questions.primary.text) }
    var primaryRationale by remember { mutableStateOf(questions.primary.rationale ?: "") }
    var secondaryQuestions by remember { mutableStateOf(questions.secondary.toMutableStateList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(700.dp),
        title = {
            Text(
                "Review Research Questions",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Primary Question Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Primary Research Question",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = primaryText,
                            onValueChange = { primaryText = it },
                            label = { Text("Question") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            supportingText = { Text("${primaryText.length} characters") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = primaryRationale,
                            onValueChange = { primaryRationale = it },
                            label = { Text("Rationale") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            placeholder = { Text("Why this question is important...") }
                        )
                    }
                }

                // Secondary Questions Section
                Text(
                    "Secondary Research Questions",
                    style = MaterialTheme.typography.titleMedium
                )

                secondaryQuestions.forEachIndexed { index, question ->
                    var questionText by remember { mutableStateOf(question.text) }
                    var questionRationale by remember { mutableStateOf(question.rationale ?: "") }

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Secondary #${index + 1}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                IconButton(
                                    onClick = {
                                        secondaryQuestions.removeAt(index)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, "Remove question")
                                }
                            }

                            OutlinedTextField(
                                value = questionText,
                                onValueChange = {
                                    questionText = it
                                    secondaryQuestions[index] = question.copy(text = it)
                                },
                                label = { Text("Question") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = questionRationale,
                                onValueChange = {
                                    questionRationale = it
                                    secondaryQuestions[index] = question.copy(rationale = it)
                                },
                                label = { Text("Rationale") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 1,
                                placeholder = { Text("Why this question adds value...") }
                            )
                        }
                    }
                }

                // Add Secondary Question Button
                OutlinedButton(
                    onClick = {
                        val newQuestion = ResearchQuestion(
                            id = "secondary_${secondaryQuestions.size + 1}",
                            text = "",
                            type = QuestionType.SECONDARY,
                            rationale = null,
                            picoMapping = questions.primary.picoMapping
                        )
                        secondaryQuestions.add(newQuestion)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Secondary Question")
                }

                // Validation info
                if (primaryText.length < 20) {
                    Text(
                        "⚠️ Primary question should be at least 20 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Create updated questions object
                    val updatedQuestions = questions.copy(
                        primary = questions.primary.copy(
                            text = primaryText,
                            rationale = primaryRationale.ifBlank { null }
                        ),
                        secondary = secondaryQuestions.toList(),
                        approved = true,
                        approvedAt = Clock.System.now()
                    )

                    // Save to file
                    try {
                        val artifactsDir = File("data/$projectId/artifacts")
                        artifactsDir.mkdirs()
                        val file = File(artifactsDir, "ResearchQuestions.json")
                        file.writeText(Json.encodeToString(updatedQuestions))
                        println("✅ Research questions saved and approved for project $projectId")
                        onApprove()
                    } catch (e: Exception) {
                        println("❌ Failed to save research questions: ${e.message}")
                        e.printStackTrace()
                    }
                },
                enabled = primaryText.length >= 20
            ) {
                Text("Approve & Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

