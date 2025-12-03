package com.lumen.core.domain.repository

import com.lumen.core.domain.model.Project

/**
 * Repository interface for Project persistence
 */
interface ProjectRepository {
    suspend fun createProject(project: Project): Result<Project>
    suspend fun getProject(id: String): Result<Project?>
    suspend fun getAllProjects(): Result<List<Project>>
    suspend fun updateProject(project: Project): Result<Project>
    suspend fun deleteProject(id: String): Result<Unit>
}



