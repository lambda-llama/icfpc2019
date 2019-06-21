package io.github.lambdallama

data class State(
    val grid: ByteMatrix,
    val robot: Robot
)

inline class Cell(val byte: Byte) {
    companion object {
        val OBSTACLE = Cell('O'.toByte())
        val WRAPPED = Cell('W'.toByte())
        val FREE = Cell(' '.toByte())
        val VOID = Cell('V'.toByte())

        val B_EXTENSION: Cell get() = Cell('B'.toByte())
        val B_FAST_WHEELS: Cell get() = Cell('F'.toByte())
        val B_DRILL: Cell get() = Cell('L'.toByte())
        val B_MYSTERIOUS_POINT: Cell get() = Cell('X'.toByte())
        val B_TELEPORT: Cell get() = Cell('T'.toByte())
    }
}

