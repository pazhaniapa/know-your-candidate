package org.example

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import java.io.File
import java.net.URI
import java.util.Collections.emptyMap

object Tools : ToolSet {
    @Tool
    @LLMDescription("Provides the candidates list for the 2026 Tamil Nadu Assembly Elections from the provided JSON files.")
    fun getCandidateList() : JsonObject  {
        // Create mutable map to collect JSON data
        val resultMap = mutableMapOf<String, JsonElement>()

        val loader = Thread.currentThread().contextClassLoader
        val resource = loader.getResource("candidates-info")
        val directory = File(resource!!.path)

        if (directory.exists() && directory.isDirectory) {
            val jsonFiles = directory.listFiles { _, name ->
                name.endsWith(".json")
            }

            jsonFiles?.forEach { file ->
                val content = Utils.readJsonFile(file.absolutePath)
                if (content != null) {
                    resultMap[file.nameWithoutExtension] = content
                }
            }
        } else {
            println("Directory not found!")
        }

        // Build immutable JsonObject from mutable map
        val resultJsonObject = JsonObject(resultMap)

        return resultJsonObject

    }
}