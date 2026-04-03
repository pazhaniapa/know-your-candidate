package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    runBlocking {
        while (true) {
            print("Enter the constituency name: ")
            val constituencyName = readLine()?.trim() ?: ""

            val candidatesJson = fetchCandidates(constituencyName, getCandidateInfoAgent())

            if (candidatesJson is JsonArray) {
                for (candidate in candidatesJson) {
                    val name = candidate.jsonObject["name"]?.jsonPrimitive?.content ?: "N/A"
                    val party = candidate.jsonObject["party"]?.jsonPrimitive?.content ?: "N/A"
                    val constituency = candidate.jsonObject["constituency"]?.jsonPrimitive?.content ?: "N/A"

                    val candidateInfo = webSearch(candidateName = name, partyName = party, getWebSearchAgent())

                    println("******************************************************************************************************************************************************************************")

                    println("Candidate Name: $name, Party: $party, Constituency: $constituency, Candidate Info: $candidateInfo")

                    println("******************************************************************************************************************************************************************************")
                }
            }
        }
    }
}


suspend fun fetchCandidates(constituencyName: String, candidateInfoAgent: AIAgent<String, String>): JsonElement? {
    if (constituencyName.isBlank()) {
        println("Constituency name cannot be empty.")
        return null
    }
    println("Fetching candidates for constituency: $constituencyName")

    val prompt =
        "who are the candidates for $constituencyName constituency? Give the response in a json array in this format: [{name: '', party: '', constituency: ''}]. This json array string should be parsable by kotlin serialization library. Only give the response as json array string, no other text."

    //println("Prompt: $prompt")

    val response = candidateInfoAgent.run(prompt)

    //println("Response: $response")

    var formattedResponse = ""
    if (response.contains("```json")) {
        //println("Cleaning response by removing code block markers")
        formattedResponse = response.replace("```json", "").replace("```", "").trim()
        //println("Cleaned Response: $formattedResponse")
    } else {
        println("Response does not contain code block markers, using as is.")
    }

    val candidatesJsonArray = Json.parseToJsonElement(formattedResponse)

    return candidatesJsonArray
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

    val aiAgent = AIAgent(
        promptExecutor = simpleGoogleAIExecutor(Constants.GOOGLE_API_KEY),
        systemPrompt = "You are information summarization assistant. Use the provided MCP search tools to search the web for information and answer the question based on that.",
        llmModel = GoogleModels.Gemini2_5Flash,
        toolRegistry = mcpToolRegistry
    )

    return aiAgent
}

private fun getCandidateInfoAgent(): AIAgent<String, String> {
    val executor = simpleGoogleAIExecutor(Constants.GOOGLE_API_KEY)

    //Candidates information has been exposed as Tools in Tools.kt file.
    val registry = ToolRegistry {
        tools(Tools.asTools())
    }

    val agent = AIAgent(
        promptExecutor = executor,
        systemPrompt = "You are a helpful Know Your Candidate assistant for Tamil Nadu, India state election 2026. You have the candidates information from various parties as json from the provided tools.",
        toolRegistry = registry,
        llmModel = GoogleModels.Gemini2_5Flash // Or Gemini2_0Pro for larger docs
    )

    return agent
}