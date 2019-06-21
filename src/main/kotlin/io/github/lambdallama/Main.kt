package io.github.lambdallama

import io.github.lambdallama.ui.*
import java.io.File

fun nonInteractiveMain(path: String) {
    val state = State.parse(File(path).readText())
    println("Map: $path, max points: ${state.maxPoints}")

    val solutions = arrayOf(
        NaiveIterative,
        GreedyUnordered,
        GreedySameMoveFirst
    ).map {
        val actions = mutableListOf<Action>()
        it.run(state.clone(), actions::plusAssign)
        System.err.println("${it.javaClass.simpleName}: ${actions.size}")
        actions
    }

    val solutionFile = File(path.substring(0, path.length - 5) + ".sol")
    solutionFile.writeText(solutions.minBy { it.size }!!.joinToString(""))
}

fun main(args: Array<String>) {
    when (args[0]) {
        "--non-interactive" -> {
            return nonInteractiveMain(args[1])
        }
    }

    // ./gradlew run --args=path/to/prob-XXX.desc
    val path = args.firstOrNull() ?: throw IllegalStateException(
        "path/to/prob-XXX.desc")
    val state = State.parse(File(path).readText())

    launchGui()
    NaiveIterative.run(state, visualize(state, true))
}
