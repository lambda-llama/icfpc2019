package io.github.lambdallama

interface Strategy {
    fun run(state: State, sink: ActionSink)
}

object Naive: Strategy {
    private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

    private fun go(grid: ByteMatrix, u: Point, sink: ActionSink) {
        grid[u] = Cell.WRAPPED
        for (m in MOVES) {
            val v = Point(u.x + m.dx, u.y + m.dy)
            if (v in grid && grid[v] == Cell.FREE) {
                sink(m)
                go(grid, v, sink)
                sink(m.flipped)
            }
        }
    }

    override fun run(state: State, sink: ActionSink) {
        go(state.grid, state.robot.position, sink)
    }
}
