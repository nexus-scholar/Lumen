package com.lumen.core.export

import com.lumen.core.domain.model.Document
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Export formats supported by Lumen
 */
enum class ExportFormat {
    JSONL,
    CSV,
    BIBTEX,
    RIS
}

/**
 * Service for exporting documents to various formats
 */
interface ExportService {
    /**
     * Export documents to the specified format
     * @param documents List of documents to export
     * @return Exported content as string
     */
    fun export(documents: List<Document>): String
}

/**
 * Export documents as JSON Lines (one JSON object per line)
 * Format is suitable for streaming and big data processing
 */
class JsonLinesExporter : ExportService {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun export(documents: List<Document>): String {
        return documents.joinToString("\n") { doc ->
            json.encodeToString(doc)
        }
    }
}

