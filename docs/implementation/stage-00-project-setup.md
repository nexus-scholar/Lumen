# Stage 0: Project Setup

**Purpose:** Initialize a new systematic review project with metadata and workspace.

## Data Models

```kotlin
@Serializable
data class ProjectContext(
    val id: String,
    val rawIdea: String,
    val title: String? = null,
    val reviewType: ReviewType,
    val authors: List<Author> = emptyList(),
    val fundingSource: String? = null,
    val conflictsOfInterest: String? = null,
    val targetJournal: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: ProjectStatus = ProjectStatus.DRAFT
)

@Serializable
enum class ReviewType {
    INTERVENTION,
    DIAGNOSTIC,
    PROGNOSTIC,
    SCOPING,
    QUALITATIVE
}

@Serializable
enum class ProjectStatus {
    DRAFT,
    PROTOCOL_REGISTERED,
    SEARCHING,
    SCREENING,
    EXTRACTING,
    SYNTHESIZING,
    COMPLETED
}

@Serializable
data class Author(
    val fullName: String,
    val email: String? = null,
    val orcid: String? = null,
    val affiliation: String? = null,
    val role: AuthorRole
)

@Serializable
enum class AuthorRole {
    LEAD,
    CO_INVESTIGATOR,
    REVIEWER,
    ADVISOR
}
```

## Implementation

```kotlin
class ProjectSetupStage : PipelineStage<ProjectSetupInput, ProjectContext> {
    
    override suspend fun execute(input: ProjectSetupInput): StageResult<ProjectContext> {
        // Generate unique project ID
        val projectId = generateProjectId()
        
        // Create workspace directory
        val workspace = createWorkspace(projectId)
        
        // Initialize context
        val context = ProjectContext(
            id = projectId,
            rawIdea = input.researchIdea,
            title = input.title,
            reviewType = input.reviewType,
            authors = input.authors,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Save artifact
        artifactStore.save(projectId, context, ProjectContext.serializer())
        
        // Initialize SQLite database
        initializeDatabase(projectId)
        
        // Initialize Git repository (optional)
        if (input.enableVersionControl) {
            initializeGitRepo(workspace)
        }
        
        return StageResult.Success(context)
    }
    
    private fun generateProjectId(): String {
        val timestamp = Clock.System.now().epochSeconds
        return "project_${timestamp}_${Random.nextInt(1000, 9999)}"
    }
    
    private fun createWorkspace(projectId: String): File {
        val workspace = File("data/$projectId")
        workspace.mkdirs()
        
        // Create subdirectories
        File(workspace, "artifacts").mkdir()
        File(workspace, "export").mkdir()
        
        // Create README
        File(workspace, "README.md").writeText("""
            # Systematic Review Project: $projectId
            
            Created: ${Clock.System.now()}
            
            ## Directory Structure
            - `artifacts/`: JSON files for each pipeline stage
            - `project.db`: SQLite database (papers, screening, extraction)
            - `export/`: Generated outputs (CSV, BibTeX, PRISMA protocol)
        """.trimIndent())
        
        return workspace
    }
    
    private fun initializeDatabase(projectId: String) {
        val dbPath = "data/$projectId/project.db"
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS documents (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                title TEXT NOT NULL,
                abstract TEXT,
                year INTEGER,
                doi TEXT,
                provider TEXT,
                provider_id TEXT,
                json_data TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        connection.close()
    }
    
    private fun initializeGitRepo(workspace: File) {
        val git = Git.init()
            .setDirectory(workspace)
            .call()
        
        // Initial commit
        git.add().addFilepattern(".").call()
        git.commit()
            .setMessage("Initial project setup")
            .setAuthor("Lumen", "lumen@localhost")
            .call()
    }
}

data class ProjectSetupInput(
    val researchIdea: String,
    val title: String? = null,
    val reviewType: ReviewType,
    val authors: List<Author> = emptyList(),
    val fundingSource: String? = null,
    val targetJournal: String? = null,
    val enableVersionControl: Boolean = true
)
```

## Desktop UI

```kotlin
@Composable
fun ProjectSetupScreen(viewModel: ProjectSetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            "Create New Systematic Review",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Research idea
        OutlinedTextField(
            value = viewModel.researchIdea,
            onValueChange = { viewModel.researchIdea = it },
            label = { Text("Research Question") },
            placeholder = { Text("What are AI methods for crop disease detection?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Review type
        DropdownMenu(
            expanded = viewModel.showReviewTypeMenu,
            onDismissRequest = { viewModel.showReviewTypeMenu = false }
        ) {
            ReviewType.values().forEach { type ->
                DropdownMenuItem(
                    onClick = {
                        viewModel.reviewType = type
                        viewModel.showReviewTypeMenu = false
                    }
                ) {
                    Text(type.name.replace('_', ' '))
                }
            }
        }
        
        Button(
            onClick = { viewModel.showReviewTypeMenu = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Review Type: ${viewModel.reviewType.name}")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Optional: Authors
        Text("Authors (optional)", style = MaterialTheme.typography.titleSmall)
        viewModel.authors.forEach { author ->
            AuthorCard(author, viewModel)
        }
        Button(onClick = { viewModel.addAuthor() }) {
            Text("+ Add Author")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { viewModel.cancel() },
                colors = ButtonDefaults.textButtonColors()
            ) {
                Text("Cancel")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = { viewModel.createProject() },
                enabled = viewModel.researchIdea.isNotBlank()
            ) {
                Text("Create Project →")
            }
        }
    }
}
```

## CLI

```bash
# Create project interactively
lumen new

# Create with inline idea
lumen new "AI methods for crop disease detection"

# Create with options
lumen new "AI methods for crop disease detection" \
  --type intervention \
  --title "Systematic Review of AI in Agriculture" \
  --author "John Doe <john@example.com>" \
  --no-git
```

## Tests

```kotlin
class ProjectSetupStageTest {
    
    @Test
    fun `creates project with valid input`() = runTest {
        val input = ProjectSetupInput(
            researchIdea = "Test research question",
            reviewType = ReviewType.SCOPING
        )
        
        val stage = ProjectSetupStage()
        val result = stage.execute(input)
        
        assertIs<StageResult.Success>(result)
        val context = result.data
        
        assertTrue(context.id.startsWith("project_"))
        assertEquals("Test research question", context.rawIdea)
        assertEquals(ReviewType.SCOPING, context.reviewType)
    }
    
    @Test
    fun `creates workspace directories`() = runTest {
        val input = ProjectSetupInput(
            researchIdea = "Test",
            reviewType = ReviewType.INTERVENTION
        )
        
        val stage = ProjectSetupStage()
        val result = stage.execute(input)
        
        val context = (result as StageResult.Success).data
        val workspace = File("data/${context.id}")
        
        assertTrue(workspace.exists())
        assertTrue(File(workspace, "artifacts").exists())
        assertTrue(File(workspace, "export").exists())
        assertTrue(File(workspace, "README.md").exists())
    }
    
    @Test
    fun `initializes SQLite database`() = runTest {
        val input = ProjectSetupInput(
            researchIdea = "Test",
            reviewType = ReviewType.DIAGNOSTIC
        )
        
        val stage = ProjectSetupStage()
        val result = stage.execute(input)
        
        val context = (result as StageResult.Success).data
        val dbPath = "data/${context.id}/project.db"
        
        assertTrue(File(dbPath).exists())
        
        // Verify schema
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val rs = conn.metaData.getTables(null, null, "documents", null)
        assertTrue(rs.next())
        conn.close()
    }
}
```

## Edge Cases

1. **Duplicate project ID**: Retry with new random suffix
2. **Workspace already exists**: Prompt user to delete or choose new location
3. **Disk space**: Check available space before creating workspace
4. **Git not available**: Skip Git init gracefully
5. **Invalid characters in research idea**: Sanitize for file paths

## Next Stage

→ [Stage 1: PICO Extraction](stage-01-pico-extraction.md)
