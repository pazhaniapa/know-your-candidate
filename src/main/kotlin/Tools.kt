package org.example

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import java.io.File
import java.net.URI
import java.util.Collections.emptyMap

object Tools {
    suspend fun getCandidateList(constituencyName : String) : JsonObject  {
        val resultCandidateMap = mutableMapOf<String, JsonElement>()

        val loader = Thread.currentThread().contextClassLoader
        val resource = loader.getResource("candidates-info")
        val directory = File(resource!!.path)
        val scope = CoroutineScope(Dispatchers.Default)
        val jobs = mutableListOf<Job>()

        if (directory.exists() && directory.isDirectory) {
            val jsonFiles = directory.listFiles { _, name ->
                name.endsWith(".json")
            }

            jsonFiles?.forEach { file ->
                val content = Utils.readJsonFile(file.absolutePath)
                if (content != null && content.isNotEmpty()) {
                    val job = scope.launch {
                        Utils.getCandidateForConstituency(constituencyName, content).let {
                            if(it.isNotEmpty()) {
                                resultCandidateMap[file.nameWithoutExtension] = it
                            }
                        }
                    }
                    jobs.add(job)
                }
            }
        } else {
            println("Directory not found!")
        }

        jobs.forEach { it.join() }

        // Build immutable JsonObject from mutable map
        val resultJsonObject = JsonObject(resultCandidateMap)

        return resultJsonObject
    }
}