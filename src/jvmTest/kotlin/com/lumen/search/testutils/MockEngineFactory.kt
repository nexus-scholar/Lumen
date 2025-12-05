package com.lumen.search.testutils

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating mock HTTP clients for testing.
 * Each provider gets its own specialized factory method.
 */
object MockEngineFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Creates a mock HTTP client for OpenAlex API testing.
     */
    fun createForOpenAlex(
        searchResponse: String,
        searchStatusCode: HttpStatusCode = HttpStatusCode.OK,
        detailsResponse: String? = null,
        detailsStatusCode: HttpStatusCode = HttpStatusCode.OK,
        statsResponse: String? = null,
        statsStatusCode: HttpStatusCode = HttpStatusCode.OK,
        responseHeaders: Headers = headersOf()
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.host == "api.openalex.org" -> {
                            val path = request.url.encodedPath
                            when {
                                // Stats request (has group_by parameter)
                                path.startsWith("/works") && request.url.parameters["group_by"] != null -> {
                                    respond(
                                        content = statsResponse ?: searchResponse,
                                        status = statsStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                // Detail request (specific work ID)
                                path.matches(Regex("/works/W\\d+")) -> {
                                    respond(
                                        content = detailsResponse ?: searchResponse,
                                        status = detailsStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                // Search request
                                path.startsWith("/works") -> {
                                    respond(
                                        content = searchResponse,
                                        status = searchStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                else -> error("Unhandled OpenAlex request: ${request.url}")
                            }
                        }
                        else -> error("Unhandled request: ${request.url}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    /**
     * Creates a mock HTTP client for Semantic Scholar API testing.
     */
    fun createForSemanticScholar(
        searchResponse: String,
        searchStatusCode: HttpStatusCode = HttpStatusCode.OK,
        detailsResponse: String? = null,
        detailsStatusCode: HttpStatusCode = HttpStatusCode.OK,
        responseHeaders: Headers = headersOf()
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.host == "api.semanticscholar.org" -> {
                            val path = request.url.encodedPath
                            when {
                                path.contains("/paper/search") -> {
                                    respond(
                                        content = searchResponse,
                                        status = searchStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                path.contains("/paper/batch") -> {
                                    respond(
                                        content = detailsResponse ?: "[]",
                                        status = detailsStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                path.matches(Regex(".*/paper/[a-zA-Z0-9]+")) -> {
                                    respond(
                                        content = detailsResponse ?: searchResponse,
                                        status = detailsStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                else -> error("Unhandled Semantic Scholar request: ${request.url}")
                            }
                        }
                        else -> error("Unhandled request: ${request.url}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    /**
     * Creates a mock HTTP client for Crossref API testing.
     */
    fun createForCrossref(
        searchResponse: String,
        searchStatusCode: HttpStatusCode = HttpStatusCode.OK,
        detailsResponse: String? = null,
        detailsStatusCode: HttpStatusCode = HttpStatusCode.OK,
        responseHeaders: Headers = headersOf()
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.host == "api.crossref.org" -> {
                            val path = request.url.encodedPath
                            when {
                                // Detail request (specific DOI)
                                path.matches(Regex("/works/10\\..+")) -> {
                                    respond(
                                        content = detailsResponse ?: searchResponse,
                                        status = detailsStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                // Search request
                                path.startsWith("/works") -> {
                                    respond(
                                        content = searchResponse,
                                        status = searchStatusCode,
                                        headers = responseHeaders + contentTypeJson()
                                    )
                                }
                                else -> error("Unhandled Crossref request: ${request.url}")
                            }
                        }
                        else -> error("Unhandled request: ${request.url}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    /**
     * Creates a mock HTTP client for ArXiv API testing.
     */
    fun createForArxiv(
        response: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        responseHeaders: Headers = headersOf()
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.host == "export.arxiv.org" -> {
                            respond(
                                content = response,
                                status = statusCode,
                                headers = responseHeaders + contentTypeXml()
                            )
                        }
                        else -> error("Unhandled request: ${request.url}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    /**
     * Creates a multi-provider mock client for orchestrator testing.
     */
    fun createMultiProvider(
        openAlexResponse: String? = null,
        semanticScholarResponse: String? = null,
        crossrefResponse: String? = null,
        arxivResponse: String? = null
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.host) {
                        "api.openalex.org" -> {
                            if (openAlexResponse != null) {
                                respond(openAlexResponse, HttpStatusCode.OK, contentTypeJson())
                            } else {
                                respond("", HttpStatusCode.ServiceUnavailable)
                            }
                        }
                        "api.semanticscholar.org" -> {
                            if (semanticScholarResponse != null) {
                                respond(semanticScholarResponse, HttpStatusCode.OK, contentTypeJson())
                            } else {
                                respond("", HttpStatusCode.ServiceUnavailable)
                            }
                        }
                        "api.crossref.org" -> {
                            if (crossrefResponse != null) {
                                respond(crossrefResponse, HttpStatusCode.OK, contentTypeJson())
                            } else {
                                respond("", HttpStatusCode.ServiceUnavailable)
                            }
                        }
                        "export.arxiv.org" -> {
                            if (arxivResponse != null) {
                                respond(arxivResponse, HttpStatusCode.OK, contentTypeXml())
                            } else {
                                respond("", HttpStatusCode.ServiceUnavailable)
                            }
                        }
                        else -> error("Unhandled request: ${request.url}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    /**
     * Creates a mock client that always fails with the specified status code.
     */
    fun createFailing(
        statusCode: HttpStatusCode,
        errorBody: String = "Error",
        retryAfter: Long? = null
    ): HttpClient {
        val headers = if (retryAfter != null) {
            headersOf("Retry-After" to listOf(retryAfter.toString()))
        } else {
            headersOf()
        }

        return HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    respond(
                        content = errorBody,
                        status = statusCode,
                        headers = headers + contentTypeJson()
                    )
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    private fun contentTypeJson(): Headers =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun contentTypeXml(): Headers =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Xml.toString())

    private operator fun Headers.plus(other: Headers): Headers {
        return Headers.build {
            appendAll(this@plus)
            appendAll(other)
        }
    }
}

