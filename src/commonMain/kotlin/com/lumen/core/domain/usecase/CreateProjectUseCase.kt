package com.lumen.core.domain.usecase

import com.lumen.core.domain.model.*
import com.lumen.core.domain.repository.ProjectRepository
import kotlinx.datetime.Clock

/**
 * Use case for creating a new systematic review project
 */
class CreateProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        reviewType: ReviewType = ReviewType.INTERVENTION,
        authors: List<Author> = emptyList()
    ): Result<Project> {
        val now = Clock.System.now()
        val project = Project(
            id = generateProjectId(),
            name = name,
            description = description,
            rawIdea = description,
            reviewType = reviewType,
            authors = authors,
            createdAt = now,
            updatedAt = now,
            status = ProjectStatus.CREATED
        )

        return projectRepository.createProject(project)
    }

    private fun generateProjectId(): String {
        return "project_${Clock.System.now().toEpochMilliseconds()}"
    }
}

