package com.lumen.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lumen.core.domain.model.ProblemFraming
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun PicoApprovalDialog(
    projectId: String,
    onDismiss: () -> Unit,
    onApprove: () -> Unit
) {
    var pico by remember { mutableStateOf<ProblemFraming?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load PICO from file
    LaunchedEffect(projectId) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val file = File("data/$projectId/artifacts/ProblemFraming.json")
            if (file.exists()) {
                pico = json.decodeFromString<ProblemFraming>(file.readText())
            } else {
                errorMessage = "PICO extraction file not found"
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load PICO: ${e.message}"
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(700.dp)
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                pico != null -> {
                    PicoApprovalContent(
                        projectId = projectId,
                        pico = pico!!,
                        onDismiss = onDismiss,
                        onApprove = onApprove
                    )
                }
            }
        }
    }
}

@Composable
private fun PicoApprovalContent(
    projectId: String,
    pico: ProblemFraming,
    onDismiss: () -> Unit,
    onApprove: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var currentPico by remember { mutableStateOf(pico) }

    // Save edited PICO to file
    fun savePico(updatedPico: ProblemFraming) {
        try {
            val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
            val file = File("data/$projectId/artifacts/ProblemFraming.json")
            file.writeText(json.encodeToString(ProblemFraming.serializer(), updatedPico))
            currentPico = updatedPico
            println("✅ PICO saved for project $projectId")
        } catch (e: Exception) {
            println("❌ Failed to save PICO: ${e.message}")
            e.printStackTrace()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "PICO Extraction Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Model: ${currentPico.llmModel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (currentPico.approved) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Approved") },
                        leadingIcon = {
                            Icon(Icons.Default.Check, "Approved", modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Population
            PicoField(
                label = "Population",
                value = currentPico.population,
                icon = "👥"
            )

            // Intervention
            PicoField(
                label = "Intervention",
                value = currentPico.intervention,
                icon = "💊"
            )

            // Comparison
            if (currentPico.comparison != null) {
                PicoField(
                    label = "Comparison",
                    value = currentPico.comparison!!,
                    icon = "⚖️"
                )
            }

            // Outcome
            PicoField(
                label = "Outcome",
                value = currentPico.outcome,
                icon = "🎯"
            )

            // Study Designs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📋 Study Designs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    currentPico.studyDesigns.forEach { design ->
                        Text(
                            "• $design",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Timeframe (if present)
            if (currentPico.timeframe != null) {
                PicoField(
                    label = "Timeframe",
                    value = currentPico.timeframe!!,
                    icon = "📅"
                )
            }

            // Context (if present)
            if (currentPico.context != null) {
                PicoField(
                    label = "Context",
                    value = currentPico.context!!,
                    icon = "🌍"
                )
            }
        }

        // Actions
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cancel", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel")
                }

                OutlinedButton(
                    onClick = { showEditDialog = true },
                    enabled = !currentPico.approved
                ) {
                    Text("✏️ Edit")
                }

                Button(
                    onClick = {
                        val approvedPico = currentPico.copy(approved = true)
                        savePico(approvedPico)
                        onApprove()
                    },
                    enabled = !currentPico.approved
                ) {
                    Icon(Icons.Default.Check, "Approve", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (currentPico.approved) "Already Approved" else "Approve & Continue")
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        PicoEditDialog(
            pico = currentPico,
            onDismiss = { showEditDialog = false },
            onSave = { edited ->
                savePico(edited)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun PicoField(
    label: String,
    value: String,
    icon: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    icon,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun PicoEditDialog(
    pico: ProblemFraming,
    onDismiss: () -> Unit,
    onSave: (ProblemFraming) -> Unit
) {
    var population by remember { mutableStateOf(pico.population) }
    var intervention by remember { mutableStateOf(pico.intervention) }
    var comparison by remember { mutableStateOf(pico.comparison ?: "") }
    var outcome by remember { mutableStateOf(pico.outcome) }
    var studyDesigns by remember { mutableStateOf(pico.studyDesigns.joinToString(", ")) }
    var timeframe by remember { mutableStateOf(pico.timeframe ?: "") }
    var context by remember { mutableStateOf(pico.context ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(700.dp)
                .heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Edit PICO Components",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Modify the extracted components as needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = population,
                        onValueChange = { population = it },
                        label = { Text("👥 Population *") },
                        placeholder = { Text("e.g., patients with type 2 diabetes, wheat crops") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = intervention,
                        onValueChange = { intervention = it },
                        label = { Text("💊 Intervention *") },
                        placeholder = { Text("e.g., metformin, machine learning detection") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = comparison,
                        onValueChange = { comparison = it },
                        label = { Text("⚖️ Comparison (optional)") },
                        placeholder = { Text("e.g., placebo, traditional methods") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = outcome,
                        onValueChange = { outcome = it },
                        label = { Text("🎯 Outcome *") },
                        placeholder = { Text("e.g., blood glucose levels, diagnostic accuracy") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = studyDesigns,
                        onValueChange = { studyDesigns = it },
                        label = { Text("📋 Study Designs (comma-separated)") },
                        placeholder = { Text("e.g., RCT, cohort study, observational study") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = timeframe,
                        onValueChange = { timeframe = it },
                        label = { Text("📅 Timeframe (optional)") },
                        placeholder = { Text("e.g., 2015-2024") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("🌍 Context (optional)") },
                        placeholder = { Text("e.g., low-resource settings, field conditions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                // Actions
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val edited = pico.copy(
                                    population = population.trim(),
                                    intervention = intervention.trim(),
                                    comparison = comparison.ifBlank { null },
                                    outcome = outcome.trim(),
                                    studyDesigns = studyDesigns.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() },
                                    timeframe = timeframe.ifBlank { null },
                                    context = context.ifBlank { null }
                                )
                                onSave(edited)
                            },
                            enabled = population.isNotBlank() &&
                                     intervention.isNotBlank() &&
                                     outcome.isNotBlank()
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

