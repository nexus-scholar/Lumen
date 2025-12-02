# Lumen

> **Systematic Reviews in Hours, Not Months**

Lumen is an AI-powered, cross-platform systematic review platform that automates the complete PRISMA 2020 workflow—from research question to publication-ready protocol. Built by researchers who know the pain of spending 200+ hours on manual literature reviews.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![PRISMA 2020](https://img.shields.io/badge/PRISMA-2020%20Compliant-success)](http://prisma-statement.org/)

## 🌟 Why Lumen?

### The Problem
- Manual systematic reviews take **6-12 months**
- Query hallucinations waste hours debugging
- Expensive tools ($300+/year) lock you into web interfaces
- No version control for protocol changes
- Limited collaboration features

### The Solution
Lumen automates **15 PRISMA stages** while maintaining full transparency and reproducibility:

✅ **Anti-Hallucination Queries** - Validate database syntax before execution  
✅ **Test & Refine Protocol** - Iteratively optimize queries with sample results  
✅ **Multi-Database Search** - OpenAlex, Crossref, arXiv, PubMed, Scopus  
✅ **Citation Expansion** - Automatic forward/backward snowballing  
✅ **Dual-Reviewer Screening** - Built-in conflict resolution  
✅ **LLM Analytics** - Narrative synthesis, contradiction detection  
✅ **Graph Visualization** - Citation networks and research clusters  
✅ **Git-Like Version Control** - Track every protocol change  
✅ **Cross-Platform** - Desktop, CLI, Web (offline-first)  
✅ **PRISMA Export** - Auto-generated flowcharts and protocols  

---

## 🚀 Quick Start

### Desktop App (Compose Multiplatform)
```bash
# Coming soon - Q2 2026
```

### CLI Tool
```bash
# Coming soon - Q2 2026
kotlin lumen-cli.jar start --research-question="AI for crop disease detection"
```

### Web App
```bash
# Coming soon - Q3 2026
# Visit lumen.io
```

---

## 📋 Features by Stage

### Identification Phase
- **Stage 0:** Project Setup with metadata capture
- **Stage 1:** PICO Extraction (LLM-powered)
- **Stage 2:** Research Questions Generation
- **Stage 3:** Concept Expansion (MeSH, synonyms)
- **Stage 4:** Database Query Plan (anti-hallucination)
- **Stage 4.5:** Test & Refine Protocol ⭐ NEW
- **Stage 5:** Screening Criteria (deterministic)
- **Stage 6:** Protocol Registration (PROSPERO/OSF)

### Search & Retrieval
- **Stage 7:** Multi-Database Search Execution
- **Stage 8:** Citation Expansion (snowballing)
- **Stage 9:** Deduplication & Initial Screening

### Screening Phase
- **Stage 10:** Title/Abstract Screening
- **Stage 11:** Full-Text Screening with PDF viewer

### Data Extraction & Assessment
- **Stage 12:** Structured Data Extraction
- **Stage 13:** Risk of Bias (RoB 2.0, QUADAS-2, etc.)

### Synthesis & Reporting
- **Stage 14:** Synthesis & Analytics ⭐
  - LLM narrative synthesis
  - Citation network graphs
  - Contradiction detection
  - Meta-analysis integration
- **Stage 15:** Export & PRISMA Reporting
  - PRISMA flowchart (SVG/PNG)
  - Protocol documents (Markdown/PDF)
  - Data exports (CSV, BibTeX, RIS, JSON)

---

## 🏗️ Architecture

### Technology Stack
- **Language:** Kotlin Multiplatform (KMP)
- **Platforms:** JVM (Desktop/CLI), JS (Web), future: Native (Mobile)
- **Desktop UI:** Compose Multiplatform
- **Database:** Hybrid SQLite + JSON + PostgreSQL
- **APIs:** OpenAlex, Crossref, Semantic Scholar, arXiv
- **LLM:** OpenAI, Anthropic (structured outputs)
- **Graph Analysis:** JGraphT
- **Version Control:** Git (JGit library)

### Data Model
```
Project Structure:
data/project_<id>/
  ├── artifacts/              # JSON files (Git-tracked)
  │   ├── ProjectContext.json
  │   ├── ProblemFraming.json
  │   └── ... (15 stage outputs)
  ├── project.db              # SQLite (screening, extraction)
  └── export/                 # Generated outputs
```

**Why Hybrid?**
- **JSON artifacts:** Human-readable, Git-compatible, portable
- **SQLite:** Fast queries for 892+ papers, ACID transactions
- **PostgreSQL:** Optional cloud sync for teams (collaborative editing)

See [docs/02-ARCHITECTURE.md](docs/02-ARCHITECTURE.md) for details.

---

## 💰 Pricing

| Tier | Price | Features |
|------|-------|----------|
| **Free** | $0 | 1 project, 200 papers, 2 databases |
| **Pro** | $99/year | Unlimited projects/papers, all 7 databases, citation expansion |
| **Pro Plus** | $149/year | Pro + LLM analytics + graph viz + meta-analysis |
| **Team** | $399/year | 5 users, dual-reviewer screening, collaboration |
| **Institutional** | Custom | Unlimited users, self-hosted, SSO, dedicated support |

**Goal:** $50K revenue in 24 months (300 Pro users + 30 Teams + 2 Institutions)

See [docs/05-REVENUE-MODEL.md](docs/05-REVENUE-MODEL.md) for breakdown.

---

## 🗓️ Roadmap

### Phase 1: Foundation (Months 1-6, Dec 2025 - May 2026)
- ✅ Project setup and architecture
- 🔄 KMP migration (simple_slr + strategy-pipeline)
- 🔄 Stages 0-7 (search pipeline)
- 🔄 Desktop app MVP
- 🔄 CLI tool
- 🎯 **Goal:** 500 free users via ProductHunt launch

### Phase 2: PRISMA Compliance (Months 7-12)
- Stage 4.5 (Test & Refine)
- Stage 8 (Citation Expansion)
- PRISMA flowchart generation
- 🎯 **Goal:** $15K ARR (100 Pro users)

### Phase 3: Advanced Features (Months 13-18)
- Stages 10-13 (Screening + Extraction + RoB)
- LLM analytics
- Graph visualization
- 🎯 **Goal:** $35K ARR

### Phase 4: Scale (Months 19-24)
- Stage 15 (Export everything)
- Team collaboration
- University pilots
- 🎯 **Goal:** $50K ARR ✅

See [docs/06-ROADMAP.md](docs/06-ROADMAP.md) for detailed timeline.

---

## 🤝 Contributing

We welcome contributions! This is an open-source project built by researchers, for researchers.

### How to Contribute
1. Read [CONTRIBUTING.md](CONTRIBUTING.md)
2. Check [Issues](https://github.com/nexus-scholar/Lumen/issues) for open tasks
3. Fork the repo and create a feature branch
4. Submit a PR with clear description

### Development Setup
```bash
# Clone repository
git clone https://github.com/nexus-scholar/Lumen.git
cd Lumen

# Install dependencies (KMP)
./gradlew build

# Run tests
./gradlew test

# Run desktop app
./gradlew :desktop:run
```

### Areas We Need Help
- 🔬 **Domain Experts:** PRISMA reviewers, research librarians
- 💻 **Developers:** Kotlin, Compose Multiplatform, graph algorithms
- 🎨 **Designers:** UI/UX for screening interfaces
- 📊 **Data Scientists:** Meta-analysis, citation network analysis
- 📝 **Technical Writers:** Documentation, tutorials

---

## 📚 Documentation

Detailed documentation is in the [docs/](docs/) folder:

1. [**Overview**](docs/01-OVERVIEW.md) - Vision, goals, competitive analysis
2. [**Architecture**](docs/02-ARCHITECTURE.md) - Technical design decisions
3. [**Pipeline Stages**](docs/03-PIPELINE-STAGES.md) - All 15 stages explained
4. [**Database Design**](docs/04-DATABASE-DESIGN.md) - Hybrid SQLite+JSON+PostgreSQL
5. [**Revenue Model**](docs/05-REVENUE-MODEL.md) - $50K in 24 months strategy
6. [**Roadmap**](docs/06-ROADMAP.md) - Development timeline
7. [**Brand Identity**](docs/07-BRAND-IDENTITY.md) - Naming exploration (Siftera, Evidentia, etc.)

---

## 🎓 Academic Use

### Citation
If you use Lumen in your research, please cite:

```bibtex
@software{lumen2025,
  title = {Lumen: AI-Powered Systematic Review Platform},
  author = {Bekhouche, Mouadh},
  year = {2025},
  url = {https://github.com/nexus-scholar/Lumen},
  note = {PRISMA 2020 compliant}
}
```

### Publications
We're planning to publish:
1. **Methods Paper:** "Lumen: An Open-Source Platform for Automated Systematic Reviews" (target: *BMC Medical Research Methodology*)
2. **Validation Study:** Compare Lumen-generated reviews vs. manual reviews (PRISMA adherence, time savings)

---

## 📧 Contact

- **Author:** Mouadh Bekhouche (PhD Researcher, AI in Agriculture)
- **Email:** bekhouche.mouadh@gmail.com
- **GitHub:** [@mbsoft31](https://github.com/mbsoft31)
- **Organization:** [Nexus Scholar](https://github.com/nexus-scholar)

### Community
- **Discussions:** [GitHub Discussions](https://github.com/nexus-scholar/Lumen/discussions)
- **Issues:** [Report bugs](https://github.com/nexus-scholar/Lumen/issues)
- **Twitter:** Coming soon
- **Discord:** Coming soon (for beta testers)

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file.

This project is open-source to advance academic research. Commercial use permitted with attribution.

---

## 🙏 Acknowledgments

- **PRISMA Group** for establishing systematic review standards
- **OpenAlex** for open scholarly data
- **Kotlin Multiplatform** for cross-platform development
- **All researchers** who spent months on manual reviews and inspired this project

---

## ⭐ Star History

If you find Lumen useful, please star the repository to help others discover it!

---

**Built with 💡 by researchers who believe systematic reviews should take hours, not months.**