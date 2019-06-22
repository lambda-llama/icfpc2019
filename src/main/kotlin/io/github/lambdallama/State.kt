package io.github.lambdallama

import kotlin.math.*

data class State(
    var grid: ByteMatrix,
    var robot: Robot
) {
    val maxPoints: Int = ceil(1000 * log2((grid.dim.x * grid.dim.y).toDouble())).toInt()

    fun clone() = State(grid.clone(), robot.copy())

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
                robot = Robot(
                    position = initialLoc,
                    tentacles = listOf(
                        Point(1, 0),
                        Point(1, 1),
                        Point(1, -1)
                    ),
                    orientation = Orientation.RIGHT
                )
            )
        }
    }

    fun apply(action: Action) {
        when (action) {
            is Move -> {
                robot = robot.copy(position = robot.position.apply(action))
                wrap()
            }
        }
    }

    fun wrap() {
        for (point in robot.parts) {
            if (grid.contains(point) && grid[point] == Cell.FREE) {
                grid[point] = Cell.WRAPPED
            }
        }

    }
}

fun Point.apply(move: Move) = Point(x + move.dx, y + move.dy)


enum class BoosterType {
    B, F, L, X, R, C;
    // TODO: X is not a booster, it's a static spawn point

    fun toCell(): Cell = when (this) {
        B -> Cell.B_EXTENSION
        F -> Cell.B_FAST_WHEELS
        L -> Cell.B_DRILL
        X -> Cell.SPAWN_POINT
        R -> Cell.B_TELEPORT
        C -> Cell.B_CLONE
    }

    companion object {
        fun fromCell(c: Cell): BoosterType = when (c) {
            Cell.B_EXTENSION -> B
            Cell.B_FAST_WHEELS -> F
            Cell.B_DRILL -> L
            Cell.B_TELEPORT -> R
            Cell.B_CLONE -> C
            else -> throw Exception("Invalid Cell $c")
        }
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
    val isWrapable: Boolean get() = when (this) {
        OBSTACLE, VOID, WRAPPED -> false
        else -> true
    }

    val isBooster: Boolean get() = when (this) {
        B_EXTENSION, B_FAST_WHEELS, B_DRILL, B_TELEPORT, B_CLONE -> true
        else -> false
    }

    companion object {
        val OBSTACLE = Cell('O'.toByte())
        val WRAPPED = Cell('W'.toByte())
        val FREE = Cell(' '.toByte())
        val VOID = Cell('V'.toByte())
        val SPAWN_POINT: Cell get() = Cell('X'.toByte())

        val B_EXTENSION: Cell get() = Cell('B'.toByte())
        val B_FAST_WHEELS: Cell get() = Cell('F'.toByte())
        val B_DRILL: Cell get() = Cell('L'.toByte())
        val B_TELEPORT: Cell get() = Cell('T'.toByte())
        val B_CLONE: Cell get() = Cell('C'.toByte())
    }
}

