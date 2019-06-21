package io.github.lambdallama

import io.github.lambdallama.ui.*
import java.io.File

fun nonInteractiveMain(path: String) {
    val state = State.parse(File(path).readText())
    println("Map: $path, max points: ${state.maxPoints}")
    val solutionFile = File(path.substring(0, path.length - 5) + ".sol")
    val strategy = Naive
    val actions = mutableListOf<Action>()
    strategy.run(state, actions::plusAssign)
    System.err.println(actions.size)
    solutionFile.writeText(actions.joinToString(""))
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
    Naive.run(state, visualize(state, true))
}
