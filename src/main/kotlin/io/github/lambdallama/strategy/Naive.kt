package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

val VISITED = Cell(255.toByte())

val Cell.canVisit get(): Boolean = !isObstacle && this != VISITED

object Naive : Strategy {
    private fun go(grid: ByteMatrix, u: Point, sink: ActionSink) {
        grid[u] = VISITED
        for (move in MOVES) {
            val v = move(u)
            if (v in grid && grid[v].canVisit) {
                sink(listOf(move))
                go(grid, v, sink)
                sink(listOf(move.flipped))
            }
        }
    }

    override fun run(state: State, sink: ActionSink) {
        go(state.grid.clone(), state.robot.position, sink)
    }
}

object NaiveIterative : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid.clone()
        val initial = state.robot.position
        val q = ArrayDeque<Point>()
        q.addFirst(initial)
        while (q.isNotEmpty()) {
            val u = q.first
            grid[u] = VISITED
            val move = MOVES.firstOrNull { move ->
                move(u).let { it in grid && grid[it].canVisit }
            }

            if (move == null) {
                q.removeFirst()
                if (q.isNotEmpty()) {
                    val v = q.peekFirst()
                    sink(listOf(MOVES.first { it(v) == u }.flipped))
                }
            } else {
                sink(listOf(move))
                val v = move(u)
                q.addFirst(v)
            }
        }
    }
}
