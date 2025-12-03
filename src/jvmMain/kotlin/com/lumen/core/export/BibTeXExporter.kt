package com.lumen.core.export

import com.lumen.core.domain.model.Document

/**
 * Export documents as BibTeX format
 * Compatible with LaTeX, reference managers (Zotero, Mendeley, etc.)
 */
class BibTeXExporter : ExportService {

    override fun export(documents: List<Document>): String {
        return documents.joinToString("\n\n") { doc ->
            generateBibTeXEntry(doc)
        }
    }

    private fun generateBibTeXEntry(doc: Document): String {
        val builder = StringBuilder()

        // Determine entry type (article is most common for papers)
        val entryType = determineEntryType(doc)

        // Generate citation key
        val citationKey = generateCitationKey(doc)

        builder.append("@$entryType{$citationKey,\n")

        // Title (required)
        builder.append("  title = {${escapeBibTeX(doc.title)}},\n")

        // Authors (required for most types)
        if (doc.authors.isNotEmpty()) {
            val authorString = doc.authors.joinToString(" and ")
            builder.append("  author = {${escapeBibTeX(authorString)}},\n")
        }

        // Year
        doc.year?.let {
            builder.append("  year = {$it},\n")
        }

        // DOI
        doc.doi?.let {
            builder.append("  doi = {$it},\n")
        }

        // Journal/Venue
        doc.venue?.let {
            if (entryType == "article") {
                builder.append("  journal = {${escapeBibTeX(it)}},\n")
            } else {
                builder.append("  booktitle = {${escapeBibTeX(it)}},\n")
            }
        }

        // Abstract
        doc.abstract?.let {
            builder.append("  abstract = {${escapeBibTeX(it)}},\n")
        }

        // URL
        doc.url?.let {
            builder.append("  url = {$it},\n")
        }

        // Provider ID as note
        builder.append("  note = {Retrieved from ${doc.provider}: ${doc.providerId}}\n")

        builder.append("}")

        return builder.toString()
    }

    private fun determineEntryType(doc: Document): String {
        // Try to infer type from venue or default to article
        val venue = doc.venue?.lowercase() ?: ""

        return when {
            venue.contains("conference") || venue.contains("proceedings") -> "inproceedings"
            venue.contains("book") -> "book"
            venue.contains("thesis") || venue.contains("dissertation") -> "phdthesis"
            venue.contains("report") || venue.contains("technical") -> "techreport"
            else -> "article" // Default to journal article
        }
    }

    private fun generateCitationKey(doc: Document): String {
        // Format: FirstAuthorLastName_Year_FirstTitleWord
        val firstAuthor = doc.authors.firstOrNull()
            ?.split(",", " ")
            ?.firstOrNull()
            ?.filter { it.isLetterOrDigit() }
            ?: "Unknown"

        val year = doc.year?.toString() ?: "XXXX"

        val firstWord = doc.title
            .split(" ")
            .firstOrNull { it.length > 3 }
            ?.filter { it.isLetterOrDigit() }
            ?.take(10)
            ?: "Document"

        return "${firstAuthor}_${year}_${firstWord}"
    }

    private fun escapeBibTeX(text: String): String {
        // Escape special BibTeX characters
        return text
            .replace("\\", "\\\\")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("%", "\\%")
            .replace("&", "\\&")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("~", "\\~")
            .replace("^", "\\^")
    }
}

