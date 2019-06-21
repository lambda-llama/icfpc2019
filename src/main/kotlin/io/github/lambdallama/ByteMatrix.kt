package io.github.lambdallama

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
}