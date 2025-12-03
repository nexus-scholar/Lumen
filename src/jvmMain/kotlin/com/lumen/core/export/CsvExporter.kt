package com.lumen.core.export

import com.lumen.core.domain.model.Document
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

/**
 * Export documents as CSV (Comma-Separated Values)
 * Format is compatible with Excel, Google Sheets, and other spreadsheet tools
 */
class CsvExporter : ExportService {

    override fun export(documents: List<Document>): String {
        val writer = StringWriter()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader(
                "ID",
                "Title",
                "Authors",
                "Year",
                "DOI",
                "URL",
                "Venue",
                "Provider",
                "Citation Count",
                "Abstract"
            )
            .build()

        val printer = CSVPrinter(writer, csvFormat)

        documents.forEach { doc ->
            printer.printRecord(
                doc.id,
                doc.title,
                doc.authors.joinToString("; "),
                doc.year ?: "",
                doc.doi ?: "",
                doc.url ?: "",
                doc.venue ?: "",
                doc.provider,
                doc.citationCount ?: "",
                doc.abstract?.take(500) ?: "" // Limit abstract length for readability
            )
        }

        printer.flush()
        return writer.toString()
    }
}

