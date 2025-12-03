package com.lumen.core.domain.pipeline

/**
 * Base interface for all pipeline stages
 * Each stage takes input of type I and produces output of type O
 */
interface PipelineStage<I, O> {
    /**
     * Execute this pipeline stage
     * @param input The input data for this stage
     * @return StageResult wrapping the output or error
     */
    suspend fun execute(input: I): StageResult<O>

    /**
     * Human-readable name for this stage
     */
    val stageName: String
}

/**
 * Result wrapper for pipeline stage execution
 */
sealed class StageResult<out T> {
    /**
     * Stage completed successfully
     */
    data class Success<T>(val data: T) : StageResult<T>()

    /**
     * Stage failed with an error
     */
    data class Failure(val error: PipelineError) : StageResult<Nothing>()

    /**
     * Stage completed but requires human approval before proceeding
     */
    data class RequiresApproval<T>(
        val data: T,
        val reason: String,
        val suggestions: List<String> = emptyList()
    ) : StageResult<T>()
}

/**
 * Pipeline error types
 */
sealed class PipelineError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class ValidationFailed(
        override val message: String,
        val errors: List<String> = emptyList()
    ) : PipelineError(message)

    data class LlmCallFailed(
        override val message: String,
        override val cause: Throwable? = null
    ) : PipelineError(message, cause)

    data class ApiCallFailed(
        override val message: String,
        val provider: String,
        override val cause: Throwable? = null
    ) : PipelineError(message, cause)

    data class PreconditionFailed(
        override val message: String
    ) : PipelineError(message)

    data class StorageFailed(
        override val message: String,
        override val cause: Throwable? = null
    ) : PipelineError(message, cause)

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : PipelineError(message, cause)
}

