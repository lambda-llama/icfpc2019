package io.github.lambdallama

class ByteMatrix private constructor(
    private val numRows: Int,
    private val numCols: Int,
    private val buf: ByteArray,
    private var numObstacle: Int,
    private var numVoid: Int,
    private var numWrapped: Int,
    private var numFree: Int
) {
    val dim: Point get() = Point(numCols, numRows)

    operator fun get(p: Point) = get(p.y, p.x)

    operator fun set(p: Point, value: Cell) {
        when (get(p).byte) {
            Cell.OBSTACLE.byte -> numObstacle--
            Cell.VOID.byte -> numVoid--
            Cell.WRAPPED.byte -> numWrapped--
            Cell.FREE.byte -> numFree--
        }
        set(p.y, p.x, value)
        when (value.byte) {
            Cell.OBSTACLE.byte -> numObstacle++
            Cell.VOID.byte -> numVoid++
            Cell.WRAPPED.byte -> numWrapped++
            Cell.FREE.byte -> numFree++
        }
    }

    fun clone() = ByteMatrix(
        numRows,
        numCols,
        buf.clone(),
        numObstacle,
        numVoid,
        numWrapped,
        numFree)

    operator fun contains(p: Point): Boolean =
        p.x >= 0 && p.y >= 0 && p.x < dim.x && p.y < dim.y

    fun count(cell: Cell): Int = when (cell.byte) {
        Cell.OBSTACLE.byte -> numObstacle
        Cell.VOID.byte -> numVoid
        Cell.WRAPPED.byte -> numWrapped
        Cell.FREE.byte -> numFree
        else -> error("Cannot count $cell entries")
    }

    private operator fun get(i: Int, j: Int) = Cell(buf[i * numCols + j])

    private operator fun set(i: Int, j: Int, value: Cell) {
        buf[i * numCols + j] = value.byte
    }

    companion object {
        operator fun invoke(numRows: Int, numCols: Int, value: Cell): ByteMatrix {
            val size = numRows * numCols
            val buf = ByteArray(size).apply { fill(value.byte) }
            return ByteMatrix(
                numRows,
                numCols,
                buf,
                numObstacle = if (value == Cell.OBSTACLE) size else 0,
                numVoid = if (value == Cell.VOID) size else 0,
                numWrapped = if (value == Cell.WRAPPED) size else 0,
                numFree = if (value == Cell.FREE) size else 0)
        }
    }
}