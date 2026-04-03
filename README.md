# Know Your Candidate

A Kotlin-based CLI tool that helps voters learn about candidates in the **2026 Tamil Nadu Assembly Elections**. Enter a constituency name and get a structured profile of every candidate standing there — pulled from local party data and enriched with live web search results.

---

## How It Works

The app runs an interactive REPL and chains two AI agents for each query:

```
User Input (constituency name)
        │
        ▼
┌─────────────────────────┐
│  getCandidateInfoAgent  │  ← Koog AIAgent + Gemini 2.5 Flash
│  Tool: getCandidateList │  ← Reads 4 party JSON files from classpath
│  Returns: [{name, party,│
│            constituency}]│
└────────────┬────────────┘
             │  (one iteration per candidate)
             ▼
┌─────────────────────────┐
│   getWebSearchAgent     │  ← Koog AIAgent + Gemini 2.5 Flash
│   MCP: tavily-mcp       │  ← Spawns `npx tavily-mcp@0.1.3` over stdio
│   Returns: summary of   │
│   publicly available    │
│   candidate background  │
└────────────┬────────────┘
             │
             ▼
     Printed to console
```

### Agent 1 — `getCandidateInfoAgent`

- Backed by **Gemini 2.5 Flash** via the Koog framework.
- Has access to a single Koog `ToolSet` (`Tools.getCandidateList`) that loads all four party JSON files from the JVM classpath and returns them as combined JSON.
- Given a constituency name, it identifies matching candidates across DMK, AIADMK, TVK, and NTK.

### Agent 2 — `getWebSearchAgent`

- Also backed by **Gemini 2.5 Flash**.
- For each candidate, it spawns `npx -y tavily-mcp@0.1.3` as a child process and communicates with it over **stdio MCP transport**.
- Uses live Tavily search results to summarise the candidate's publicly available background.

---

## Candidate Data

Party JSON files are bundled as classpath resources under `src/main/resources/`. Each file is a flat JSON array of objects with the shape:

```json
{ "candidate_name": "...", "constituency": "...", "party_name": "..." }
```

| Party  | File                              | Approx. candidates |
|--------|-----------------------------------|--------------------|
| DMK    | `DMK-candidates-list-2026.json`   | 245                |
| TVK    | `tvk-candidates-list-2026.json`   | 293                |
| AIADMK | `AIADMK-candidates-list-2026.json`| 209                |
| NTK    | `NTK-candidates-list-2026-.json`  | 59                 |

---

## Prerequisites

| Requirement | Details |
|-------------|---------|
| JDK 23 | Required by `jvmToolchain(23)` in `build.gradle.kts` |
| Node.js + `npx` | Must be on `PATH`; used at runtime to spawn the Tavily MCP server |
| `GOOGLE_API_KEY` | Gemini API key — set in `src/main/kotlin/Constants.kt` |
| `TAVILY_API_KEY` | Tavily search API key — set in `src/main/kotlin/Constants.kt` |

---

## Setup

1. **Clone the repo:**
   ```bash
   git clone <repo-url>
   cd know-your-candidate
   ```

2. **Add your API keys** in `src/main/kotlin/Constants.kt`:
   ```kotlin
   object Constants {
       const val GOOGLE_API_KEY = "YOUR_GOOGLE_API_KEY"
       const val TAVILY_API_KEY = "YOUR_TAVILY_API_KEY"
   }
   ```

3. **Build and run:**
   ```bash
   ./gradlew run
   ```

---

## Usage

```
Enter the constituency name: Chennai Central
Fetching candidates for constituency: Chennai Central
**************************************************************
Candidate Name: ..., Party: DMK, Constituency: Chennai Central
Candidate Info: <web-search summary>
**************************************************************
...
Enter the constituency name:
```

The prompt loops until you terminate the process (`Ctrl+C`).

---

## Project Structure

```
src/main/kotlin/
├── Main.kt       # Entry point; REPL loop, agent factory functions
├── Tools.kt      # Koog ToolSet — loads all party JSON files
├── Utils.kt      # Reads JSON resources from JVM classpath
└── Constants.kt  # GOOGLE_API_KEY and TAVILY_API_KEY

src/main/resources/
├── DMK-candidates-list-2026.json
├── tvk-candidates-list-2026.json
├── AIADMK-candidates-list-2026.json
└── NTK-candidates-list-2026-.json
```

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.10 |
| JVM target | 23 |
| Koog Agents | 0.6.2 |
| kotlinx.serialization | bundled with Koog |
| Gemini model | Gemini 2.5 Flash |
| MCP server | `tavily-mcp@0.1.3` (via `npx`) |
| Build tool | Gradle 8.10 (Kotlin DSL) |

---

## Commands

```bash
./gradlew build   # Compile and build
./gradlew run     # Run the application
./gradlew test    # Run tests
```
