package com.lumen.core.domain.repository

import com.lumen.core.domain.model.Paper

/**
 * Repository interface for Paper persistence
 */
interface PaperRepository {
    suspend fun savePaper(paper: Paper): Result<Paper>
    suspend fun getPaper(id: String): Result<Paper?>
    suspend fun getPapersByProject(projectId: String): Result<List<Paper>>
    suspend fun searchPapers(query: String, projectId: String): Result<List<Paper>>
    suspend fun deletePaper(id: String): Result<Unit>
    suspend fun bulkSavePapers(papers: List<Paper>): Result<List<Paper>>
}

