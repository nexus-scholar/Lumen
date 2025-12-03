package com.lumen.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.core.domain.model.Document
import com.lumen.core.export.BibTeXExporter
import com.lumen.core.export.CsvExporter
import com.lumen.core.export.JsonLinesExporter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun ResultsBrowserScreen(
    projectId: String,
    onBack: () -> Unit
) {
    var documents by remember { mutableStateOf<List<Document>>(emptyList()) }
    var filteredDocuments by remember { mutableStateOf<List<Document>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedDocument by remember { mutableStateOf<Document?>(null) }
    val scope = rememberCoroutineScope()

    // Load documents on composition
    LaunchedEffect(projectId) {
        scope.launch {
            try {
                documents = loadDocuments(projectId)
                filteredDocuments = documents
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load documents: ${e.message}"
                isLoading = false
            }
        }
    }

    // Filter documents when search query changes
    LaunchedEffect(searchQuery) {
        filteredDocuments = if (searchQuery.isBlank()) {
            documents
        } else {
            documents.filter { doc ->
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.authors.any { it.contains(searchQuery, ignoreCase = true) } ||
                doc.abstract?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }

                    Text(
                        "Search Results",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )

                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, "Filters")
                    }

                    ExportMenu(projectId, documents)
                }

                Spacer(Modifier.height(8.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by title, authors, or abstract...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Stats
                Text(
                    "Showing ${filteredDocuments.size} of ${documents.size} documents",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            documents.isEmpty() -> {
                EmptyResultsView()
            }

            else -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Document list
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDocuments) { doc ->
                            DocumentListItem(
                                document = doc,
                                isSelected = selectedDocument?.id == doc.id,
                                onClick = { selectedDocument = doc }
                            )
                        }
                    }

                    // Detail panel
                    if (selectedDocument != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                        }
                        DocumentDetailPanel(
                            document = selectedDocument!!,
                            onClose = { selectedDocument = null },
                            modifier = Modifier.width(400.dp).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResultsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Text(
                "No Documents Found",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This project doesn't have any documents yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Run the search execution stage to retrieve documents.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DocumentListItem(
    document: Document,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = document.authors.take(3).joinToString(", ") +
                      if (document.authors.size > 3) " et al." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                document.year?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                document.citationCount?.let {
                    Text(
                        text = "Citations: $it",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = document.provider,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DocumentDetailPanel(
    document: Document,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Document Details",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onClose) {
                    Text("×", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Scrollable content
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DetailSection("Title") {
                        Text(
                            document.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                item {
                    DetailSection("Authors") {
                        document.authors.forEach { author ->
                            Text("• $author", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item {
                    DetailSection("Metadata") {
                        document.year?.let {
                            MetadataRow("Year", it.toString())
                        }
                        document.doi?.let {
                            MetadataRow("DOI", it)
                        }
                        document.venue?.let {
                            MetadataRow("Venue", it)
                        }
                        document.citationCount?.let {
                            MetadataRow("Citations", it.toString())
                        }
                        MetadataRow("Provider", document.provider)
                    }
                }

                document.abstract?.let { abstract ->
                    item {
                        DetailSection("Abstract") {
                            Text(
                                abstract,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                document.url?.let { url ->
                    item {
                        DetailSection("Link") {
                            Text(
                                url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ExportMenu(
    projectId: String,
    documents: List<Document>
) {
    var expanded by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FileDownload, "Export")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export as CSV") },
                onClick = {
                    expanded = false
                    scope.launch {
                        isExporting = true
                        try {
                            val exporter = CsvExporter()
                            val content = exporter.export(documents)
                            val outputFile = File("data/$projectId/export/documents.csv")
                            outputFile.parentFile?.mkdirs()
                            outputFile.writeText(content)
                            exportMessage = "Exported ${documents.size} documents to CSV"
                        } catch (e: Exception) {
                            exportMessage = "Export failed: ${e.message}"
                        }
                        isExporting = false
                    }
                }
            )

            DropdownMenuItem(
                text = { Text("Export as JSONL") },
                onClick = {
                    expanded = false
                    scope.launch {
                        isExporting = true
                        try {
                            val exporter = JsonLinesExporter()
                            val content = exporter.export(documents)
                            val outputFile = File("data/$projectId/export/documents.jsonl")
                            outputFile.parentFile?.mkdirs()
                            outputFile.writeText(content)
                            exportMessage = "Exported ${documents.size} documents to JSONL"
                        } catch (e: Exception) {
                            exportMessage = "Export failed: ${e.message}"
                        }
                        isExporting = false
                    }
                }
            )

            DropdownMenuItem(
                text = { Text("Export as BibTeX") },
                onClick = {
                    expanded = false
                    scope.launch {
                        isExporting = true
                        try {
                            val exporter = BibTeXExporter()
                            val content = exporter.export(documents)
                            val outputFile = File("data/$projectId/export/documents.bib")
                            outputFile.parentFile?.mkdirs()
                            outputFile.writeText(content)
                            exportMessage = "Exported ${documents.size} documents to BibTeX"
                        } catch (e: Exception) {
                            exportMessage = "Export failed: ${e.message}"
                        }
                        isExporting = false
                    }
                }
            )
        }
    }

    // Export feedback
    if (isExporting) {
        Snackbar { Text("Exporting...") }
    }

    exportMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            exportMessage = null
        }
        Snackbar { Text(message) }
    }
}

private fun loadDocuments(projectId: String): List<Document> {
    val json = Json {
        ignoreUnknownKeys = true
    }

    val documents = mutableListOf<Document>()
    val dbFile = File("data/$projectId/project.db")

    if (!dbFile.exists()) {
        return emptyList()
    }

    try {
        val connection = java.sql.DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.use { conn ->
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT json_data FROM documents WHERE project_id = '$projectId'"
            )

            while (resultSet.next()) {
                try {
                    val jsonData = resultSet.getString("json_data")
                    val document = json.decodeFromString<Document>(jsonData)
                    documents.add(document)
                } catch (e: Exception) {
                    // Skip invalid documents
                }
            }
        }
    } catch (e: Exception) {
        // Return empty list if database access fails
    }

    return documents.sortedByDescending { it.year ?: 0 }
}

