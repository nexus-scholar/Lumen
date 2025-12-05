package com.lumen.search.domain.ports

import com.lumen.search.domain.models.ScholarlyDocument

/**
 * Result from a provider search operation
 */
sealed interface ProviderResult {
    /** Successful result with documents */
    data class Success(
        val documents: List<ScholarlyDocument>,
        val totalCount: Int,
        val hasMore: Boolean
    ) : ProviderResult

    /** Error result with retry information */
    data class Error(
        val throwable: Throwable,
        val providerId: String,
        val canRetry: Boolean,
        val retryAfterMs: Long? = null
    ) : ProviderResult
}

