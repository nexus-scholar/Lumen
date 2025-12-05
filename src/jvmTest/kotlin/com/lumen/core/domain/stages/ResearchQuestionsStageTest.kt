package com.lumen.core.domain.stages

import com.lumen.core.domain.model.*
import com.lumen.core.domain.pipeline.ArtifactStore
import com.lumen.core.domain.pipeline.StageResult
import com.lumen.core.services.llm.LlmService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class ResearchQuestionsStageTest {

    private lateinit var llmService: LlmService
    private lateinit var artifactStore: ArtifactStore
    private lateinit var stage: ResearchQuestionsStage

    private val testPico = ProblemFraming(
        population = "Patients with type 2 diabetes",
        intervention = "Metformin",
        comparison = "Placebo",
        outcome = "Blood glucose levels",
        studyDesigns = listOf("RCT", "Cohort"),
        approved = true,
        extractedAt = Clock.System.now()
    )

    @BeforeEach
    fun setup() {
        llmService = mockk()
        artifactStore = mockk(relaxed = true)
        stage = ResearchQuestionsStage(llmService, artifactStore)
    }

    @Test
    fun `rejects unapproved PICO`() = runTest {
        val unapprovedPico = testPico.copy(approved = false)

        val result = stage.execute(unapprovedPico)

        assertTrue(result is StageResult.Failure)
        val failure = result as StageResult.Failure
        assertTrue(failure.error.message.contains("approved"))
    }

    @Test
    fun `handles LLM unavailable`() = runTest {
        coEvery { llmService.isAvailable() } returns false

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.RequiresApproval)
        val approval = result as StageResult.RequiresApproval<*>
        assertTrue(approval.reason.contains("unavailable"))
    }

    @Test
    fun `generates valid research questions`() = runTest {
        coEvery { llmService.isAvailable() } returns true
        every { llmService.modelName } returns "gpt-4"

        val mockResponse = QuestionsResponse(
            primary = QuestionDto(
                text = "What is the effect of metformin on blood glucose levels in patients with type 2 diabetes?",
                rationale = "Primary outcome of interest"
            ),
            secondary = listOf(
                QuestionDto(
                    text = "How does the effect vary by patient age?",
                    rationale = "Explores age as moderator"
                ),
                QuestionDto(
                    text = "What is the effect on HbA1c levels?",
                    rationale = "Secondary outcome measure"
                )
            )
        )

        coEvery {
            llmService.generateStructured<QuestionsResponse>(
                any(), any(), any()
            )
        } returns mockResponse

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.RequiresApproval)
        val approval = result as StageResult.RequiresApproval<ResearchQuestions>

        assertEquals("primary_1", approval.data.primary.id)
        assertEquals(QuestionType.PRIMARY, approval.data.primary.type)
        assertEquals(2, approval.data.secondary.size)
        assertFalse(approval.data.approved)

        // Verify artifact saved
        coVerify {
            artifactStore.save(
                projectId = any(),
                artifact = any<ResearchQuestions>(),
                serializer = any<KSerializer<ResearchQuestions>>(),
                filename = "ResearchQuestions.json"
            )
        }
    }

    @Test
    fun `handles LLM generation failure`() = runTest {
        coEvery { llmService.isAvailable() } returns true

        coEvery {
            llmService.generateStructured<QuestionsResponse>(any(), any(), any())
        } throws Exception("API error")

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.Failure)

        // Verify error saved
        coVerify {
            artifactStore.saveError(
                projectId = any(),
                stageName = any(),
                error = any(),
                context = any()
            )
        }
    }

    @Test
    fun `validates generated questions`() = runTest {
        coEvery { llmService.isAvailable() } returns true
        every { llmService.modelName } returns "gpt-4"

        // Generate questions with validation issues
        val mockResponse = QuestionsResponse(
            primary = QuestionDto(
                text = "Short?", // Too short
                rationale = "Test"
            ),
            secondary = emptyList()
        )

        coEvery {
            llmService.generateStructured<QuestionsResponse>(any(), any(), any())
        } returns mockResponse

        val result = stage.execute(testPico)

        assertTrue(result is StageResult.Failure)
        val failure = result as StageResult.Failure
        assertTrue(failure.error.message.contains("validation"))
    }
}

