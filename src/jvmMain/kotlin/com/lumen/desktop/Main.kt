package com.lumen.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lumen.core.di.coreModule
import com.lumen.desktop.ui.CreateProjectDialog
import com.lumen.desktop.ui.ProjectListScreen
import com.lumen.desktop.ui.ProjectDetailScreen
import com.lumen.desktop.ui.ResultsBrowserScreen
import org.koin.core.context.startKoin

@Composable
fun LumenApp() {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var showResults by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                selectedProjectId == null -> {
                    // Show project list
                    ProjectListScreen(
                        onCreateNew = { showCreateDialog = true },
                        onOpenProject = { projectId ->
                            selectedProjectId = projectId
                            showResults = false
                        }
                    )
                }
                showResults -> {
                    // Show results browser
                    ResultsBrowserScreen(
                        projectId = selectedProjectId!!,
                        onBack = { showResults = false }
                    )
                }
                else -> {
                    // Show project detail with stage controls
                    ProjectDetailScreen(
                        projectId = selectedProjectId!!,
                        onBack = { selectedProjectId = null },
                        onViewResults = { showResults = true }
                    )
                }
            }

            // Create project dialog
            if (showCreateDialog) {
                CreateProjectDialog(
                    onDismiss = { showCreateDialog = false },
                    onProjectCreated = { projectId ->
                        showCreateDialog = false
                        selectedProjectId = projectId
                    }
                )
            }
        }
    }
}


@Composable
@Preview
fun App() {
    LumenApp()
}

fun main() {
    // Initialize Koin
    startKoin {
        modules(coreModule, com.lumen.core.di.jvmModule)
    }

    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Lumen - Systematic Review Tool",
            state = windowState
        ) {
            App()
        }
    }
}

