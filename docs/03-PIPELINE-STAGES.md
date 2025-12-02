# Lumen: Pipeline Stages

## 15 Pipeline Stages (PRISMA 2020-Compliant)

1. **Project Setup**  
   Create workspace, add metadata, select review type  
2. **Problem Framing (PICO Extraction)**  
   LLM-aided, with human-in-the-loop edit/approval  
3. **Research Questions Generation**  
   Automatic from PICO, editable  
4. **Concept Expansion (Synonyms/MeSH)**  
   LLM and data-driven  
5. **Database Query Plan (Anti-Hallucination)**  
   Automated Boolean query generation, syntax validation  
6. **Test & Refine Protocol**  
   Test searches, LLM-powered refinement loop
7. **Screening Criteria Definition**  
   Deterministic, generated from PICO/questions
8. **Protocol Registration**  
   Generate/protocol for PROSPERO/OSF (future: API submit)
9. **Query Execution (Search)**  
   Automated, API-backed, database normalization
10. **Citation Expansion (Snowballing)**  
   Forward/backward chaining to expand corpus
11. **Deduplication & Initial Screening**  
   Automated and manual, with conservative clustering
12. **Title/Abstract Screening**  
   Dual-reviewer with conflict workflow, keyboard shortcuts
13. **Full-Text Screening**  
   PDF management, eligibility checklist
14. **Data Extraction**  
   Guided templates, LLM-extraction for future
15. **Risk of Bias Assessment**   
   RoB 2.0, QUADAS-2, ROBINS-I, Newcastle-Ottawa
16. **Synthesis/Analytics**  
   LLM summaries, network graphs, meta-analysis (R integration)
17. **Export & PRISMA Reporting**  
   Flowcharts, protocols, logs, journal-ready tables

See [database and analytics docs](04-DATABASE-DESIGN.md) for detailed models.
