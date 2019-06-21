package io.github.lambdallama

import io.github.lambdallama.ui.*
import io.github.lambdallama.ui.Map
import java.io.File

fun nonInteractiveMain(path: String) {
    val state = State.parse(File(path).readText())
    val solutionFile = File(path.substring(0, path.length - 5) + ".sol")
    val strategy = Naive
    strategy.run(state).also { actions ->
        System.err.println(actions.size)
        solutionFile.writeText(actions.joinToString(""))
    }
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
    val pills: MutableList<Pair<Point, Pill>> = mutableListOf()
    draw(Map(state.grid.dim,
        { p ->
            val c = state.grid[p]
            if (state.robot.parts.contains(p)) {
                pills += p to Pill.ROBOT
            }
            when (c) {
                Cell.OBSTACLE -> UiCell.WALL
                Cell.OBSTACLE -> UiCell.WALL
                Cell.FREE -> UiCell.FREE
                Cell.WRAPPED -> UiCell.WRAPPED
                Cell.VOID -> UiCell.VOID
                else -> {
                    when (c) {
                        Cell.B_EXTENSION -> pills += p to Pill.BOOST_B
                        Cell.B_DRILL -> pills += p to Pill.BOOST_L
                        Cell.B_FAST_WHEELS -> pills += p to Pill.BOOST_F
                        Cell.B_MYSTERIOUS_POINT-> pills += p to Pill.BOOST_X
                    }
                    UiCell.FREE
                }
            }

        }, pills))
    for (i in 0 until 100) {
        Thread.sleep(300)
    }
}
