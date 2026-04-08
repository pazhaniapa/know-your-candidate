package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.IO.println

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    runBlocking {
        while (true) {
            print("Enter the constituency name: ")
            val constituencyName = readLine()?.trim() ?: ""

            val candidatesListJsonObject = fetchCandidates(constituencyName)
            val scope = CoroutineScope(Dispatchers.IO)
            val jobs = mutableListOf<Job>()

            candidatesListJsonObject?.let {
                for (candidate in it) {
                    println("PartyName: ${candidate.key}")
                    println("Candidate Info: ${candidate.value}")
                    val candidateInfoJson = candidate.value.jsonObject
                    val name = candidateInfoJson["candidate_name"]?.jsonPrimitive?.content ?: ""
                    val party = candidateInfoJson["party_name"]?.jsonPrimitive?.content ?: ""
                    val constituency = candidateInfoJson["constituency"]?.jsonPrimitive?.content ?: ""

                    val job = scope.launch {
                        if (!name.isEmpty() && !party.isEmpty() && !constituency.isEmpty()) {
                            val candidateInfo = webSearch(candidateName = name, partyName = party, getWebSearchAgent())

                            println("******************************************************************************************************************************************************************************")

                            println("Candidate Name: $name, Party: $party, Constituency: $constituency, Candidate Info: $candidateInfo")

                            println("******************************************************************************************************************************************************************************")
                        }
                    }
                    jobs.add(job)
                }
                jobs.forEach {it.join()}
            }

        }
    }
}


suspend fun fetchCandidates(constituencyName: String): JsonObject? {
    if (constituencyName.isBlank()) {
        println("Constituency name cannot be empty.")
        return null
    }
    println("Fetching candidates for constituency: $constituencyName")
    println("*************************************************************************************")
    return Tools.getCandidateList(constituencyName)
}

private suspend fun webSearch(
    candidateName: String,
    partyName: String,
    webSearchAgent: AIAgent<String, String>
): String {
    val prompt = "search about this person and summarise the information. $partyName candidate $candidateName"

    //println("Prompt : $prompt")

    val response = webSearchAgent.run(prompt)

    //println("Response: $response")

    return response
}

private suspend fun getWebSearchAgent(): AIAgent<String, String> {
    val process = ProcessBuilder(
        "npx", "-y", "tavily-mcp@0.1.3"
    ).apply {
        // Pass your Tavily API Key
        environment()["TAVILY_API_KEY"] = Constants.TAVILY_API_KEY
        // Optional: Configure default search behavior (results count, depth, etc.)
        // environment()["DEFAULT_PARAMETERS"] = """{"max_results": 5, "search_depth": "advanced"}"""

        // Crucial for troubleshooting: redirects JS errors to your Kotlin console
        redirectError(ProcessBuilder.Redirect.INHERIT)
    }.start()

    // 2. Create the stdio transport to communicate with the process
    val transport = McpToolRegistryProvider.defaultStdioTransport(process)

    // 3. Create a Tool Registry that discovers tools from this server
    val mcpToolRegistry = McpToolRegistryProvider.fromTransport(
        transport = transport,
        name = "tavily-search-server", version = "1.0.0"
    )

    //val executor = simpleGoogleAIExecutor(Constants.GOOGLE_API_KEY)

    val executor = SingleLLMPromptExecutor(OllamaClient(baseUrl = "http://localhost:11434"))

    val gemma4Model = LLModel(
        provider = LLMProvider.Ollama,
        id = "gemma4:26b",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Tools
        ),
        contextLength = 200000
    )

    val aiAgent = AIAgent(
        promptExecutor = executor,
        systemPrompt = "You are information summarization assistant. Use the provided MCP search tools to search the web for information and answer the question based on that.",
        //llmModel = GoogleModels.Gemini2_5Flash,
        llmModel = gemma4Model,
        toolRegistry = mcpToolRegistry
    )

    return aiAgent
}