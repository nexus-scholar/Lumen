# 🎨 Desktop UI Guide - Running Stages

**You asked: "How can I run stages in the desktop UI?"**

**Answer**: I just added it! Here's how to use it:

---

## 🚀 Running the Desktop App

### Step 1: Launch the Application

```powershell
# From your terminal in the project folder
.\gradlew run

# Or in IntelliJ IDEA
# Click the green play button ▶️ next to 'fun main()' in:
# src/jvmMain/kotlin/com/lumen/desktop/Main.kt
```

### Step 2: Create or Open a Project

**Creating a New Project:**
1. Click "+ New Project" button
2. Enter your research question
3. (Optional) Enter a project name
4. Select review type (Intervention, Diagnostic, etc.)
5. Click "Create Project"

**Opening an Existing Project:**
1. Click on any project card in the list
2. Project detail screen opens

---

## 🎯 Running Pipeline Stages

### The New Project Detail Screen

When you open a project, you'll see:

```
┌─────────────────────────────────────────────────┐
│ ← Back    Project Name         [View Results]  │
├─────────────────────────────────────────────────┤
│                                                 │
│  📋 Project Information                         │
│  Type: Intervention                             │
│  Status: CREATED                                │
│  Created: 2025-12-03                           │
│                                                 │
│  🔍 Research Question                           │
│  What is the effectiveness of...?              │
│                                                 │
│  Pipeline Stages                                │
│                                                 │
│  ┌──────────────────────────────┐              │
│  │ ▶ 1. PICO Extraction  [Run] │              │
│  │   Extract Population, Inter...│              │
│  └──────────────────────────────┘              │
│                                                 │
│  ┌──────────────────────────────┐              │
│  │   3. Concept Expansion [Run] │  (disabled) │
│  │   Generate synonyms...        │              │
│  └──────────────────────────────┘              │
│                                                 │
│  ... more stages ...                            │
└─────────────────────────────────────────────────┘
```

### How to Run a Stage

1. **Click the "Run" button** on any enabled stage
2. Watch the button change to a spinner ⏳
3. Wait for the stage to complete
4. See the result message:
   - ✅ "PICO extracted successfully"
   - ⚠️ "PICO extracted, requires approval"
   - ❌ "Failed: error message"

### Stage Order (Must Run in Sequence)

```
1. PICO Extraction         ← Always available
   ↓
3. Concept Expansion       ← Enabled after PICO
   ↓
4. Query Generation        ← Enabled after Concept
   ↓
4.5. Test & Refine        ← Enabled after Query
   ↓
7. Search Execution       ← Enabled after Test
   ↓
9. Deduplication          ← Enabled after Search
```

Each stage becomes available only after the previous one completes!

---

## 🎬 Complete Workflow Example

### Scenario: Create a New Systematic Review

**1. Launch App**
```powershell
.\gradlew run
```

**2. Create Project**
- Click "+ New Project"
- Enter: "What is the effectiveness of machine learning for crop disease detection?"
- Select: Intervention
- Click "Create Project"

**3. Run Pipeline Stages** (one by one)

**Stage 1: PICO Extraction**
- Click "Run" button on "1. PICO Extraction"
- Wait ~5-10 seconds
- See: ✅ "PICO extracted successfully"
- The card turns green with a checkmark ✓

**Stage 2: Concept Expansion**
- "3. Concept Expansion" button is now enabled
- Click "Run"
- Wait ~10-15 seconds
- See: ✅ "Concepts expanded"

**Stage 3: Query Generation**
- Click "Run" on "4. Query Generation"
- Wait ~10 seconds
- See: ✅ "Queries generated"

**Stage 4: Test & Refine**
- Click "Run" on "4.5. Test & Refine"
- Wait ~15-20 seconds (it searches a small sample)
- See: ✅ "Test completed" or ⚠️ "Queries may need refinement"

**Stage 5: Search Execution**
- Click "Run" on "7. Search Execution"
- Wait ~30-60 seconds (searches full databases!)
- See: ✅ "Search completed: 247 documents"

**Stage 6: Deduplication**
- Click "Run" on "9. Deduplication"
- Wait ~10 seconds
- See: ✅ "Deduplicated: 198 unique from 247"

**4. View Results**
- Click "View Results" button (top right)
- Browse all documents
- Search by title/author
- Click document to see details
- Export to CSV or JSONL

---

## 🐛 Debugging the Desktop App (for Beginners)

### Adding Print Statements

Want to see what's happening? Add `println()`:

```kotlin
@Composable
fun ProjectDetailScreen(...) {
    LaunchedEffect(projectId) {
        println("Loading project: $projectId")  // ← Add this!
        project = loadProject(projectId)
        println("Loaded: ${project?.name}")     // ← And this!
    }
}
```

**Where to see output:**
- In IntelliJ: "Run" tab at bottom
- In terminal: Where you ran `.\gradlew run`

### Using Breakpoints

**1. Add a Breakpoint**
```kotlin
private suspend fun runStage(...) {
    return when (stageType) {
        StageType.PICO -> {
            val stage = StageKoinHelper.getPicoStage()
            // ← Click in gray margin here (red dot appears)
            when (val result = stage.execute(project)) {
                // Debug stops here, inspect variables!
```

**2. Run in Debug Mode**
- In IntelliJ: Click the bug icon 🐞 next to `fun main()`
- Click "Run" button in UI
- App pauses at your breakpoint
- Inspect `stage`, `project`, `result` variables

**3. Debug Controls**
- F8: Step to next line
- F9: Continue running
- Hover over variables to see values

### Common Issues

**"Button doesn't work"**
```kotlin
Button(onClick = {
    println("Button clicked!")  // ← Add this first
    scope.launch {
        println("Launching coroutine")  // ← Then this
        runStage(...)
    }
}) {
    Text("Run")
}
```

**"Stage fails immediately"**
```kotlin
try {
    val result = runStage(projectId, stageType)
    println("Result: $result")  // ← What happened?
} catch (e: Exception) {
    println("Error: ${e.message}")  // ← See the error
    e.printStackTrace()  // ← Full details
}
```

---

## 📁 File Locations (For Learning)

```
Desktop UI Code:
src/jvmMain/kotlin/com/lumen/desktop/
├── Main.kt                      ← App entry point
└── ui/
    ├── ProjectListScreen.kt     ← Project list
    ├── ProjectDetailScreen.kt   ← Stage controls (NEW!)
    ├── ResultsBrowserScreen.kt  ← Document browser
    └── CreateProjectDialog.kt   ← New project form

Pipeline Stages:
src/commonMain/kotlin/com/lumen/core/domain/stages/
├── ProjectSetupStage.kt
├── PicoExtractionStage.kt
├── ConceptExpansionStage.kt
├── QueryGenerationStage.kt
├── TestAndRefineStage.kt
├── SearchExecutionStage.kt
└── DeduplicationStage.kt
```

### Understanding the Flow

```
User clicks "Run" button
    ↓
onClick handler fires
    ↓
scope.launch { } (runs async)
    ↓
runStage(projectId, StageType.PICO)
    ↓
StageKoinHelper.getPicoStage() (get from Koin)
    ↓
stage.execute(project) (do the work)
    ↓
Result displayed in UI
```

---

## 🎓 Learning Tips

### Start Small

**1. Just Run It**
- Don't try to understand everything
- Run the app, click buttons, see what happens
- It's OK not to know how it works yet!

**2. Add println() Everywhere**
```kotlin
println("1. Starting function")
val result = doSomething()
println("2. Got result: $result")
return result
println("3. Never see this (after return)")
```

**3. Break Things**
- Change text, see what updates
- Comment out code, see what breaks
- You can always undo (Ctrl+Z)!

### Reading Kotlin Code

**Variables:**
```kotlin
var count = 0        // Mutable (can change)
val name = "test"    // Immutable (can't change)
```

**Functions:**
```kotlin
fun add(a: Int, b: Int): Int {
    return a + b
}

// Same but shorter
fun add(a: Int, b: Int) = a + b
```

**Null Safety:**
```kotlin
var name: String = "test"   // Never null
var name: String? = null    // Can be null

println(name?.length)        // Safe: returns null if name is null
println(name!!.length)       // Unsafe: crashes if name is null
```

**When (like if/else or switch):**
```kotlin
when (status) {
    Status.READY -> println("Ready!")
    Status.RUNNING -> println("Running...")
    else -> println("Other")
}
```

---

## 🚀 Next Steps

**1. Run Your First Project**
- Create a project
- Run all stages (takes ~2-3 minutes total)
- View results
- Export to CSV

**2. Look at the Code**
- Open ProjectDetailScreen.kt
- Find the "Run" button's onClick handler
- Add println() to see what happens

**3. Try Making Changes**
- Change button text: "Run" → "Execute"
- Change stage description
- Add emoji to result messages: ✅🎉

**4. Read the Full Debugging Guide**
- See `DEBUGGING_GUIDE.md` in project root
- Covers IntelliJ debugging, breakpoints, etc.

---

## 📞 Quick Reference

**Run Desktop App:**
```powershell
.\gradlew run
```

**Add Debug Output:**
```kotlin
println("Debug: $variableName")
```

**Where Stages Run:**
`ProjectDetailScreen.kt` → Line ~200+ in `runStage()` function

**View Output:**
IntelliJ "Run" tab or terminal where you ran `.\gradlew run`

---

**You now have a fully functional Desktop UI with stage controls!** 

Just run `.\gradlew run` and start clicking buttons. Don't worry about understanding everything - learning by doing is the best way! 🎉

**Welcome to Kotlin development!** 🚀

