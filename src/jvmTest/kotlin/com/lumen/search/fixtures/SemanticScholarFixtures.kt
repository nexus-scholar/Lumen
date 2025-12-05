package com.lumen.search.fixtures

/**
 * JSON fixtures for Semantic Scholar API responses.
 * Based on real API response structure from https://api.semanticscholar.org/
 */
object SemanticScholarFixtures {

    val PAPER_COMPLETE = """
    {
        "paperId": "abc123def456",
        "corpusId": 12345678,
        "externalIds": {
            "DOI": "10.1038/s41586-019-1666-5",
            "ArXiv": null,
            "PubMed": "31234567"
        },
        "url": "https://www.semanticscholar.org/paper/abc123def456",
        "title": "The effect of metformin on type 2 diabetes",
        "abstract": "Background: Metformin is the first-line treatment for type 2 diabetes mellitus. This systematic review examines its efficacy and safety profile.",
        "venue": "Nature",
        "publicationVenue": {
            "id": "nature-id",
            "name": "Nature",
            "type": "journal"
        },
        "year": 2019,
        "referenceCount": 45,
        "citationCount": 148,
        "influentialCitationCount": 25,
        "isOpenAccess": true,
        "openAccessPdf": {
            "url": "https://www.nature.com/articles/s41586-019-1666-5.pdf",
            "status": "GOLD"
        },
        "fieldsOfStudy": ["Medicine", "Biology"],
        "s2FieldsOfStudy": [
            {"category": "Medicine", "source": "s2-fos-model"},
            {"category": "Biology", "source": "s2-fos-model"}
        ],
        "authors": [
            {
                "authorId": "12345",
                "name": "Jane Doe"
            },
            {
                "authorId": "67890",
                "name": "John Smith"
            }
        ],
        "tldr": {
            "model": "tldr@v2.0.0",
            "text": "This study shows metformin effectively reduces blood glucose levels in T2D patients."
        }
    }
    """.trimIndent()

    val PAPER_MINIMAL = """
    {
        "paperId": "xyz789",
        "corpusId": 99999999,
        "title": "Untitled Paper",
        "abstract": null,
        "venue": null,
        "year": null,
        "citationCount": 0,
        "authors": [],
        "tldr": null
    }
    """.trimIndent()

    val SEARCH_RESPONSE_SUCCESS = """
    {
        "total": 850,
        "offset": 0,
        "next": 10,
        "data": [
            {
                "paperId": "abc123def456",
                "corpusId": 12345678,
                "externalIds": {"DOI": "10.1038/s41586-019-1666-5"},
                "title": "The effect of metformin on type 2 diabetes",
                "abstract": "Background: Metformin is the first-line treatment...",
                "venue": "Nature",
                "year": 2019,
                "citationCount": 148,
                "isOpenAccess": true,
                "authors": [{"authorId": "12345", "name": "Jane Doe"}],
                "tldr": {"text": "This study shows metformin effectively reduces blood glucose levels."}
            },
            {
                "paperId": "def456ghi789",
                "corpusId": 87654321,
                "externalIds": {"DOI": "10.1016/j.cell.2020.01.001"},
                "title": "Glucose homeostasis in metabolic disorders",
                "abstract": "We examine the molecular mechanisms...",
                "venue": "Cell",
                "year": 2020,
                "citationCount": 92,
                "isOpenAccess": false,
                "authors": [{"authorId": "54321", "name": "Bob Wilson"}],
                "tldr": null
            }
        ]
    }
    """.trimIndent()

    val SEARCH_RESPONSE_EMPTY = """
    {
        "total": 0,
        "offset": 0,
        "data": []
    }
    """.trimIndent()

    val BATCH_DETAILS_RESPONSE = """
    [
        {
            "paperId": "abc123def456",
            "title": "The effect of metformin on type 2 diabetes",
            "abstract": "Full abstract text here...",
            "year": 2019,
            "citationCount": 150,
            "referenceCount": 45,
            "references": [
                {"paperId": "ref1", "title": "Reference paper 1"},
                {"paperId": "ref2", "title": "Reference paper 2"}
            ],
            "citations": [
                {"paperId": "cite1", "title": "Citing paper 1"}
            ]
        }
    ]
    """.trimIndent()

    val ERROR_RESPONSE_429 = """
    {
        "error": "Too Many Requests",
        "message": "Rate limit exceeded. Please slow down your requests."
    }
    """.trimIndent()

    val ERROR_RESPONSE_400 = """
    {
        "error": "Bad Request",
        "message": "Invalid query parameter"
    }
    """.trimIndent()
}

