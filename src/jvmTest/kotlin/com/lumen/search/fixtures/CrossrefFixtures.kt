package com.lumen.search.fixtures

/**
 * JSON fixtures for Crossref API responses.
 * Based on real API response structure from https://api.crossref.org/
 */
object CrossrefFixtures {

    val WORK_COMPLETE = """
    {
        "status": "ok",
        "message-type": "work",
        "message-version": "1.0.0",
        "message": {
            "DOI": "10.1038/s41586-019-1666-5",
            "type": "journal-article",
            "title": ["The Effect of Metformin on Type 2 Diabetes: A Systematic Review"],
            "container-title": ["Nature"],
            "published-print": {
                "date-parts": [[2019, 10, 15]]
            },
            "published-online": {
                "date-parts": [[2019, 10, 10]]
            },
            "author": [
                {
                    "given": "Jane",
                    "family": "Doe",
                    "sequence": "first",
                    "affiliation": [{"name": "Harvard University"}],
                    "ORCID": "http://orcid.org/0000-0001-2345-6789"
                },
                {
                    "given": "John",
                    "family": "Smith",
                    "sequence": "additional",
                    "affiliation": [{"name": "MIT"}]
                }
            ],
            "is-referenced-by-count": 150,
            "references-count": 45,
            "abstract": "<jats:p>Background: Metformin is the first-line treatment for type 2 diabetes.</jats:p>",
            "link": [
                {
                    "URL": "https://www.nature.com/articles/s41586-019-1666-5.pdf",
                    "content-type": "application/pdf",
                    "intended-application": "text-mining"
                }
            ],
            "license": [
                {
                    "URL": "https://creativecommons.org/licenses/by/4.0/",
                    "content-version": "vor",
                    "delay-in-days": 0
                }
            ],
            "subject": ["General Medicine", "Endocrinology"],
            "ISSN": ["0028-0836", "1476-4687"],
            "publisher": "Springer Nature"
        }
    }
    """.trimIndent()

    val WORK_MINIMAL = """
    {
        "status": "ok",
        "message-type": "work",
        "message": {
            "DOI": "10.9999/minimal",
            "type": "other",
            "title": ["Minimal Record"]
        }
    }
    """.trimIndent()

    val SEARCH_RESPONSE_SUCCESS = """
    {
        "status": "ok",
        "message-type": "work-list",
        "message-version": "1.0.0",
        "message": {
            "total-results": 5200,
            "items-per-page": 25,
            "query": {
                "search-terms": "metformin diabetes"
            },
            "items": [
                {
                    "DOI": "10.1038/s41586-019-1666-5",
                    "type": "journal-article",
                    "title": ["The Effect of Metformin on Type 2 Diabetes"],
                    "container-title": ["Nature"],
                    "published-print": {"date-parts": [[2019, 10, 15]]},
                    "author": [
                        {"given": "Jane", "family": "Doe", "ORCID": "http://orcid.org/0000-0001-2345-6789"}
                    ],
                    "is-referenced-by-count": 150,
                    "link": [{"URL": "https://example.com/paper.pdf", "content-type": "application/pdf"}]
                },
                {
                    "DOI": "10.1016/j.cell.2020.01.001",
                    "type": "journal-article",
                    "title": ["Glucose Metabolism Studies"],
                    "container-title": ["Cell"],
                    "published-print": {"date-parts": [[2020, 1, 15]]},
                    "author": [],
                    "is-referenced-by-count": 75
                }
            ]
        }
    }
    """.trimIndent()

    val SEARCH_RESPONSE_EMPTY = """
    {
        "status": "ok",
        "message-type": "work-list",
        "message": {
            "total-results": 0,
            "items-per-page": 25,
            "items": []
        }
    }
    """.trimIndent()

    val ERROR_RESPONSE_404 = """
    {
        "status": "failed",
        "message-type": "error",
        "message": "Resource not found"
    }
    """.trimIndent()

    val ERROR_RESPONSE_503 = """
    {
        "status": "failed",
        "message-type": "error",
        "message": "Service temporarily unavailable"
    }
    """.trimIndent()
}

