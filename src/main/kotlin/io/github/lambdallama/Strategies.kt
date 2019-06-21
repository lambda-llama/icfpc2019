package io.github.lambdallama

val DX = arrayOf(-1, 0, 1, 0)
val DY = arrayOf(0, -1, 0, 1)
val DIR_NAMES = "AWDS"

class Naive(var state: State) {
    private lateinit var board: ByteMatrix
    private lateinit var path: String

    fun go(u: Point) {
        board[u] = Cell.WRAPPED
        for (i in 0..3) {
            val v = Point(u.x + DX[i], u.y + DY[i])
            if (board.contains(v) && board[v] == Cell.FREE) {
                path += DIR_NAMES[i]
                go(v)
                path += DIR_NAMES[i xor 2]
            }
        }
    }

    fun traverse(start: Point): String {
        board = state.grid
        path = ""
        go(start)
        return path
    }
}
