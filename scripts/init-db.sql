-- Initialize Lumen database schema

-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Papers table
CREATE TABLE IF NOT EXISTS papers (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    abstract TEXT,
    year INTEGER,
    doi VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- Paper authors table
CREATE TABLE IF NOT EXISTS paper_authors (
    id SERIAL PRIMARY KEY,
    paper_id VARCHAR(255) NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    author_name VARCHAR(500) NOT NULL,
    author_order INTEGER NOT NULL,
    CONSTRAINT fk_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
);

-- Paper sources table
CREATE TABLE IF NOT EXISTS paper_sources (
    id SERIAL PRIMARY KEY,
    paper_id VARCHAR(255) NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(255),
    CONSTRAINT fk_paper_source FOREIGN KEY (paper_id) REFERENCES papers(id)
);

-- Screening decisions table
CREATE TABLE IF NOT EXISTS screening_decisions (
    id SERIAL PRIMARY KEY,
    paper_id VARCHAR(255) NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    project_id VARCHAR(255) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    stage VARCHAR(50) NOT NULL,
    decision VARCHAR(50) NOT NULL,
    reason TEXT,
    screener_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_paper_screening FOREIGN KEY (paper_id) REFERENCES papers(id),
    CONSTRAINT fk_project_screening FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_papers_project_id ON papers(project_id);
CREATE INDEX IF NOT EXISTS idx_papers_doi ON papers(doi);
CREATE INDEX IF NOT EXISTS idx_paper_authors_paper_id ON paper_authors(paper_id);
CREATE INDEX IF NOT EXISTS idx_paper_sources_paper_id ON paper_sources(paper_id);
CREATE INDEX IF NOT EXISTS idx_screening_paper_id ON screening_decisions(paper_id);
CREATE INDEX IF NOT EXISTS idx_screening_project_id ON screening_decisions(project_id);

-- Insert sample data (optional)
INSERT INTO projects (id, name, description, status, created_at, updated_at)
VALUES
    ('sample-project-1', 'Sample Systematic Review', 'A sample project for testing', 'CREATED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

