package io.github.lambdallama

import kotlin.math.*

data class State(
    val grid: ByteMatrix,
    val robot: Robot
) {
    val maxPoints: Int = ceil(1000 * log2((grid.dim.x * grid.dim.y).toDouble())).toInt()

    companion object {
        fun parse(s: String): State {
            val (rawMap, rawInitial, rawObstacles, rawBoosters) = s.split('#')
            val obstacles = rawObstacles.splitToSequence(';')
                .filter { it.isNotEmpty() }
                .map { Poly.parse(it) }
                .toList()
            val boosters = rawBoosters.splitToSequence(';')
                .filter { it.isNotEmpty() }
                .map { Booster.parse(it) }
                .toList()
            val map = Poly.parse(rawMap)
            val initialLoc = Point.parse(rawInitial)

            val (bottomLeft, topRight) = map.bbox
            val (minX, minY) = bottomLeft
            val (maxX, maxY) = topRight
            // TODO(superbobry): apply shift if minX/minY are non-zero.
            check(minX == 0 && minY == 0)
            val numRows = maxY - minY
            val numCols = maxX - minX
            val grid = ByteMatrix(numRows, numCols, Cell.VOID)
            listOf(map).project(grid, Cell.FREE)
            obstacles.project(grid, Cell.OBSTACLE)
            for (booster in boosters) {
                grid[booster.loc] = booster.type.toCell()
            }

            return State(
                grid,
                robot = Robot(position = initialLoc, tentacles = listOf(
                    Point(1, 0),
                    Point(1, 1),
                    Point(1, -1)
                )))
        }
    }
}

enum class BoosterType {
    B, F, L, X, R;

    fun toCell(): Cell = when (this) {
        B -> Cell.B_EXTENSION
        F -> Cell.B_FAST_WHEELS
        L -> Cell.B_DRILL
        X -> Cell.B_MYSTERIOUS_POINT
        R -> Cell.B_TELEPORT
    }
}

data class Booster(
    val type: BoosterType,
    val loc: Point
) {
    companion object {
        fun parse(s: String): Booster = Booster(
            type = BoosterType.valueOf(s.take(1)),
            loc = Point.parse(s.drop(1)))
    }
}

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

