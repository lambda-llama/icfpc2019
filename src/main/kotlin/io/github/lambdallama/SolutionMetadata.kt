package io.github.lambdallama

import com.beust.klaxon.*
import java.nio.file.Files
import java.nio.file.Paths

data class SolutionMetadata(private val path: String, private val obj: JsonObject) {
    var bestTime
        get() = obj.int("bestTime") ?: 0
        set(value) { obj["bestTime"] = value }

    fun getTime(strategy: String): Int? {
        return obj.int(strategy)
    }

    fun setTime(strategy: String, time: Int) {
        obj[strategy] = time
    }

    fun saveToDisk() {
        Files.write(Paths.get(path), JsonObject(obj.toSortedMap()).toJsonString().toByteArray())
    }

    companion object {
        fun parse(path: String): SolutionMetadata {
            if (!Files.exists(Paths.get(path))) {
                return SolutionMetadata(path, JsonObject())
            }

            val obj = Parser.default().parse(path) as JsonObject
            return SolutionMetadata(
                    path = path,
                    obj = obj
            )
        }
    }
}
