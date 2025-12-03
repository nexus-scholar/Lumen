package com.lumen.core.domain.pipeline

import kotlinx.serialization.KSerializer

/**
 * Interface for storing and loading pipeline stage artifacts as JSON files
 */
interface ArtifactStore {
    /**
     * Save an artifact to the project's artifacts directory
     * @param projectId The project ID
     * @param artifact The artifact to save
     * @param serializer The serializer for the artifact type
     * @param filename Optional custom filename (defaults to artifact class name)
     */
    suspend fun <T> save(
        projectId: String,
        artifact: T,
        serializer: KSerializer<T>,
        filename: String? = null
    ): Result<Unit>

    /**
     * Load an artifact from the project's artifacts directory
     * @param projectId The project ID
     * @param serializer The serializer for the artifact type
     * @param filename Optional custom filename (defaults to T class name)
     */
    suspend fun <T> load(
        projectId: String,
        serializer: KSerializer<T>,
        filename: String? = null
    ): Result<T>

    /**
     * Check if an artifact exists
     */
    suspend fun exists(projectId: String, filename: String): Boolean

    /**
     * List all artifacts for a project
     */
    suspend fun listArtifacts(projectId: String): List<String>

    /**
     * Save an error artifact for debugging
     */
    suspend fun saveError(
        projectId: String,
        stageName: String,
        error: PipelineError,
        context: Map<String, Any?> = emptyMap()
    ): Result<Unit>

    /**
     * Initialize project workspace (create directories, database, etc.)
     */
    suspend fun initializeProjectWorkspace(projectId: String): Result<Unit>
}

