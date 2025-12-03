package com.lumen.core.domain.stages

import com.lumen.core.domain.model.Document
import com.lumen.core.domain.model.SearchResults
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.PipelineError
import com.lumen.core.domain.pipeline.PipelineStage
import com.lumen.core.domain.pipeline.StageResult
import kotlinx.serialization.Serializable

/**
 * Stage 9: Deduplication
 * Identifies and groups duplicate documents across search results
 */
class DeduplicationStage(
    private val artifactStore: ArtifactStore,
    private val saveGroups: suspend (String, List<DuplicateGroup>) -> Unit
) : PipelineStage<SearchResults, DeduplicationResult> {

    override val stageName: String = "Stage 9: Deduplication"

    companion object {
        const val TITLE_SIMILARITY_THRESHOLD = 0.85
        const val AUTHOR_OVERLAP_THRESHOLD = 0.5
    }

    override suspend fun execute(input: SearchResults): StageResult<DeduplicationResult> {
        return try {
            val allDocuments = input.results.values.flatMap { it.documents }

            // Group duplicates
            val groups = identifyDuplicates(allDocuments)

            // Save deduplication groups to database
            saveGroups(input.projectId, groups)

            // Create result
            val uniqueDocuments = groups.map { it.representativeDocument }
            val result = DeduplicationResult(
                totalDocuments = allDocuments.size,
                uniqueDocuments = uniqueDocuments.size,
                duplicateGroups = groups.size,
                groups = groups
            )

            // Save artifact
            artifactStore.save(
                projectId = input.projectId,
                artifact = result,
                serializer = DeduplicationResult.serializer(),
                filename = "DeduplicationResult.json"
            )

            StageResult.Success(result)

        } catch (e: Exception) {
            StageResult.Failure(
                PipelineError.Unknown(
                    "Deduplication failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Identify duplicate documents using multiple strategies
     */
    private fun identifyDuplicates(documents: List<Document>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val processed = mutableSetOf<String>()

        documents.forEach { doc ->
            if (doc.id !in processed) {
                // Find all duplicates of this document
                val duplicates = mutableListOf(doc)
                processed.add(doc.id)

                // Check remaining documents
                documents.forEach { other ->
                    if (other.id !in processed && isDuplicate(doc, other)) {
                        duplicates.add(other)
                        processed.add(other.id)
                    }
                }

                // Create group if duplicates found
                if (duplicates.size > 1) {
                    groups.add(
                        DuplicateGroup(
                            representativeDocument = selectRepresentative(duplicates),
                            duplicates = duplicates,
                            method = determineDuplicationMethod(duplicates)
                        )
                    )
                } else {
                    // Single document (no duplicates)
                    groups.add(
                        DuplicateGroup(
                            representativeDocument = doc,
                            duplicates = listOf(doc),
                            method = "none"
                        )
                    )
                }
            }
        }

        return groups
    }

    /**
     * Check if two documents are duplicates
     */
    private fun isDuplicate(doc1: Document, doc2: Document): Boolean {
        // Strategy 1: Exact DOI match (highest confidence)
        if (doc1.doi != null && doc2.doi != null && doc1.doi == doc2.doi) {
            return true
        }

        // Strategy 2: Fuzzy title match
        val titleSimilarity = calculateTitleSimilarity(doc1.title, doc2.title)
        if (titleSimilarity >= TITLE_SIMILARITY_THRESHOLD) {
            // Verify with author overlap if available
            if (doc1.authors.isNotEmpty() && doc2.authors.isNotEmpty()) {
                val authorOverlap = calculateAuthorOverlap(doc1.authors, doc2.authors)
                return authorOverlap >= AUTHOR_OVERLAP_THRESHOLD
            }
            return true
        }

        return false
    }

    /**
     * Calculate similarity between two titles using Levenshtein distance
     */
    private fun calculateTitleSimilarity(title1: String, title2: String): Double {
        val normalized1 = normalizeTitle(title1)
        val normalized2 = normalizeTitle(title2)

        if (normalized1 == normalized2) return 1.0

        val distance = levenshteinDistance(normalized1, normalized2)
        val maxLength = maxOf(normalized1.length, normalized2.length)

        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Normalize title for comparison
     */
    private fun normalizeTitle(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Calculate author overlap between two author lists
     */
    private fun calculateAuthorOverlap(authors1: List<String>, authors2: List<String>): Double {
        val normalized1 = authors1.map { normalizeAuthorName(it) }.toSet()
        val normalized2 = authors2.map { normalizeAuthorName(it) }.toSet()

        val intersection = normalized1.intersect(normalized2).size
        val union = normalized1.union(normalized2).size

        return if (union > 0) intersection.toDouble() / union else 0.0
    }

    /**
     * Normalize author name for comparison
     */
    private fun normalizeAuthorName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 1 } // Remove initials
            .sorted()
            .joinToString(" ")
    }

    /**
     * Select representative document from a group of duplicates
     */
    private fun selectRepresentative(duplicates: List<Document>): Document {
        // Prefer documents with DOI
        val withDoi = duplicates.filter { it.doi != null }
        if (withDoi.isNotEmpty()) {
            return withDoi.maxByOrNull { it.citationCount ?: 0 } ?: withDoi.first()
        }

        // Otherwise, prefer most cited
        return duplicates.maxByOrNull { it.citationCount ?: 0 } ?: duplicates.first()
    }

    /**
     * Determine which method was used to identify duplicates
     */
    private fun determineDuplicationMethod(duplicates: List<Document>): String {
        if (duplicates.size == 1) return "none"

        val hasDoi = duplicates.all { it.doi != null }
        return if (hasDoi) "doi" else "title_fuzzy"
    }
}

/**
 * Result of deduplication stage
 */
@Serializable
data class DeduplicationResult(
    val totalDocuments: Int,
    val uniqueDocuments: Int,
    val duplicateGroups: Int,
    val groups: List<DuplicateGroup>
)

@Serializable
data class DuplicateGroup(
    val representativeDocument: Document,
    val duplicates: List<Document>,
    val method: String
)

