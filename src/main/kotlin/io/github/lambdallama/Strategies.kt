package io.github.lambdallama

import java.util.*

interface Strategy {
    fun run(state: State, sink: ActionSink)
}

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

object Naive : Strategy {
    private fun go(grid: ByteMatrix, u: Point, sink: ActionSink) {
        grid[u] = Cell.WRAPPED
        for (move in MOVES) {
            val v = move(u)
            if (v in grid && grid[v] == Cell.FREE) {
                sink(move)
                go(grid, v, sink)
                sink(move.flipped)
            }
        }
    }

    override fun run(state: State, sink: ActionSink) {
        go(state.grid, state.robot.position, sink)
    }
}

object NaiveIterative : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid
        val initial = state.robot.position
        val q = ArrayDeque<Point>()
        q.add(initial)
        while (q.isNotEmpty()) {
            val u = q.first
            grid[u] = Cell.WRAPPED
            val move = MOVES.firstOrNull { move ->
                move(u).let { it in grid && grid[it] == Cell.FREE }
            }

            if (move == null) {
                q.removeFirst()
                if (q.isNotEmpty()) {
                    val v = q.peekFirst()
                    sink(MOVES.first { it(v) == u }.flipped)
                }
            } else {
                sink(move)
                val v = move(u)
                q.addFirst(v)
            }
        }
    }
}