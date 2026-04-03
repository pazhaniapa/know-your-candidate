package org.example

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray

object Tools : ToolSet {
    @Tool
    @LLMDescription("Provides the candidates list for the 2026 Tamil Nadu Assembly Elections from the provided JSON files.")
    fun getCandidateList(): JsonObject {
        val tvkCandidatesInfoJson = Utils.readJsonResource("tvk-candidates-list-2026.json")
        val ntkCandidatesJson = Utils.readJsonResource("NTK-candidates-list-2026-.json")
        val dmkCandidatesInfoJson = Utils.readJsonResource("DMK-candidates-list-2026.json")
        val aiadmkCandidatesInfoJson = Utils.readJsonResource("AIADMK-candidates-list-2026.json")

        return JsonObject(
            mapOf(
                Pair("tvkCandidatesInfo", tvkCandidatesInfoJson ?: buildJsonArray { }),
                Pair("ntkCandidatesInfo", ntkCandidatesJson ?: buildJsonArray { }),
                Pair("dmkCandidatesInfo", dmkCandidatesInfoJson ?: buildJsonArray { }),
                Pair("aiadmkCandidatesInfo", aiadmkCandidatesInfoJson ?: buildJsonArray { })
            )
        )
    }
}