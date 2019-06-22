package io.github.lambdallama

import kotlin.math.*

data class State(
    val grid: ByteMatrix,
    val boosters: MutableMap<Point, BoosterType>,
    val robot: Robot
) {
    val maxPoints: Int = ceil(1000 * log2((grid.dim.x * grid.dim.y).toDouble())).toInt()

    fun clone() = State(grid.clone(), HashMap(boosters), robot.clone())

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

            return State(
                grid,
                boosters.map { it.loc to it.type }.toMap(HashMap()),
                robot = Robot(
                    position = initialLoc,
                    tentacles = mutableListOf(
                        Point(1, 0),
                        Point(1, 1),
                        Point(1, -1)
                    ),
                    orientation = Orientation.RIGHT
                )
            ).apply { wrap() }
        }
    }

    fun apply(action: Action) {
        when (action) {
            is Move -> {
                robot.position = robot.position.apply(action)
                wrap()
            }
            is TurnClockwise -> {
                robot.rotate(Rotation.CLOCKWISE)
                wrap()
            }
            is TurnCounter -> {
                robot.rotate(Rotation.COUNTERCLOCKWISE)
                wrap()
            }
            is Attach -> {
                robot.tentacles.add(action.location)
            }
        }
    }

    fun wrap() = robot.wrap(grid)
}

fun Point.apply(move: Move) = Point(x + move.dx, y + move.dy)


enum class BoosterType {
    B, F, L, X, R, C;
    // TODO: X is not a booster, it's a static spawn point
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
    val isObstacle: Boolean
        get() = when (this) {
            OBSTACLE, VOID -> true
            else -> false
        }

    val isWrapable: Boolean
        get() = when (this) {
            OBSTACLE, VOID, WRAPPED -> false
            else -> true
        }

    companion object {
        val OBSTACLE = Cell('O'.toByte())
        val VOID = Cell('V'.toByte())
        val WRAPPED = Cell('W'.toByte())
        val FREE = Cell(' '.toByte())
    }
}

