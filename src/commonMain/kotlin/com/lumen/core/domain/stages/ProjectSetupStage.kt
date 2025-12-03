package com.lumen.core.domain.stages

import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import kotlinx.datetime.Clock
import kotlin.random.Random


/**
 * Input for project setup stage
 */
data class ProjectSetupInput(
    val researchIdea: String,
    val title: String? = null,
    val reviewType: ReviewType,
    val authors: List<Author> = emptyList(),
    val fundingSource: String? = null,
    val conflictsOfInterest: String? = null,
    val targetJournal: String? = null,
    val enableVersionControl: Boolean = false
)

/**
 * Stage 0: Project Setup
 * Creates project workspace, initializes database, and saves initial context
 */
class ProjectSetupStage(
    private val artifactStore: ArtifactStore
) : PipelineStage<ProjectSetupInput, Project> {

    override val stageName: String = "Stage 0: Project Setup"

    override suspend fun execute(input: ProjectSetupInput): StageResult<Project> {
        return try {
            // Generate unique project ID
            val projectId = generateProjectId()

            // Create workspace and initialize database
            val workspaceResult = artifactStore.initializeProjectWorkspace(projectId)
            if (workspaceResult.isFailure) {
                return StageResult.Failure(
                    PipelineError.StorageFailed(
                        "Failed to create workspace: ${workspaceResult.exceptionOrNull()?.message}",
                        workspaceResult.exceptionOrNull()
                    )
                )
            }


            // Create project context
            val now = Clock.System.now()
            val project = Project(
                id = projectId,
                name = input.title ?: generateDefaultTitle(input.researchIdea),
                description = input.researchIdea,
                rawIdea = input.researchIdea,
                reviewType = input.reviewType,
                authors = input.authors,
                fundingSource = input.fundingSource,
                conflictsOfInterest = input.conflictsOfInterest,
                targetJournal = input.targetJournal,
                createdAt = now,
                updatedAt = now,
                status = ProjectStatus.CREATED,
                currentStage = stageName,
                enableVersionControl = input.enableVersionControl
            )

            // Save project artifact
            val saveResult = artifactStore.save(
                projectId = projectId,
                artifact = project,
                serializer = Project.serializer(),
                filename = "Project.json"
            )

            if (saveResult.isFailure) {
                return StageResult.Failure(
                    PipelineError.StorageFailed(
                        "Failed to save project artifact: ${saveResult.exceptionOrNull()?.message}",
                        saveResult.exceptionOrNull()
                    )
                )
            }

            StageResult.Success(project)

        } catch (e: Exception) {
            StageResult.Failure(
                PipelineError.Unknown(
                    "Project setup failed: ${e.message}",
                    e
                )
            )
        }
    }

    private fun generateProjectId(): String {
        val timestamp = Clock.System.now().epochSeconds
        val random = Random.nextInt(1000, 9999)
        return "project_${timestamp}_$random"
    }

    private fun generateDefaultTitle(researchIdea: String): String {
        // Take first 50 characters and clean up
        val cleaned = researchIdea
            .take(50)
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .trim()

        return if (cleaned.isNotEmpty()) cleaned else "Systematic Review Project"
    }
}

