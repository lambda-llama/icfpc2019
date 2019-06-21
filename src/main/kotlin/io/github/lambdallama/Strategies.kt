package io.github.lambdallama

interface Strategy {
    fun run(state: State): List<Action>
}

val DX = arrayOf(-1, 0, 1, 0)
val DY = arrayOf(0, -1, 0, 1)

object Naive: Strategy {
    private fun go(grid: ByteMatrix, u: Point, path: MutableList<Action>) {
        grid[u] = Cell.WRAPPED
        for (i in 0..3) {
            val v = Point(u.x + DX[i], u.y + DY[i])
            if (v in grid && grid[v] == Cell.FREE) {
                path.add(Move(i))
                go(grid, v, path)
                path.add(Move(i xor 2))
            }
        }
    }

    override fun run(state: State): List<Action> {
        val actions = mutableListOf<Action>()
        go(state.grid, state.robot.position, actions)
        return actions
    }
}
