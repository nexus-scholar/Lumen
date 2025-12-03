package com.lumen.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.enum
import com.lumen.cli.commands.ExportCommand
import com.lumen.cli.commands.ListProjectsCommand
import com.lumen.cli.commands.RunStageCommand
import com.lumen.core.di.coreModule
import com.lumen.core.di.jvmModule
import com.lumen.core.domain.model.ReviewType
import com.lumen.core.domain.stages.*
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class LumenCli : CliktCommand(
    name = "lumen",
    help = "Lumen - AI-Powered Systematic Review Tool CLI"
) {
    override fun run() = Unit
}

class NewProjectCommand : CliktCommand(
    name = "new",
    help = "Create a new systematic review project"
), KoinComponent {
    private val question: String by option("-q", "--question", help = "Research question").required()
    private val name: String by option("-n", "--name", help = "Project name").default("")
    private val reviewType: ReviewType by option("-t", "--type", help = "Review type")
        .enum<ReviewType>()
        .default(ReviewType.INTERVENTION)

    override fun run() = runBlocking {
        echo("Creating new systematic review project...")
        echo("Question: $question")
        echo("Type: $reviewType")

        val setupStage: ProjectSetupStage = get()
        val picoStage: PicoExtractionStage = get()

        // Stage 0: Project Setup
        val setupInput = ProjectSetupInput(
            researchIdea = question,
            title = name.ifBlank { null },
            reviewType = reviewType
        )

        val setupResult = setupStage.execute(setupInput)
        when (setupResult) {
            is com.lumen.core.domain.pipeline.StageResult.Success -> {
                val project = setupResult.data
                echo("✅ Project created: ${project.id}")
                echo("   Name: ${project.name}")
                echo("   Status: ${project.status}")

                // Stage 1: PICO Extraction
                echo("\nExtracting PICO components...")
                val picoResult = picoStage.execute(project)
                when (picoResult) {
                    is com.lumen.core.domain.pipeline.StageResult.Success,
                    is com.lumen.core.domain.pipeline.StageResult.RequiresApproval -> {
                        val pico = (picoResult as? com.lumen.core.domain.pipeline.StageResult.Success)?.data
                            ?: (picoResult as com.lumen.core.domain.pipeline.StageResult.RequiresApproval).data
                        echo("✅ PICO extracted:")
                        echo("   Population: ${pico.population}")
                        echo("   Intervention: ${pico.intervention}")
                        echo("   Comparison: ${pico.comparison ?: "N/A"}")
                        echo("   Outcome: ${pico.outcome}")
                    }
                    is com.lumen.core.domain.pipeline.StageResult.Failure -> {
                        echo("⚠️  PICO extraction failed: ${picoResult.error.message}", err = true)
                    }
                }
            }
            is com.lumen.core.domain.pipeline.StageResult.Failure -> {
                echo("❌ Failed to create project: ${setupResult.error.message}", err = true)
            }
            else -> {}
        }
    }
}


fun main(args: Array<String>) {
    // Initialize Koin
    startKoin {
        modules(coreModule, jvmModule)
    }

    try {
        LumenCli()
            .subcommands(
                NewProjectCommand(),
                ListProjectsCommand(),
                RunStageCommand(),
                ExportCommand()
            )
            .main(args)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        stopKoin()
    }
}

