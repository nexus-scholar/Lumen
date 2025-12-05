package com.lumen.search.fixtures

/**
 * JSON fixtures for OpenAlex API responses.
 * Based on real API response structure from https://docs.openalex.org/
 */
object OpenAlexFixtures {

    val WORK_COMPLETE = """
    {
        "id": "https://openalex.org/W2741809807",
        "doi": "https://doi.org/10.1038/s41586-019-1666-5",
        "title": "The effect of metformin on type 2 diabetes: A systematic review",
        "display_name": "The effect of metformin on type 2 diabetes: A systematic review",
        "publication_year": 2019,
        "publication_date": "2019-10-15",
        "type": "journal-article",
        "cited_by_count": 150,
        "is_oa": true,
        "authorships": [
            {
                "author_position": "first",
                "author": {
                    "id": "https://openalex.org/A5001234567",
                    "display_name": "Jane Doe",
                    "orcid": "https://orcid.org/0000-0001-2345-6789"
                },
                "institutions": [
                    {
                        "id": "https://openalex.org/I136199984",
                        "display_name": "Harvard University"
                    }
                ]
            },
            {
                "author_position": "last",
                "author": {
                    "id": "https://openalex.org/A5009876543",
                    "display_name": "John Smith",
                    "orcid": null
                },
                "institutions": [
                    {
                        "id": "https://openalex.org/I63966007",
                        "display_name": "MIT"
                    }
                ]
            }
        ],
        "primary_location": {
            "source": {
                "id": "https://openalex.org/S137773608",
                "display_name": "Nature"
            },
            "pdf_url": "https://www.nature.com/articles/s41586-019-1666-5.pdf",
            "is_oa": true
        },
        "abstract_inverted_index": {
            "Background": [0],
            ":": [1],
            "Metformin": [2],
            "is": [3],
            "the": [4],
            "first-line": [5],
            "treatment": [6],
            "for": [7],
            "type": [8],
            "2": [9],
            "diabetes": [10],
            "mellitus": [11]
        },
        "concepts": [
            {
                "id": "https://openalex.org/C71924100",
                "display_name": "Diabetes mellitus",
                "level": 2,
                "score": 0.95
            },
            {
                "id": "https://openalex.org/C502942594",
                "display_name": "Metformin",
                "level": 3,
                "score": 0.92
            }
        ],
        "referenced_works": [
            "https://openalex.org/W1234567890",
            "https://openalex.org/W9876543210"
        ],
        "cited_by_api_url": "https://api.openalex.org/works?filter=cites:W2741809807"
    }
    """.trimIndent()

    val WORK_MINIMAL = """
    {
        "id": "https://openalex.org/W9999999999",
        "doi": null,
        "title": "Untitled Work",
        "display_name": "Untitled Work",
        "publication_year": null,
        "type": "unknown",
        "cited_by_count": 0,
        "is_oa": false,
        "authorships": [],
        "primary_location": null,
        "abstract_inverted_index": null,
        "concepts": [],
        "referenced_works": []
    }
    """.trimIndent()

    val SEARCH_RESPONSE_SUCCESS = """
    {
        "meta": {
            "count": 1250,
            "db_response_time_ms": 45,
            "page": 1,
            "per_page": 25
        },
        "results": [
            {
                "id": "https://openalex.org/W2741809807",
                "doi": "https://doi.org/10.1038/s41586-019-1666-5",
                "title": "The effect of metformin on type 2 diabetes",
                "publication_year": 2019,
                "type": "journal-article",
                "cited_by_count": 150,
                "is_oa": true,
                "authorships": [
                    {
                        "author": {
                            "id": "https://openalex.org/A5001234567",
                            "display_name": "Jane Doe",
                            "orcid": "https://orcid.org/0000-0001-2345-6789"
                        },
                        "institutions": [{"display_name": "Harvard University"}]
                    }
                ],
                "primary_location": {
                    "source": {"display_name": "Nature"},
                    "pdf_url": "https://www.nature.com/articles/s41586-019-1666-5.pdf"
                },
                "abstract_inverted_index": {"Background": [0], "Metformin": [1]},
                "concepts": [{"id": "C71924100", "display_name": "Diabetes", "score": 0.95}]
            },
            {
                "id": "https://openalex.org/W1234567890",
                "doi": "https://doi.org/10.1016/j.cell.2020.01.001",
                "title": "Glucose metabolism in diabetes",
                "publication_year": 2020,
                "type": "journal-article",
                "cited_by_count": 75,
                "is_oa": false,
                "authorships": [],
                "primary_location": null,
                "abstract_inverted_index": null,
                "concepts": []
            }
        ]
    }
    """.trimIndent()

    val SEARCH_RESPONSE_EMPTY = """
    {
        "meta": {
            "count": 0,
            "db_response_time_ms": 12,
            "page": 1,
            "per_page": 25
        },
        "results": []
    }
    """.trimIndent()

    val STATS_RESPONSE = """
    {
        "meta": {
            "count": 5000,
            "db_response_time_ms": 30,
            "page": 1,
            "per_page": 1
        },
        "results": [],
        "group_by": [
            {"key": "2024", "key_display_name": "2024", "count": 1200},
            {"key": "2023", "key_display_name": "2023", "count": 1500},
            {"key": "2022", "key_display_name": "2022", "count": 1100},
            {"key": "2021", "key_display_name": "2021", "count": 800},
            {"key": "2020", "key_display_name": "2020", "count": 400}
        ]
    }
    """.trimIndent()

    val ERROR_RESPONSE_429 = """
    {
        "error": "Rate limit exceeded",
        "message": "Too many requests. Please wait before retrying."
    }
    """.trimIndent()

    val ERROR_RESPONSE_500 = """
    {
        "error": "Internal server error",
        "message": "An unexpected error occurred."
    }
    """.trimIndent()
}

