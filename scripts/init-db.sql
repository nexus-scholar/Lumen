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

-- ================================================================
-- Research Questions Tables
-- Stage 2: Research Questions Generation
-- ================================================================

-- Research questions master table
CREATE TABLE IF NOT EXISTS research_questions (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL, -- PRIMARY, SECONDARY, EXPLORATORY
    rationale TEXT,
    display_order INTEGER NOT NULL,
    approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_project_questions FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_question_type CHECK (question_type IN ('PRIMARY', 'SECONDARY', 'EXPLORATORY'))
);

-- PICO mapping for each research question
CREATE TABLE IF NOT EXISTS research_question_pico (
    question_id VARCHAR(255) PRIMARY KEY,
    population TEXT NOT NULL,
    intervention TEXT NOT NULL,
    comparison TEXT,
    outcome TEXT NOT NULL,
    CONSTRAINT fk_question_pico FOREIGN KEY (question_id) REFERENCES research_questions(id) ON DELETE CASCADE
);

-- Research questions metadata (generation details)
CREATE TABLE IF NOT EXISTS research_questions_metadata (
    project_id VARCHAR(255) PRIMARY KEY,
    llm_model VARCHAR(255),
    prompt TEXT,
    raw_output TEXT,
    generated_at TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    version INTEGER DEFAULT 1,
    CONSTRAINT fk_project_metadata FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Audit trail for question edits
CREATE TABLE IF NOT EXISTS research_question_history (
    id SERIAL PRIMARY KEY,
    question_id VARCHAR(255) NOT NULL,
    previous_text TEXT NOT NULL,
    new_text TEXT NOT NULL,
    edited_by VARCHAR(255),
    edited_at TIMESTAMP NOT NULL,
    edit_reason TEXT,
    CONSTRAINT fk_question_history FOREIGN KEY (question_id) REFERENCES research_questions(id) ON DELETE CASCADE
);

-- ================================================================
-- Indexes for Research Questions
-- ================================================================

CREATE INDEX IF NOT EXISTS idx_questions_project_id ON research_questions(project_id);
CREATE INDEX IF NOT EXISTS idx_questions_type ON research_questions(question_type);
CREATE INDEX IF NOT EXISTS idx_questions_approved ON research_questions(approved);
CREATE INDEX IF NOT EXISTS idx_questions_display_order ON research_questions(project_id, display_order);
CREATE INDEX IF NOT EXISTS idx_question_history_question_id ON research_question_history(question_id);
CREATE INDEX IF NOT EXISTS idx_question_history_edited_at ON research_question_history(edited_at);

-- ================================================================
-- Sample Data (Optional - for testing)
-- ================================================================

-- Insert sample data (optional)
INSERT INTO projects (id, name, description, status, created_at, updated_at)
VALUES
    ('sample-project-1', 'Sample Systematic Review', 'A sample project for testing', 'CREATED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

