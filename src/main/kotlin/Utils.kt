package org.example

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Utils {
    fun readJsonFile(filePath: String): JsonArray? {
        try {
            val jsonPath = Path(path = filePath)
            // Check if file exists using the pure-Kotlin filesystem
            if (!SystemFileSystem.exists(jsonPath)) return null

            // Open a source and read the entire content as a string
            val jsonString = SystemFileSystem.source(jsonPath).buffered().use { source ->
                source.readString()
            }

            // Parse as dynamic JSON object
            return Json.parseToJsonElement(jsonString).jsonArray
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getCandidateForConstituency(constituencyName: String, partyCandidatesInfoJson: JsonArray): JsonObject {
        for (candidate in partyCandidatesInfoJson) {
            val candidateConstituency = candidate.jsonObject["constituency"]?.jsonPrimitive?.content ?: ""
            if (candidateConstituency.contains(constituencyName, ignoreCase = true)) {
                return candidate.jsonObject
            }
        }
        return JsonObject(emptyMap())
    }
}