package io.github.lambdallama

import java.lang.StringBuilder

class ByteMatrix(
    private val numRows: Int,
    private val numCols: Int,
    value: Cell
) {
    val dim: Point get() = Point(numCols, numRows)
    private val buf: ByteArray = ByteArray(numRows * numCols).apply { fill(value.byte) }

    operator fun get(p: Point) = get(p.y, p.x)

    operator fun set(p: Point, value: Cell) = set(p.y, p.x, value)

    fun contains(p: Point): Boolean =
        p.x >= 0 && p.y >= 0 && p.x < numRows && p.y < numCols

    private operator fun get(i: Int, j: Int) = Cell(buf[i * numCols + j])

    private operator fun set(i: Int, j: Int, value: Cell) {
        buf[i * numCols + j] = value.byte
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (y in 0 until numRows) {
            for (x in 0 until numCols) {
                sb.append(get(y, x).byte.toChar())
            }

            sb.append('\n')
        }

        return sb.toString()
    }
}