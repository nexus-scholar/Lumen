package com.lumen.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.domain.stages.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

class RunStageCommand : CliktCommand(
    name = "run",
    help = "Run a specific pipeline stage for a project"
), KoinComponent {
    private val projectId: String by option(
        "--project", "-p",
        help = "Project ID"
    ).required()

    private val stageName: String by option(
        "--stage", "-s",
        help = "Stage to run (pico, concept, query, test, search, dedup)"
    ).choice(
        "pico", "1",
        "concept", "3",
        "query", "4",
        "test", "4.5",
        "search", "7",
        "dedup", "9"
    ).required()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override fun run() = runBlocking {
        echo("🔧 Running stage: $stageName for project $projectId")

        // Verify project exists
        val projectDir = File("data/$projectId")
        if (!projectDir.exists()) {
            echo("❌ Error: Project $projectId not found", err = true)
            echo("   Run 'lumen list' to see available projects")
            return@runBlocking
        }

        // Map stage names to numbers
        val stageNumber = when (stageName) {
            "pico", "1" -> "1"
            "concept", "3" -> "3"
            "query", "4" -> "4"
            "test", "4.5" -> "4.5"
            "search", "7" -> "7"
            "dedup", "9" -> "9"
            else -> stageName
        }

        echo("   Stage: $stageNumber")

        try {
            when (stageNumber) {
                "1" -> runPicoExtraction()
                "3" -> runConceptExpansion()
                "4" -> runQueryGeneration()
                "4.5" -> runTestAndRefine()
                "7" -> runSearchExecution()
                "9" -> runDeduplication()
                else -> {
                    echo("❌ Unknown stage: $stageName", err = true)
                }
            }
        } catch (e: Exception) {
            echo("❌ Stage execution failed: ${e.message}", err = true)
            e.printStackTrace()
        }
    }

    private suspend fun runPicoExtraction() {
        echo("   Loading project...")
        val project = loadArtifact<Project>("Project.json")
            ?: run {
                echo("❌ Could not load project", err = true)
                return
            }

        echo("   Executing PICO extraction...")
        val stage: PicoExtractionStage = get()

        when (val result = stage.execute(project)) {
            is StageResult.Success -> {
                echo("✅ PICO extraction completed")
                displayPico(result.data)
            }
            is StageResult.RequiresApproval -> {
                echo("⚠️  PICO extraction requires approval")
                displayPico(result.data)
                echo("\nSuggestions:")
                result.suggestions.forEach { echo("   • $it") }
            }
            is StageResult.Failure -> {
                echo("❌ PICO extraction failed: ${result.error.message}", err = true)
            }
        }
    }

    private suspend fun runConceptExpansion() {
        echo("   Loading PICO...")
        val pico = loadArtifact<ProblemFraming>("ProblemFraming.json")
            ?: run {
                echo("❌ Could not load PICO. Run 'lumen run -p $projectId -s pico' first", err = true)
                return
            }

        if (!pico.approved) {
            echo("⚠️  Warning: PICO not approved. Results may need review.")
        }

        echo("   Executing concept expansion...")
        val stage: ConceptExpansionStage = get()

        when (val result = stage.execute(pico)) {
            is StageResult.Success -> {
                echo("✅ Concept expansion completed")
                displayConceptExpansion(result.data)
            }
            is StageResult.RequiresApproval -> {
                echo("⚠️  Concept expansion requires approval")
                displayConceptExpansion(result.data)
            }
            is StageResult.Failure -> {
                echo("❌ Concept expansion failed: ${result.error.message}", err = true)
            }
        }
    }

    private suspend fun runQueryGeneration() {
        echo("   Loading concept expansion...")
        val concepts = loadArtifact<ConceptExpansion>("ConceptExpansion.json")
            ?: run {
                echo("❌ Could not load concepts. Run 'lumen run -p $projectId -s concept' first", err = true)
                return
            }

        echo("   Executing query generation...")
        val stage: QueryGenerationStage = get()

        when (val result = stage.execute(concepts)) {
            is StageResult.Success -> {
                echo("✅ Query generation completed")
                displayQueryPlan(result.data)
            }
            is StageResult.RequiresApproval -> {
                echo("⚠️  Query plan requires approval")
                displayQueryPlan(result.data)
            }
            is StageResult.Failure -> {
                echo("❌ Query generation failed: ${result.error.message}", err = true)
            }
        }
    }

    private suspend fun runTestAndRefine() {
        echo("   Loading query plan...")
        val queryPlan = loadArtifact<DatabaseQueryPlan>("DatabaseQueryPlan.json")
            ?: run {
                echo("❌ Could not load query plan. Run 'lumen run -p $projectId -s query' first", err = true)
                return
            }

        echo("   Executing test search...")
        val stage: TestAndRefineStage = get()

        when (val result = stage.execute(queryPlan)) {
            is StageResult.Success -> {
                echo("✅ Test search completed - query is appropriate")
                displayTestResults(result.data)
            }
            is StageResult.RequiresApproval -> {
                echo("⚠️  Test search requires refinement")
                displayTestResults(result.data)
                echo("\nRefinement suggestions:")
                result.suggestions.forEach { echo("   • $it") }
            }
            is StageResult.Failure -> {
                echo("❌ Test search failed: ${result.error.message}", err = true)
            }
        }
    }

    private suspend fun runSearchExecution() {
        echo("   Loading test results...")
        val testResults = loadArtifact<TestSearchResult>("TestSearchResult.json")
            ?: run {
                echo("❌ Could not load test results. Run 'lumen run -p $projectId -s test' first", err = true)
                return
            }

        if (!testResults.approved) {
            echo("⚠️  Warning: Test results not approved. Proceeding anyway...")
        }

        echo("   Executing full search (this may take a while)...")
        val stage: SearchExecutionStage = get()

        when (val result = stage.execute(testResults)) {
            is StageResult.Success -> {
                echo("✅ Search execution completed")
                echo("   Total documents: ${result.data.totalDocuments}")
                result.data.results.forEach { (db, dbResult) ->
                    echo("   - $db: ${dbResult.totalCount} documents")
                }
            }
            is StageResult.RequiresApproval -> {
                echo("⚠️  Search execution requires approval")
            }
            is StageResult.Failure -> {
                echo("❌ Search execution failed: ${result.error.message}", err = true)
            }
        }
    }

    private suspend fun runDeduplication() {
        echo("   Loading search results...")
        val searchResults = loadArtifact<SearchResults>("SearchResults.json")
            ?: run {
                echo("❌ Could not load search results. Run 'lumen run -p $projectId -s search' first", err = true)
                return
            }

        echo("   Executing deduplication...")
        val stage: DeduplicationStage = get()

        when (val result = stage.execute(searchResults)) {
            is StageResult.Success -> {
                echo("✅ Deduplication completed")
                echo("   Total documents: ${result.data.totalDocuments}")
                echo("   Unique documents: ${result.data.uniqueDocuments}")
                echo("   Duplicate groups: ${result.data.duplicateGroups}")
                echo("   Reduction: ${((1 - result.data.uniqueDocuments.toDouble() / result.data.totalDocuments) * 100).toInt()}%")
            }
            is StageResult.RequiresApproval -> {
                echo("⚠️  Deduplication requires approval")
            }
            is StageResult.Failure -> {
                echo("❌ Deduplication failed: ${result.error.message}", err = true)
            }
        }
    }

    // Display helpers

    private fun displayPico(pico: ProblemFraming) {
        echo("\nPICO Components:")
        echo("   Population: ${pico.population}")
        echo("   Intervention: ${pico.intervention}")
        echo("   Comparison: ${pico.comparison ?: "N/A"}")
        echo("   Outcome: ${pico.outcome}")
        if (pico.studyDesigns.isNotEmpty()) {
            echo("   Study Designs: ${pico.studyDesigns.joinToString(", ")}")
        }
    }

    private fun displayConceptExpansion(concepts: ConceptExpansion) {
        echo("\nExpanded Concepts:")
        echo("   Population: ${concepts.populationBlock.coreTerm}")
        echo("      Synonyms: ${concepts.populationBlock.synonyms.take(5).joinToString(", ")}${if (concepts.populationBlock.synonyms.size > 5) "..." else ""}")
        echo("   Intervention: ${concepts.interventionBlock.coreTerm}")
        echo("      Synonyms: ${concepts.interventionBlock.synonyms.take(5).joinToString(", ")}${if (concepts.interventionBlock.synonyms.size > 5) "..." else ""}")
        echo("   Outcome: ${concepts.outcomeBlock.coreTerm}")
        echo("      Synonyms: ${concepts.outcomeBlock.synonyms.take(5).joinToString(", ")}${if (concepts.outcomeBlock.synonyms.size > 5) "..." else ""}")
    }

    private fun displayQueryPlan(plan: DatabaseQueryPlan) {
        echo("\nGenerated Queries:")
        plan.queries.forEach { (db, query) ->
            echo("   Database: $db")
            echo("   Query: ${query.queryText.take(100)}${if (query.queryText.length > 100) "..." else ""}")
            echo("   Validation: ${query.validationStatus}")
        }
    }

    private fun displayTestResults(results: TestSearchResult) {
        echo("\nTest Search Results:")
        echo("   Broadness: ${results.currentAnalysis.broadness}")
        echo("   Total documents: ${results.currentAnalysis.totalAcrossDatabases}")
        echo("   Recommendations:")
        results.currentAnalysis.recommendations.forEach { echo("      • $it") }
    }

    // Artifact loading

    private inline fun <reified T> loadArtifact(filename: String): T? {
        val file = File("data/$projectId/artifacts/$filename")
        if (!file.exists()) {
            return null
        }

        return try {
            json.decodeFromString<T>(file.readText())
        } catch (e: Exception) {
            echo("⚠️  Warning: Could not parse $filename: ${e.message}", err = true)
            null
        }
    }
}

