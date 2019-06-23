package io.github.lambdallama

import com.beust.klaxon.*
import java.nio.file.Files
import java.nio.file.Paths

private const val BEST_TIME_KEY = "bestTime"

data class SolutionMetadata(private val path: String, private val obj: JsonObject) {
    var bestTime
        get() = obj.int(BEST_TIME_KEY) ?: 0
        set(value) { obj[BEST_TIME_KEY] = value }

    fun getTimes(): Map<String, Int> {
        return obj.toSortedMap()
                .filterKeys { it != BEST_TIME_KEY }
                .mapValues { (k, v) -> v as Int }
    }

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
