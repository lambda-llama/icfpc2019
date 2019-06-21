package io.github.lambdallama

import io.github.lambdallama.ui.*
import io.github.lambdallama.ui.Map
import java.io.File

fun main(args: Array<String>) {
    // ./gradlew run --args=path/to/prob-XXX.desc
    val path = args.firstOrNull() ?: throw IllegalStateException(
        "path/to/prob-XXX.desc")
    val state = Task.parse(File(path).readText()).toState()

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

    println(state.grid)
}
