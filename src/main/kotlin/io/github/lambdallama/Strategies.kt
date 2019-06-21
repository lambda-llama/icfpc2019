package io.github.lambdallama

interface Strategy {
    fun run(state: State): List<Action>
}

object Naive: Strategy {
    private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

    private fun go(grid: ByteMatrix, u: Point, path: MutableList<Action>) {
        grid[u] = Cell.WRAPPED
        for (m in MOVES) {
            val v = Point(u.x + m.dx, u.y + m.dy)
            if (v in grid && grid[v] == Cell.FREE) {
                path.add(m)
                go(grid, v, path)
                path.add(m.flipped)
            }
        }
    }

    override fun run(state: State): List<Action> {
        val actions = mutableListOf<Action>()
        go(state.grid, state.robot.position, actions)
        return actions
    }
}
