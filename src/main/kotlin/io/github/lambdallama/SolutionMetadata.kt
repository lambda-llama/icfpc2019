package io.github.lambdallama

import com.beust.klaxon.*
import java.nio.file.Files
import java.nio.file.Paths

data class SolutionMetadata(val path: String, var bestTime: Int) {
    fun saveToDisk() {
        val json = JsonObject(mapOf(
                "bestTime" to bestTime
        )).toJsonString()
        Files.write(Paths.get(path), json.toByteArray())
    }

    companion object {
        fun parse(path: String): SolutionMetadata {
            if (!Files.exists(Paths.get(path))) {
                return SolutionMetadata(path, 0)
            }

            val obj = Parser.default().parse(path) as JsonObject
            return SolutionMetadata(
                    path = path,
                    bestTime = obj.int("bestTime")!!
            )
        }
    }
}
