# 🐛 Debugging Guide for Lumen (Kotlin Beginner-Friendly)

**Welcome to Kotlin!** This guide will help you debug both the CLI and Desktop applications.

---

## 🎯 Quick Start: Running & Debugging

### Desktop Application (Visual UI)

**Run Normally:**
```powershell
# From terminal
.\gradlew run

# Or in IntelliJ IDEA
# Click the green play button ▶️ next to 'fun main()' in src/jvmMain/kotlin/com/lumen/desktop/Main.kt
```

**Debug (Step Through Code):**
```powershell
# In IntelliJ IDEA:
1. Open src/jvmMain/kotlin/com/lumen/desktop/Main.kt
2. Click in the left margin next to a line to add a breakpoint (red dot will appear)
3. Click the green bug icon 🐞 next to 'fun main()' (or right-click → Debug)
4. App will pause at your breakpoint
5. Use F8 to step over, F7 to step into functions
```

### CLI Application

**Run Normally:**
```powershell
# Build the JAR first
.\gradlew cliJar

# Then run commands
java -jar build\libs\lumen-cli.jar list
java -jar build\libs\lumen-cli.jar new -q "Test question"
```

**Debug (Step Through Code):**
```powershell
# In IntelliJ IDEA:
1. Open src/jvmMain/kotlin/com/lumen/cli/Main.kt
2. Click Run → Edit Configurations
3. Click + → Kotlin (not Application)
4. Set:
   - Main class: com.lumen.cli.MainKt
   - Program arguments: list (or whatever command)
   - Use classpath of module: Lumen.jvmMain
5. Add breakpoints by clicking in left margin
6. Click debug button 🐞
```

---

## 🔍 IntelliJ IDEA Debugging Basics

### Setting Breakpoints

**What**: Pauses execution at a specific line so you can inspect variables

**How**:
```
1. Click in the gray area left of line numbers
2. A red dot 🔴 appears = breakpoint set
3. Click again to remove it
```

**Example**:
```kotlin
fun createProject(question: String) {
    val project = Project(...)  // ← Click here to add breakpoint
    println("Created: ${project.id}")
}
```

### Debug Controls (When Paused)

| Button | Key | What It Does |
|--------|-----|--------------|
| ▶️ Resume | F9 | Continue until next breakpoint |
| ⏭️ Step Over | F8 | Execute current line, move to next |
| ⬇️ Step Into | F7 | Go inside function being called |
| ⬆️ Step Out | Shift+F8 | Exit current function |
| 🔄 Evaluate | Alt+F8 | Test any expression |

### Inspecting Variables

When paused at a breakpoint:

**Variables Panel** (bottom left):
- Shows all variables in current scope
- Expand objects to see properties
- Hover over variables in code to see values

**Watches** (bottom right):
- Right-click variable → Add to Watches
- Stays visible as you step through code

**Evaluate Expression** (Alt+F8):
- Type any Kotlin code to test
- Example: `project.status`, `documents.size`, etc.

---

## 🖥️ Desktop App Debugging

### Finding Your Way Around

**Main Entry Point**:
```kotlin
// src/jvmMain/kotlin/com/lumen/desktop/Main.kt
fun main() {  // ← App starts here
    startKoin { ... }  // Set up dependency injection
    application {
        Window(...) {
            MaterialTheme {
                LumenApp()  // ← Main UI
            }
        }
    }
}
```

**UI Screens**:
```
src/jvmMain/kotlin/com/lumen/desktop/ui/
├── ProjectListScreen.kt      ← Shows all projects
├── CreateProjectDialog.kt    ← New project form
└── ResultsBrowserScreen.kt   ← Shows documents/results
```

### Common Debugging Scenarios

**1. "Why doesn't my button work?"**

Add breakpoint in the onClick handler:
```kotlin
Button(onClick = {
    println("Button clicked!")  // ← Add this for quick check
    // Add breakpoint on next line
    val result = doSomething()
}) {
    Text("Click Me")
}
```

**2. "Why isn't data loading?"**

Check the LaunchedEffect:
```kotlin
LaunchedEffect(projectId) {  // ← Runs when projectId changes
    println("Loading data for: $projectId")  // Add this
    // Add breakpoint here
    val data = loadData(projectId)
}
```

**3. "Why is Koin injection failing?"**

Check your module configuration:
```kotlin
// src/commonMain/kotlin/com/lumen/core/di/CoreModule.kt
val coreModule = module {
    single { DatabaseManager() }  // ← Is this defined?
    single { PicoExtractionStage(get()) }  // ← Check dependencies
}
```

### Compose-Specific Debugging

**Recomposition Issues**:
```kotlin
@Composable
fun MyScreen() {
    var count by remember { mutableStateOf(0) }
    
    // Add this to see when recomposition happens
    println("MyScreen recomposed! Count: $count")
    
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

**State Not Updating?**

Use `remember` for state that should trigger recomposition:
```kotlin
// ❌ Wrong - changes won't trigger UI update
var count = 0

// ✅ Correct - UI updates when changed
var count by remember { mutableStateOf(0) }
```

---

## 📋 CLI App Debugging

### Running CLI Commands in Debug Mode

**Method 1: IntelliJ Run Configuration**

```
1. Run → Edit Configurations
2. Click + → Kotlin
3. Name: "CLI List Command"
4. Main class: com.lumen.cli.MainKt
5. Program arguments: list
6. Working directory: C:\Users\mouadh\Desktop\ai-llm-research\Lumen
7. Use classpath of module: Lumen.jvmMain
8. Click OK
9. Click Debug 🐞
```

**Method 2: Attach to Running JAR** (Advanced)

```powershell
# Run with debug port open
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -jar build\libs\lumen-cli.jar list

# In IntelliJ: Run → Attach to Process → Select the java process
```

### Command Flow

```kotlin
// src/jvmMain/kotlin/com/lumen/cli/Main.kt

fun main(args: Array<String>) {  // ← Entry point
    startKoin { ... }
    
    LumenCli()
        .subcommands(
            NewProjectCommand(),     // ← "lumen new"
            ListProjectsCommand(),   // ← "lumen list"
            RunStageCommand(),       // ← "lumen run"
            ExportCommand()          // ← "lumen export"
        )
        .main(args)  // ← Parses args, calls correct command
}
```

**Example: Debugging "lumen list"**

```kotlin
// src/jvmMain/kotlin/com/lumen/cli/commands/ListProjectsCommand.kt

class ListProjectsCommand : CliktCommand(...) {
    override fun run() {  // ← Add breakpoint here
        val dataDir = File("data")
        
        // Add breakpoint here to see what files exist
        val projects = dataDir.listFiles()
        
        // Step through to see each project loaded
        projects?.forEach { projectDir ->
            val project = loadProject(projectDir)
            echo("Project: ${project.name}")  // ← See output
        }
    }
}
```

---

## 🔧 Common Issues & Solutions

### 1. "Unresolved reference" Errors

**Problem**: IntelliJ can't find classes/functions

**Solutions**:
```
1. File → Invalidate Caches → Invalidate and Restart
2. Right-click project → Gradle → Reload Gradle Project
3. Build → Rebuild Project
4. Check you're in the right source set (jvmMain vs commonMain)
```

### 2. "Could not find Koin instance"

**Problem**: Dependency injection not initialized

**Debug**:
```kotlin
fun main() {
    startKoin {
        modules(coreModule, jvmModule)
    }
    
    // Try to get a dependency
    val stage: PicoExtractionStage = get()  // ← Add breakpoint
    println("Got stage: $stage")  // Should work
}
```

**Check module is loaded**:
```kotlin
// Make sure your module is in the list
startKoin {
    modules(
        coreModule,    // ← Is this imported?
        jvmModule      // ← Is this imported?
    )
}
```

### 3. Gradle Build Fails

**Read the error carefully!**

Common patterns:
```
"Unresolved reference: X"
→ Import is missing or dependency not added

"Kotlin version mismatch"
→ Clean: .\gradlew clean build

"Task 'X' not found"
→ Check build.gradle.kts for task definition
```

### 4. App Crashes on Launch

**Check logs**:
```kotlin
// Add try-catch to see errors
fun main() {
    try {
        startKoin { ... }
        application { ... }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()  // ← Full error details
    }
}
```

---

## 💡 Kotlin Basics for Debugging

### Reading Function Signatures

```kotlin
fun createProject(
    name: String,              // Required parameter
    type: ReviewType = INTERVENTION  // Optional, has default
): Project {                   // Returns Project object
    // ...
}

// Call with all params
createProject("My Project", ReviewType.DIAGNOSTIC)

// Call with defaults
createProject("My Project")
```

### Understanding Null Safety

```kotlin
var name: String = "test"     // Cannot be null
var name: String? = null      // Can be null

// Safe access
println(name?.length)         // Returns null if name is null
println(name!!.length)        // Crashes if name is null (avoid!)
println(name ?: "default")    // Use "default" if name is null
```

### Coroutines (Async Code)

```kotlin
// Run async code
suspend fun loadData() {  // ← 'suspend' means can pause/resume
    val data = apiCall()  // Might take time
    return data
}

// Must be called from coroutine scope
runBlocking {         // Blocks until complete
    val data = loadData()
}

scope.launch {        // Runs in background
    val data = loadData()
}
```

### When (Like Switch/Case)

```kotlin
when (status) {
    ProjectStatus.CREATED -> "Just started"
    ProjectStatus.COMPLETED -> "All done"
    else -> "In progress"
}
```

---

## 📊 Useful Debugging Snippets

### Print All Variables

```kotlin
fun debugFunction() {
    val project = loadProject()
    
    // Print everything
    println("""
        Project Debug:
        - ID: ${project.id}
        - Name: ${project.name}
        - Status: ${project.status}
        - Authors: ${project.authors.size}
    """.trimIndent())
}
```

### Log Compose Recompositions

```kotlin
@Composable
fun MyScreen(projectId: String) {
    // See when this runs
    DisposableEffect(projectId) {
        println("MyScreen created for: $projectId")
        onDispose {
            println("MyScreen disposed")
        }
    }
}
```

### Check Koin Dependencies

```kotlin
fun checkKoin() {
    val koin = GlobalContext.get()
    println("Registered modules: ${koin.getAll<Module>()}")
}
```

---

## 🎓 Learning Resources

### IntelliJ IDEA
- Help → Learn IntelliJ IDEA (interactive tutorial)
- View → Tool Windows → Debug (when debugging)

### Kotlin Basics
- https://kotlinlang.org/docs/basic-syntax.html
- https://play.kotlinlang.org (try code online)

### Compose Desktop
- https://github.com/JetBrains/compose-multiplatform

### Koin (Dependency Injection)
- https://insert-koin.io/docs/quickstart/kotlin

---

## 🚀 Quick Reference Card

```
BUILD
.\gradlew build              Build everything
.\gradlew clean build        Clean build
.\gradlew cliJar            Build CLI JAR

RUN
.\gradlew run               Desktop app
java -jar build\libs\lumen-cli.jar <cmd>   CLI

DEBUG SHORTCUTS
F8          Step over (next line)
F7          Step into (enter function)
Shift+F8    Step out (exit function)
F9          Resume (continue to next breakpoint)
Alt+F8      Evaluate expression

BREAKPOINTS
Click left margin          Add/remove breakpoint
Ctrl+F8                   Toggle breakpoint
Ctrl+Shift+F8            View all breakpoints
```

---

## 📞 Getting Help

1. **Read the error message** - Usually tells you exactly what's wrong
2. **Add println() statements** - Quick way to see what's happening
3. **Use breakpoints** - Stop and inspect when you're confused
4. **Check the docs** - Links above for Kotlin/Compose/Koin
5. **Look at similar code** - Other commands/screens as examples

**Remember**: Everyone was new to Kotlin once. Debugging is how you learn! 🎉

---

**Happy Debugging!** 🐛➡️🎯

