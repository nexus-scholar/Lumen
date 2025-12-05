package com.lumen.search.fixtures

/**
 * XML fixtures for ArXiv API responses (Atom feed format).
 * Based on real API response structure from https://arxiv.org/help/api/
 */
object ArxivFixtures {

    val ENTRY_COMPLETE = """
    <?xml version="1.0" encoding="UTF-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <entry>
        <id>http://arxiv.org/abs/2401.12345v1</id>
        <updated>2024-01-25T00:00:00Z</updated>
        <published>2024-01-20T12:00:00Z</published>
        <title>Deep Learning for Medical Image Analysis: A Comprehensive Survey</title>
        <summary>We present a comprehensive survey of deep learning techniques for medical image analysis. Our review covers convolutional neural networks, transformers, and their applications in diagnosis, segmentation, and treatment planning.</summary>
        <author>
          <name>Alice Johnson</name>
          <arxiv:affiliation xmlns:arxiv="http://arxiv.org/schemas/atom">Stanford University</arxiv:affiliation>
        </author>
        <author>
          <name>Bob Williams</name>
          <arxiv:affiliation xmlns:arxiv="http://arxiv.org/schemas/atom">MIT</arxiv:affiliation>
        </author>
        <arxiv:comment xmlns:arxiv="http://arxiv.org/schemas/atom">25 pages, 10 figures, accepted to IEEE TMI</arxiv:comment>
        <link href="http://arxiv.org/abs/2401.12345v1" rel="alternate" type="text/html"/>
        <link title="pdf" href="http://arxiv.org/pdf/2401.12345v1" rel="related" type="application/pdf"/>
        <arxiv:primary_category xmlns:arxiv="http://arxiv.org/schemas/atom" term="cs.CV" scheme="http://arxiv.org/schemas/atom"/>
        <category term="cs.CV" scheme="http://arxiv.org/schemas/atom"/>
        <category term="cs.LG" scheme="http://arxiv.org/schemas/atom"/>
        <category term="eess.IV" scheme="http://arxiv.org/schemas/atom"/>
      </entry>
    </feed>
    """.trimIndent()

    val ENTRY_MINIMAL = """
    <?xml version="1.0" encoding="UTF-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <entry>
        <id>http://arxiv.org/abs/2401.99999v1</id>
        <updated>2024-01-01T00:00:00Z</updated>
        <published>2024-01-01T00:00:00Z</published>
        <title>Minimal ArXiv Entry</title>
        <summary></summary>
        <author>
          <name>Anonymous</name>
        </author>
        <link href="http://arxiv.org/abs/2401.99999v1" rel="alternate" type="text/html"/>
        <arxiv:primary_category xmlns:arxiv="http://arxiv.org/schemas/atom" term="cs.AI" scheme="http://arxiv.org/schemas/atom"/>
      </entry>
    </feed>
    """.trimIndent()

    val SEARCH_RESPONSE_SUCCESS = """
    <?xml version="1.0" encoding="UTF-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom"
          xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/"
          xmlns:arxiv="http://arxiv.org/schemas/atom">
      <link href="http://arxiv.org/api/query?search_query=machine+learning" rel="self" type="application/atom+xml"/>
      <title type="html">ArXiv Query: search_query=machine learning</title>
      <id>http://arxiv.org/api/query</id>
      <updated>2024-01-25T00:00:00Z</updated>
      <opensearch:totalResults>125000</opensearch:totalResults>
      <opensearch:startIndex>0</opensearch:startIndex>
      <opensearch:itemsPerPage>10</opensearch:itemsPerPage>
      <entry>
        <id>http://arxiv.org/abs/2401.12345v1</id>
        <updated>2024-01-25T00:00:00Z</updated>
        <published>2024-01-20T12:00:00Z</published>
        <title>Deep Learning for Medical Image Analysis</title>
        <summary>We present a comprehensive survey of deep learning techniques...</summary>
        <author><name>Alice Johnson</name></author>
        <author><name>Bob Williams</name></author>
        <link href="http://arxiv.org/abs/2401.12345v1" rel="alternate" type="text/html"/>
        <link title="pdf" href="http://arxiv.org/pdf/2401.12345v1" rel="related" type="application/pdf"/>
        <arxiv:primary_category term="cs.CV" scheme="http://arxiv.org/schemas/atom"/>
        <category term="cs.CV" scheme="http://arxiv.org/schemas/atom"/>
        <category term="cs.LG" scheme="http://arxiv.org/schemas/atom"/>
      </entry>
      <entry>
        <id>http://arxiv.org/abs/2401.11111v2</id>
        <updated>2024-01-24T00:00:00Z</updated>
        <published>2024-01-15T10:00:00Z</published>
        <title>Neural Network Optimization Techniques</title>
        <summary>This paper introduces novel optimization methods for training neural networks...</summary>
        <author><name>Carol Zhang</name></author>
        <link href="http://arxiv.org/abs/2401.11111v2" rel="alternate" type="text/html"/>
        <link title="pdf" href="http://arxiv.org/pdf/2401.11111v2" rel="related" type="application/pdf"/>
        <arxiv:primary_category term="cs.LG" scheme="http://arxiv.org/schemas/atom"/>
        <category term="cs.LG" scheme="http://arxiv.org/schemas/atom"/>
      </entry>
    </feed>
    """.trimIndent()

    val SEARCH_RESPONSE_EMPTY = """
    <?xml version="1.0" encoding="UTF-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom"
          xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
      <title type="html">ArXiv Query: search_query=nonexistent</title>
      <id>http://arxiv.org/api/query</id>
      <updated>2024-01-25T00:00:00Z</updated>
      <opensearch:totalResults>0</opensearch:totalResults>
      <opensearch:startIndex>0</opensearch:startIndex>
      <opensearch:itemsPerPage>10</opensearch:itemsPerPage>
    </feed>
    """.trimIndent()

    val ERROR_RESPONSE = """
    <?xml version="1.0" encoding="UTF-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>Error</title>
      <id>http://arxiv.org/api/query</id>
      <updated>2024-01-25T00:00:00Z</updated>
      <entry>
        <id>http://arxiv.org/api/errors#incorrect_id_format</id>
        <title>Error</title>
        <summary>incorrect id format for 12345</summary>
      </entry>
    </feed>
    """.trimIndent()

    val RATE_LIMIT_RESPONSE = """
    <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
    <html><head>
    <title>503 Service Unavailable</title>
    </head><body>
    <h1>Service Unavailable</h1>
    <p>The server is temporarily unable to service your request due to maintenance downtime or capacity problems. Please try again later.</p>
    </body></html>
    """.trimIndent()
}

