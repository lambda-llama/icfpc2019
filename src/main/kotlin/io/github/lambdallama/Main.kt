package io.github.lambdallama

import io.github.lambdallama.ui.*
import io.github.lambdallama.ui.Map
import java.io.File

fun main(args: Array<String>) {
    val task = Task.parse(File("part-1-initial/prob-002.desc").readText())
    val state = task.toState()

    launchGui()
    val pills: MutableList<Pair<Point, Pill>> = mutableListOf()
    draw(Map(state.grid.dim,
        { p ->
            val c = state.grid[p]
            if (state.wrappy.contains(p)) {
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
