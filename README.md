# Know Your Candidate

A Kotlin-based CLI tool that helps voters learn about candidates in the **2026 Tamil Nadu Assembly Elections**. Enter a constituency name and get a structured profile of every candidate standing there — pulled from local party data and enriched with live web search results.

---

## How It Works

The app runs an interactive REPL. For each query, candidate data is loaded directly from local JSON files, then web searches run in parallel for all matched candidates:

```
User Input (constituency name)
        │
        ▼
┌─────────────────────────┐
│  Tools.getCandidateList │  ← Reads all party JSON files from candidates-info/
│  Returns: JsonObject    │  ← One entry per party with matching candidate
│  {partyFile: candidate} │
└────────────┬────────────┘
             │  (parallel coroutine per candidate)
             ▼
┌─────────────────────────┐
│   getWebSearchAgent     │  ← Koog AIAgent + Ollama (gemma4:26b)
│   MCP: tavily-mcp       │  ← Spawns `npx tavily-mcp@0.1.3` over stdio
│   Returns: summary of   │
│   publicly available    │
│   candidate background  │
└────────────┬────────────┘
             │
             ▼
     Printed to console
```

### Candidate Lookup — `Tools.getCandidateList`

- Scans all `.json` files in the `candidates-info/` resource directory at runtime.
- Searches each file concurrently using coroutines.
- Returns a `JsonObject` mapping each party filename to its matching candidate.

### Web Search Agent — `getWebSearchAgent`

- Backed by **Ollama** running **gemma4:26b** locally (requires Ollama running on `localhost:11434`).
- For each candidate, it spawns `npx -y tavily-mcp@0.1.3` as a child process and communicates with it over **stdio MCP transport**.
- All candidate searches run in parallel via `CoroutineScope(Dispatchers.IO)`.
- Uses live Tavily search results to summarise the candidate's publicly available background.

---

## Candidate Data

Party JSON files are bundled as classpath resources under `src/main/resources/candidates-info/`. Each file is a flat JSON array of objects with the shape:

```json
{ "candidate_name": "...", "constituency": "...", "party_name": "..." }
```

Current parties covered: DMK, TVK, AIADMK, NTK, INC, BJP, MDMK, DMDK, PMK, AMMK.

---

## Prerequisites

| Requirement | Details |
|-------------|---------|
| JDK 23 | Required by `jvmToolchain(23)` in `build.gradle.kts` |
| Node.js + `npx` | Must be on `PATH`; used at runtime to spawn the Tavily MCP server |
| Ollama | Must be running locally at `http://localhost:11434` with `gemma4:26b` pulled |
| `TAVILY_API_KEY` | Tavily search API key — set in `src/main/kotlin/Constants.kt` |

> **Note:** `GOOGLE_API_KEY` is still present in `Constants.kt` but is not used by the current implementation.

---

## Setup

1. **Clone the repo:**
   ```bash
   git clone <repo-url>
   cd know-your-candidate
   ```

2. **Pull the Ollama model:**
   ```bash
   ollama pull gemma4:26b
   ```

3. **Add your Tavily API key** in `src/main/kotlin/Constants.kt`:
   ```kotlin
   object Constants {
       const val TAVILY_API_KEY = "YOUR_TAVILY_API_KEY"
   }
   ```

4. **Build and run:**
   ```bash
   ./gradlew run
   ```

---

## Usage

```
Enter the constituency name: Chennai Central
Fetching candidates for constituency: Chennai Central
*************************************************************************************
PartyName: DMK-candidates-list-2026
Candidate Info: {...}
**************************************************************...
Candidate Name: ..., Party: DMK, Constituency: Chennai Central, Candidate Info: <web-search summary>
**************************************************************...
...
Enter the constituency name:
```

The prompt loops until you terminate the process (`Ctrl+C`).

---

## Project Structure

```
src/main/kotlin/
├── Main.kt       # Entry point; REPL loop, parallel web search, agent factory
├── Tools.kt      # Loads all party JSON files from candidates-info/ directory
├── Utils.kt      # File reading (kotlinx.io) and constituency matching
└── Constants.kt  # GOOGLE_API_KEY and TAVILY_API_KEY

src/main/resources/candidates-info/
├── DMK-candidates-list-2026.json
├── TVK-candidates-list-2026.json
├── AIADMK-candidates-list-2026.json
├── NTK-candidates-list-2026-.json
├── INC-candidates-list-2026.json
├── BJP-candidates-list-2026.json
├── MDMK-candidates-list-2026.json
├── DMDK-candidates-list-2026.json
├── PMK-candidates-list-2026.json
└── AMMK-candidates-list-2026.json
```

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.10 |
| JVM target | 23 |
| Koog Agents | 0.6.2 |
| kotlinx.serialization | bundled with Koog |
| kotlinx.io | for filesystem access |
| LLM backend | Ollama (`gemma4:26b`) |
| MCP server | `tavily-mcp@0.1.3` (via `npx`) |
| Build tool | Gradle 8.10 (Kotlin DSL) |

---

## Commands

```bash
./gradlew build   # Compile and build
./gradlew run     # Run the application
./gradlew test    # Run tests
```
