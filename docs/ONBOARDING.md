# Lumen Developer Onboarding

Welcome to **Lumen** – an AI‑assisted, PRISMA‑compliant systematic review platform built with Kotlin Multiplatform.

This guide helps you get from zero to your first contribution as quickly as possible.

---

## 1. Prerequisites Checklist

Make sure you have:

- [ ] **Git**
  - Windows: install via Git for Windows
  - Linux/macOS: use your package manager
- [ ] **JDK 17+**
  - Recommended: Temurin / Azul Zulu
- [ ] **IntelliJ IDEA**
  - Community Edition is enough, Ultimate is nicer
- [ ] **Gradle** (optional)
  - The repo uses the Gradle wrapper (`./gradlew`), no global install required
- [ ] **GitHub account**
  - To fork the repo and open pull requests

Optional (for advanced work):

- [ ] Docker (for future server / PostgreSQL work)
- [ ] R (for meta‑analysis integration later)

---

## 2. Clone and Run the Project

1. **Fork the repo** on GitHub:

   - Go to: `https://github.com/nexus-scholar/Lumen`
   - Click "Fork" to create your own copy under your account

2. **Clone your fork**:

   ```bash
   git clone https://github.com/<your-username>/Lumen.git
   cd Lumen
   ```

3. **Open in IntelliJ IDEA**:

   - `File → Open...` → select the `Lumen` folder
   - Let IntelliJ import the Gradle project

4. **Run tests (sanity check)**:

   ```bash
   ./gradlew test
   ```

5. **Run the desktop app (once it exists)**:

   ```bash
   ./gradlew :desktopApp:run
   ```

   or in IntelliJ: run the `desktopApp` run configuration.

If tests pass and the app runs, your environment is good.

---

## 3. Repository Structure (High‑Level)

This is the intended structure as the project grows:

```text
Lumen/
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── docs/
│   ├── 01-OVERVIEW.md
│   ├── 02-ARCHITECTURE.md
│   ├── 03-PIPELINE-STAGES.md
│   ├── 04-DATABASE-DESIGN.md
│   ├── 05-REVENUE-MODEL.md
│   ├── 06-ROADMAP.md
│   ├── 07-BRAND-IDENTITY.md
│   ├── ONBOARDING.md
│   ├── USER-GUIDE.md
│   └── milestones/
│       ├── MVP-CHECKLIST.md
│       └── PHASE-2-CHECKLIST.md
├── shared/                  # KMP shared module (core engine)
├── desktopApp/              # Compose Desktop UI
├── cli/                     # CLI tool
└── webApp/                  # Web frontend (Kotlin/JS)
```

You can find detailed architecture and pipeline descriptions in:

- `docs/02-ARCHITECTURE.md`
- `docs/03-PIPELINE-STAGES.md`
- `docs/04-DATABASE-DESIGN.md`

---

## 4. How to Pick a First Task

Good first contributions:

- Add/adjust **models** in `shared/` (e.g., `Document`, `ProjectContext`)
- Implement a **single pipeline stage** skeleton
- Implement **tests** for an existing utility (e.g., text normalization, Levenshtein)
- Improve **docs** (typos, clarifications, examples)

Workflow:

1. Check the issue tracker (once populated) for labels like:
   - `good first issue`
   - `help wanted`
2. Comment on the issue:  
   "I'd like to work on this – @maintainer is this OK?"
3. Wait for confirmation, then start coding.

---

## 5. Development Workflow

1. **Create a branch**:

   ```bash
   git checkout -b feature/<short-description>
   # e.g., feature/add-openalex-provider
   ```

2. **Write code + tests together**:
   - Keep changes focused and small.
   - Ensure types and nullability are handled cleanly.

3. **Run tests locally**:

   ```bash
   ./gradlew test
   ```

4. **Commit with clear messages**:

   ```bash
   git add .
   git commit -m "Add OpenAlex provider basic implementation"
   ```

5. **Push and open a PR**:

   ```bash
   git push origin feature/add-openalex-provider
   ```

   Then open a Pull Request on GitHub:
   - Base: `main` (or `develop` if we introduce that)
   - Title: short but descriptive
   - Description: what you changed, how you tested it

---

## 6. Coding Standards (Short Version)

- **Language:** Kotlin (idiomatic, KMP‑friendly code)
- **Style:**
  - Prefer small, composable functions
  - Avoid global state
  - Use `data class` for models
  - Use `sealed class` for results/errors where appropriate
- **Null Safety:**
  - Prefer non‑nullable fields; use nullable only where the data truly may be absent
- **Error Handling:**
  - Avoid swallowing exceptions
  - Use `Result`‑like patterns or sealed types for pipeline stages

More details: see `CONTRIBUTING.md`.

---

## 7. Asking for Help

If you get stuck:

- Open a **GitHub Discussion** or issue
- Or open a draft PR with a note: "Not ready – need feedback on X"

The goal is to make Lumen welcoming for contributors while keeping the codebase clean and maintainable.

Welcome aboard. 🚀
