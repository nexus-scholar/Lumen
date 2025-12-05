package com.lumen.search.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The core domain model for scholarly documents.
 * Handles the "Swiss Cheese" problem (missing data) via nullable fields and fusion.
 *
 * Implements the "Sidecar" pattern: preserves raw API responses in [rawSourceData]
 * so AI agents can access provider-specific fields not mapped to the schema.
 */
@Serializable
data class ScholarlyDocument(
    // --- IDENTITY ---
    /** Internal Lumen ID (e.g., "oa:W20034...", "ss:abc123") */
    val lumenId: String,
    /** The universal key for merging across providers */
    val doi: String?,
    /** Origin provider ("openalex", "semanticscholar", "crossref", "arxiv") */
    val sourceProvider: String,

    // --- DISCOVERY LAYER (Phase 1 - Lite) ---
    val title: String,
    val authors: List<Author>,
    val publicationYear: Int?,
    val venue: String?,
    val citationCount: Int,
    val pdfUrl: String?,

    // --- ENRICHMENT LAYER (Phase 2 - Deep) ---
    /** Full abstract text; null if not yet fetched */
    val abstract: String? = null,
    /** AI-generated summary from Semantic Scholar */
    val tldr: String? = null,
    /** Topic concepts with relevance scores */
    val concepts: List<Concept> = emptyList(),
    /** List of referenced DOIs/IDs */
    val references: List<String> = emptyList(),
    /** List of citing DOIs/IDs */
    val citations: List<String> = emptyList(),

    // --- THE SIDECAR (Raw Data Preservation) ---
    /**
     * Preserves the original JSON from each API.
     * Keys are provider IDs ("openalex", "crossref"), values are raw JSON objects.
     * UI reads normalized fields; AI can inspect sidecar for hidden details.
     */
    val rawSourceData: Map<String, JsonObject> = emptyMap(),

    // --- STATE METADATA ---
    /** True if Phase 2 enrichment has been completed */
    val isFullyHydrated: Boolean = false,
    /** Confidence score (0.0-1.0); lower if fuzzy matched */
    val retrievalConfidence: Double = 1.0,
    /** IDs from other providers that were merged into this document */
    val mergedFromIds: List<String> = emptyList()
)

