package io.github.lambdallama

import java.lang.StringBuilder

class ByteMatrix private constructor(
    private val numRows: Int,
    private val numCols: Int,
    private val buf: ByteArray
) {
    constructor(numRows: Int,
                numCols: Int, value: Cell)
        : this(numRows, numCols, ByteArray(numRows * numCols).apply { fill(value.byte) })

    val dim: Point get() = Point(numCols, numRows)

    operator fun get(p: Point) = get(p.y, p.x)

    operator fun set(p: Point, value: Cell) = set(p.y, p.x, value)

    fun contains(p: Point): Boolean =
        p.x >= 0 && p.y >= 0 && p.x < dim.x && p.y < dim.y

    private operator fun get(i: Int, j: Int) = Cell(buf[i * numCols + j])

    private operator fun set(i: Int, j: Int, value: Cell) {
        buf[i * numCols + j] = value.byte
    }

    fun clone(): ByteMatrix = ByteMatrix(numRows, numCols, buf = buf.clone())

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