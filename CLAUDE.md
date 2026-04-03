# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build   # Compile and build
./gradlew run     # Run the application
./gradlew test    # Run tests (none written yet)
```

**Runtime prerequisites:**
- JDK 23 (required by `jvmToolchain(23)`)
- Node.js + `npx` on `PATH` (used at runtime to spawn the Tavily MCP server)
- Valid `GOOGLE_API_KEY` and `TAVILY_API_KEY` in `src/main/kotlin/Constants.kt`

## Architecture

This is a Kotlin CLI app that answers questions about candidates in the 2026 Tamil Nadu Assembly Elections. It runs an interactive REPL loop with two Koog AI agents chained together:

1. **`getCandidateInfoAgent`** — A Koog `AIAgent` backed by Gemini 2.5 Flash. It has access to one tool (`Tools.getCandidateList`) which loads all four party JSON files from classpath resources and returns combined candidate data. Given a constituency name, this agent identifies matching candidates across DMK, AIADMK, TVK, and NTK.

2. **`getWebSearchAgent`** — A second Koog `AIAgent` also backed by Gemini 2.5 Flash. For each candidate returned by the first agent, this agent spawns `npx -y tavily-mcp@0.1.3` as a child process and communicates with it over stdio MCP transport to perform live web searches and summarize publicly available background info.

### Key files

- `Main.kt` — Entry point; `main()` REPL loop, `fetchCandidates()`, `webSearch()`, agent factory functions
- `Tools.kt` — Koog `ToolSet` with `@Tool`-annotated `getCandidateList()` that loads all party JSON files
- `Utils.kt` — `readJsonResource()` loads JSON files from JVM classpath via `getResourceAsStream()`
- `Constants.kt` — Hardcoded `GOOGLE_API_KEY` and `TAVILY_API_KEY`
- `src/main/resources/` — Four JSON files (DMK ~245, TVK ~293, AIADMK ~209, NTK ~59 candidates), each a flat array of `{ candidate_name, constituency, party_name }` objects

### Dependencies

- `ai.koog:koog-agents:0.6.2` — JetBrains' AI agent framework (tools, MCP, agent orchestration)
- `kotlinx.serialization` — JSON parsing for candidate data files
- Gradle 8.10 with Kotlin DSL (`build.gradle.kts`)