package com.lumen.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.domain.stages.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

// Helper to access Koin from Composables
private object StageKoinHelper : KoinComponent {
    fun getPicoStage(): PicoExtractionStage {
        println("🔍 DEBUG: StageKoinHelper.getPicoStage() called")
        return try {
            val stage = get<PicoExtractionStage>()
            println("✅ DEBUG: Successfully got PicoExtractionStage from Koin")
            stage
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get PicoExtractionStage from Koin!")
            println("❌ DEBUG: Error type: ${e::class.simpleName}")
            println("❌ DEBUG: Error message: ${e.message}")
            println("❌ DEBUG: Stack trace:")
            e.printStackTrace()
            throw e
        }
    }

    fun getQuestionsStage(): ResearchQuestionsStage {
        println("🔍 DEBUG: Getting ResearchQuestionsStage from Koin...")
        return try {
            get<ResearchQuestionsStage>()
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get ResearchQuestionsStage: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getConceptStage(): ConceptExpansionStage {
        println("🔍 DEBUG: Getting ConceptExpansionStage from Koin...")
        return try {
            get<ConceptExpansionStage>()
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get ConceptExpansionStage: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getQueryStage(): QueryGenerationStage {
        println("🔍 DEBUG: Getting QueryGenerationStage from Koin...")
        return try {
            get<QueryGenerationStage>()
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get QueryGenerationStage: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getTestStage(): TestAndRefineStage {
        println("🔍 DEBUG: Getting TestAndRefineStage from Koin...")
        return try {
            get<TestAndRefineStage>()
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get TestAndRefineStage: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getSearchStage(): SearchExecutionStage {
        println("🔍 DEBUG: Getting SearchExecutionStage from Koin...")
        return try {
            get<SearchExecutionStage>()
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get SearchExecutionStage: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getDedupStage(): DeduplicationStage {
        println("🔍 DEBUG: Getting DeduplicationStage from Koin...")
        return try {
            get<DeduplicationStage>()
        } catch (e: Exception) {
            println("❌ DEBUG: Failed to get DeduplicationStage: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    onViewResults: () -> Unit
) {
    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    // Load project
    LaunchedEffect(projectId, reloadTrigger) {
        try {
            project = loadProject(projectId)
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load project: ${e.message}"
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            project?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            projectId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(onClick = onViewResults) {
                    Text("View Results")
                }
            }
        }

        // Content
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            project != null -> {
                ProjectContent(
                    project = project!!,
                    projectId = projectId,
                    onProjectUpdate = { reloadTrigger++ }
                )
            }
        }
    }
}

@Composable
private fun ProjectContent(
    project: Project,
    projectId: String,
    onProjectUpdate: () -> Unit
) {
    // Load PICO if it exists
    var pico by remember { mutableStateOf<ProblemFraming?>(null) }

    LaunchedEffect(projectId) {
        pico = loadArtifact<ProblemFraming>(projectId, "ProblemFraming.json")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Info Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Project Information", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                InfoRow("Type", project.reviewType.name)
                InfoRow("Status", project.status.name)
                InfoRow("Created", project.createdAt.toString().substringBefore('T'))
                InfoRow("Authors", project.authors.size.toString())
            }
        }

        // Research Question Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Research Question", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(project.rawIdea, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // PICO Display Card (if exists and approved)
        if (pico != null && pico!!.approved) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "✅ Problem Framing (PICO)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        var showPicoDialog by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { showPicoDialog = true }) {
                            Text("View Details")
                        }

                        if (showPicoDialog) {
                            PicoApprovalDialog(
                                projectId = projectId,
                                onDismiss = {
                                    showPicoDialog = false
                                    // Reload PICO in case it was edited
                                    pico = loadArtifact<ProblemFraming>(projectId, "ProblemFraming.json")
                                },
                                onApprove = {
                                    showPicoDialog = false
                                    // Reload PICO
                                    pico = loadArtifact<ProblemFraming>(projectId, "ProblemFraming.json")
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Quick summary
                    Text(
                        "👥 Population: ${pico!!.population}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        "💊 Intervention: ${pico!!.intervention}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        "🎯 Outcome: ${pico!!.outcome}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Pipeline Stages
        Text("Pipeline Stages", style = MaterialTheme.typography.headlineSmall)

        StageCard(
            name = "1. PICO Extraction",
            description = "Extract Population, Intervention, Comparison, Outcome",
            projectId = projectId,
            stageType = StageType.PICO,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )

        StageCard(
            name = "2. Research Questions",
            description = "Generate primary and secondary research questions",
            projectId = projectId,
            stageType = StageType.RESEARCH_QUESTIONS,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )

        StageCard(
            name = "3. Concept Expansion",
            description = "Generate synonyms and related terms",
            projectId = projectId,
            stageType = StageType.CONCEPT,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )

        StageCard(
            name = "4. Query Generation",
            description = "Create Boolean search queries",
            projectId = projectId,
            stageType = StageType.QUERY,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )

        StageCard(
            name = "4.5. Test & Refine",
            description = "Test queries and refine if needed",
            projectId = projectId,
            stageType = StageType.TEST,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )

        StageCard(
            name = "7. Search Execution",
            description = "Execute full search across databases",
            projectId = projectId,
            stageType = StageType.SEARCH,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )

        StageCard(
            name = "9. Deduplication",
            description = "Remove duplicate documents",
            projectId = projectId,
            stageType = StageType.DEDUP,
            currentStatus = project.status,
            onProjectUpdate = onProjectUpdate
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

enum class StageType {
    PICO, RESEARCH_QUESTIONS, CONCEPT, QUERY, TEST, SEARCH, DEDUP
}

@Composable
private fun StageCard(
    name: String,
    description: String,
    projectId: String,
    stageType: StageType,
    currentStatus: ProjectStatus,
    onProjectUpdate: () -> Unit = {}
) {
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Check if stage is already completed
    val isEnabled = when (stageType) {
        StageType.PICO -> true
        StageType.RESEARCH_QUESTIONS -> true
        StageType.CONCEPT -> currentStatus >= ProjectStatus.RESEARCH_QUESTIONS
        StageType.QUERY -> currentStatus >= ProjectStatus.CONCEPT_EXPANSION
        StageType.TEST -> currentStatus >= ProjectStatus.QUERY_GENERATION
        StageType.SEARCH -> currentStatus >= ProjectStatus.TEST_REFINE
        StageType.DEDUP -> currentStatus >= ProjectStatus.SEARCH_EXECUTION
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isCompleted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(name, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (result != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                result = null
                                try {
                                    val stageResult = runStage(projectId, stageType)
                                    isCompleted = stageResult != null
                                    result = stageResult
                                } catch (e: Exception) {
                                    result = "Error: ${e.message}"
                                }
                                isRunning = false
                            }
                        },
                        enabled = isEnabled && !isRunning
                    ) {
                        Icon(Icons.Default.PlayArrow, "Run", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Run")
                    }

                    // Show "View" button for PICO stage if results exist
                    if (stageType == StageType.PICO && result?.contains("PICO") == true) {
                        var showApprovalDialog by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showApprovalDialog = true }
                        ) {
                            Text("View")
                        }

                        if (showApprovalDialog) {
                            PicoApprovalDialog(
                                projectId = projectId,
                                onDismiss = { showApprovalDialog = false },
                                onApprove = {
                                    // Approve the PICO and enable next stage
                                    approvePico(projectId)
                                    showApprovalDialog = false
                                    result = "✅ PICO approved!"
                                    // Trigger project reload to update status and enable Stage 2
                                    onProjectUpdate()
                                }
                            )
                        }
                    }

                    // Show "View/Approve" button for Research Questions if results exist or LLM failed
                    if (stageType == StageType.RESEARCH_QUESTIONS &&
                        (result?.contains("Questions generated") == true || result?.contains("LLM failed") == true)) {
                        var showQuestionsDialog by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showQuestionsDialog = true }
                        ) {
                            Text("View & Approve")
                        }

                        if (showQuestionsDialog) {
                            // Load questions from file
                            val questions = loadArtifact<ResearchQuestions>(projectId, "ResearchQuestions.json")
                            if (questions != null) {
                                QuestionsApprovalDialog(
                                    projectId = projectId,
                                    questions = questions,
                                    onDismiss = { showQuestionsDialog = false },
                                    onApprove = {
                                        showQuestionsDialog = false
                                        result = "✅ Research questions approved!"
                                        isCompleted = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun runStage(projectId: String, stageType: StageType): String? {
    println("🔍 DEBUG: Starting runStage for $stageType")

    return try {
        when (stageType) {
            StageType.PICO -> {
                println("🔍 DEBUG: Loading project for PICO stage...")
                val project = loadProject(projectId)
                if (project == null) {
                    println("❌ DEBUG: Project not found!")
                    return "❌ Project not found"
                }
                println("✅ DEBUG: Project loaded: ${project.name}")

                println("🔍 DEBUG: Getting PicoExtractionStage from Koin...")
                val stage = StageKoinHelper.getPicoStage()
                println("✅ DEBUG: Got PicoExtractionStage instance")

                println("🔍 DEBUG: Executing PICO extraction...")
                when (val result = stage.execute(project)) {
                    is StageResult.Success -> {
                        println("✅ DEBUG: PICO extraction successful")
                        "✅ PICO extracted successfully"
                    }
                    is StageResult.RequiresApproval -> {
                        println("⚠️ DEBUG: PICO needs approval")
                        "⚠️ PICO extracted, requires approval"
                    }
                    is StageResult.Failure -> {
                        println("❌ DEBUG: PICO failed: ${result.error.message}")
                        "❌ Failed: ${result.error.message}"
                    }
                }
            }
            StageType.RESEARCH_QUESTIONS -> {
                println("🔍 DEBUG: Loading PICO for research questions generation...")
                val pico = loadArtifact<ProblemFraming>(projectId, "ProblemFraming.json")
                if (pico == null) {
                    println("❌ DEBUG: PICO not found!")
                    return "❌ PICO not found. Please complete Stage 1 first."
                }

                if (!pico.approved) {
                    println("❌ DEBUG: PICO not approved!")
                    return "❌ PICO must be approved before generating questions."
                }
                println("✅ DEBUG: PICO loaded and approved")

                println("🔍 DEBUG: Getting ResearchQuestionsStage from Koin...")
                val stage = StageKoinHelper.getQuestionsStage()
                println("✅ DEBUG: Got ResearchQuestionsStage instance")

                println("🔍 DEBUG: Executing research questions generation...")
                when (val result = stage.execute(pico)) {
                    is StageResult.Success -> {
                        println("✅ DEBUG: Research questions generated")
                        // Save artifact
                        saveArtifact(projectId, result.data, "ResearchQuestions.json")
                        "✅ Research questions generated successfully"
                    }
                    is StageResult.RequiresApproval -> {
                        println("⚠️ DEBUG: Questions require approval")
                        // Save artifact
                        saveArtifact(projectId, result.data, "ResearchQuestions.json")
                        buildString {
                            appendLine("✅ Questions generated - Review required")
                            appendLine()
                            appendLine("Primary: ${result.data.primary.text}")
                            appendLine()
                            result.data.secondary.forEachIndexed { i, q ->
                                appendLine("Secondary ${i+1}: ${q.text}")
                            }
                            if (result.suggestions.isNotEmpty()) {
                                appendLine()
                                appendLine("⚠️ Warnings:")
                                result.suggestions.forEach {
                                    appendLine("  • $it")
                                }
                            }
                        }
                    }
                    is StageResult.Failure -> {
                        println("❌ DEBUG: Research questions generation failed: ${result.error.message}")

                        // If LLM failed, offer manual entry
                        if (result.error.message.contains("API") || result.error.message.contains("LLM")) {
                            // Create empty questions for manual entry
                            val emptyQuestions = ResearchQuestions(
                                primary = ResearchQuestion(
                                    id = "primary_1",
                                    text = "",
                                    type = QuestionType.PRIMARY,
                                    rationale = null,
                                    picoMapping = PicoMapping(
                                        population = pico.population,
                                        intervention = pico.intervention,
                                        comparison = pico.comparison,
                                        outcome = pico.outcome
                                    )
                                ),
                                secondary = emptyList(),
                                approved = false,
                                generatedAt = kotlinx.datetime.Clock.System.now()
                            )
                            saveArtifact(projectId, emptyQuestions, "ResearchQuestions.json")
                            "❌ LLM failed: ${result.error.message}\n\nClick 'View & Approve' to enter questions manually"
                        } else {
                            "❌ Failed: ${result.error.message}"
                        }
                    }
                }
            }
            StageType.CONCEPT -> {
                val pico = loadArtifact<ProblemFraming>(projectId, "ProblemFraming.json") ?: return "Missing PICO"
                val stage = StageKoinHelper.getConceptStage()
                when (val result = stage.execute(pico)) {
                    is StageResult.Success -> "✅ Concepts expanded"
                    is StageResult.RequiresApproval -> "⚠️ Concepts generated, needs review"
                    is StageResult.Failure -> "❌ Failed: ${result.error.message}"
                }
            }
            StageType.QUERY -> {
                val concepts = loadArtifact<ConceptExpansion>(projectId, "ConceptExpansion.json") ?: return "Missing concepts"
                val stage = StageKoinHelper.getQueryStage()
                when (val result = stage.execute(concepts)) {
                    is StageResult.Success -> "✅ Queries generated"
                    is StageResult.RequiresApproval -> "⚠️ Queries need approval"
                    is StageResult.Failure -> "❌ Failed: ${result.error.message}"
                }
            }
            StageType.TEST -> {
                val queryPlan = loadArtifact<DatabaseQueryPlan>(projectId, "DatabaseQueryPlan.json") ?: return "Missing query plan"
                val stage = StageKoinHelper.getTestStage()
                when (val result = stage.execute(queryPlan)) {
                    is StageResult.Success -> "✅ Test completed"
                    is StageResult.RequiresApproval -> "⚠️ Queries may need refinement"
                    is StageResult.Failure -> "❌ Failed: ${result.error.message}"
                }
            }
            StageType.SEARCH -> {
                val testResult = loadArtifact<TestSearchResult>(projectId, "TestSearchResult.json") ?: return "Missing test results"
                val stage = StageKoinHelper.getSearchStage()
                when (val result = stage.execute(testResult)) {
                    is StageResult.Success -> "✅ Search completed: ${result.data.totalDocuments} documents"
                    is StageResult.RequiresApproval -> "⚠️ Search completed"
                    is StageResult.Failure -> "❌ Failed: ${result.error.message}"
                }
            }
            StageType.DEDUP -> {
                val searchResults = loadArtifact<SearchResults>(projectId, "SearchResults.json") ?: return "Missing search results"
                val stage = StageKoinHelper.getDedupStage()
                when (val result = stage.execute(searchResults)) {
                    is StageResult.Success -> "✅ Deduplicated: ${result.data.uniqueDocuments} unique from ${result.data.totalDocuments}"
                    is StageResult.RequiresApproval -> "⚠️ Deduplication complete"
                    is StageResult.Failure -> "❌ Failed: ${result.error.message}"
                }
            }
        }
    } catch (e: Exception) {
        println("❌ DEBUG: Exception in runStage:")
        println("❌ DEBUG: Error type: ${e::class.simpleName}")
        println("❌ DEBUG: Error message: ${e.message}")
        println("❌ DEBUG: Stack trace:")
        e.printStackTrace()

        // User-friendly error messages
        val errorMsg = when {
            e.message?.contains("OpenAI API key not configured") == true ->
                "❌ Missing API Key!\n\nSet OPENAI_API_KEY in:\n• Environment variable, OR\n• src/jvmMain/resources/application.conf\n\nSee API_KEY_SETUP.md for help!"
            e.message?.contains("Could not create instance") == true ->
                "❌ Configuration Error: ${e.cause?.message ?: e.message}"
            else ->
                "❌ Error: ${e.message}"
        }

        errorMsg
    }
}

private fun loadProject(projectId: String): Project? {
    val json = Json { ignoreUnknownKeys = true }
    val file = File("data/$projectId/artifacts/Project.json")
    return if (file.exists()) {
        json.decodeFromString<Project>(file.readText())
    } else null
}

private fun approvePico(projectId: String) {
    try {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        // 1. Approve the PICO artifact
        val picoFile = File("data/$projectId/artifacts/ProblemFraming.json")
        if (picoFile.exists()) {
            val pico = json.decodeFromString<ProblemFraming>(picoFile.readText())
            val approved = pico.copy(approved = true)
            picoFile.writeText(json.encodeToString(ProblemFraming.serializer(), approved))
            println("✅ PICO approved for project $projectId")
        }

        // 2. Update project status to enable Stage 2 (Research Questions)
        val projectFile = File("data/$projectId/artifacts/Project.json")
        if (projectFile.exists()) {
            val project = json.decodeFromString<Project>(projectFile.readText())
            val updatedProject = project.copy(
                status = ProjectStatus.RESEARCH_QUESTIONS,
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            projectFile.writeText(json.encodeToString(Project.serializer(), updatedProject))
            println("✅ Project status updated to RESEARCH_QUESTIONS")
        }
    } catch (e: Exception) {
        println("❌ Failed to approve PICO: ${e.message}")
        e.printStackTrace()
    }
}

private inline fun <reified T> loadArtifact(projectId: String, filename: String): T? {
    val json = Json { ignoreUnknownKeys = true }
    val file = File("data/$projectId/artifacts/$filename")
    return if (file.exists()) {
        try {
            json.decodeFromString<T>(file.readText())
        } catch (e: Exception) {
            null
        }
    } else null
}

private inline fun <reified T> saveArtifact(projectId: String, artifact: T, filename: String) {
    try {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val artifactsDir = File("data/$projectId/artifacts")
        artifactsDir.mkdirs()
        val file = File(artifactsDir, filename)

        // Use kotlinx.serialization's serializer() to get the serializer for T
        val serializedJson = json.encodeToString(kotlinx.serialization.serializer<T>(), artifact)
        file.writeText(serializedJson)

        println("✅ Saved $filename for project $projectId")
    } catch (e: Exception) {
        println("❌ Failed to save $filename: ${e.message}")
        e.printStackTrace()
    }
}

