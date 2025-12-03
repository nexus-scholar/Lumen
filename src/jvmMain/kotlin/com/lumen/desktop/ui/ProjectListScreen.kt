package com.lumen.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.core.domain.model.Project
import com.lumen.core.domain.model.ProjectStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun ProjectListScreen(
    onCreateNew: () -> Unit,
    onOpenProject: (String) -> Unit
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load projects on composition
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                projects = loadProjects()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load projects: ${e.message}"
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        // Header with title and create button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Systematic Review Projects",
                style = MaterialTheme.typography.headlineLarge
            )

            Button(
                onClick = onCreateNew,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create")
                Spacer(Modifier.width(8.dp))
                Text("New Project")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Content area
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            projects.isEmpty() -> {
                EmptyProjectsView(onCreateNew)
            }

            else -> {
                ProjectListContent(
                    projects = projects,
                    onOpenProject = onOpenProject,
                    onRefresh = {
                        scope.launch {
                            isLoading = true
                            projects = loadProjects()
                            isLoading = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectsView(onCreateNew: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No Projects Yet",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Create your first systematic review project to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCreateNew) {
                Icon(Icons.Default.Add, contentDescription = "Create")
                Spacer(Modifier.width(8.dp))
                Text("Create First Project")
            }
        }
    }
}

@Composable
private fun ProjectListContent(
    projects: List<Project>,
    onOpenProject: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${projects.size} project${if (projects.size != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects.sortedByDescending { it.updatedAt }) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onOpenProject(project.id) }
                )
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Title and status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                StatusChip(project.status)
            }

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetadataChip(
                    label = "Type",
                    value = project.reviewType.name.lowercase().replaceFirstChar { it.uppercase() }
                )

                MetadataChip(
                    label = "Authors",
                    value = project.authors.size.toString()
                )

                MetadataChip(
                    label = "Created",
                    value = formatDate(project.createdAt.toString())
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: ProjectStatus) {
    val (color, text) = when (status) {
        ProjectStatus.CREATED -> MaterialTheme.colorScheme.primary to "Created"
        ProjectStatus.RESEARCH_QUESTIONS -> MaterialTheme.colorScheme.secondary to "Questions"
        ProjectStatus.PICO_EXTRACTION -> MaterialTheme.colorScheme.secondary to "PICO"
        ProjectStatus.CONCEPT_EXPANSION -> MaterialTheme.colorScheme.secondary to "Concepts"
        ProjectStatus.QUERY_GENERATION -> MaterialTheme.colorScheme.secondary to "Queries"
        ProjectStatus.TEST_REFINE -> MaterialTheme.colorScheme.tertiary to "Testing"
        ProjectStatus.PROTOCOL_REGISTRATION -> MaterialTheme.colorScheme.tertiary to "Protocol"
        ProjectStatus.SEARCH_EXECUTION -> MaterialTheme.colorScheme.tertiary to "Searching"
        ProjectStatus.CITATION_EXPANSION -> MaterialTheme.colorScheme.tertiary to "Citations"
        ProjectStatus.DEDUPLICATION -> MaterialTheme.colorScheme.tertiary to "Deduplication"
        ProjectStatus.SCREENING -> MaterialTheme.colorScheme.tertiary to "Screening"
        ProjectStatus.FULLTEXT_SCREENING -> MaterialTheme.colorScheme.tertiary to "Full-text"
        ProjectStatus.DATA_EXTRACTION -> MaterialTheme.colorScheme.tertiary to "Extraction"
        ProjectStatus.RISK_OF_BIAS -> MaterialTheme.colorScheme.tertiary to "Risk"
        ProjectStatus.SYNTHESIS -> MaterialTheme.colorScheme.tertiary to "Synthesis"
        ProjectStatus.EXPORT -> MaterialTheme.colorScheme.tertiary to "Export"
        ProjectStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Completed"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun MetadataChip(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val date = isoDate.substringBefore('T')
        date
    } catch (e: Exception) {
        "Unknown"
    }
}

private fun loadProjects(): List<Project> {
    val dataDir = File("data")
    if (!dataDir.exists()) return emptyList()

    val json = Json {
        ignoreUnknownKeys = true
    }

    return dataDir.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { projectDir ->
            try {
                val projectFile = File(projectDir, "artifacts/Project.json")
                if (projectFile.exists()) {
                    json.decodeFromString<Project>(projectFile.readText())
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        ?.sortedByDescending { it.updatedAt }
        ?: emptyList()
}

