package io.github.lambdallama

import kotlin.math.*

data class State(
    val grid: ByteMatrix,
    val boosters: MutableMap<Point, BoosterType>,
    val robots: MutableList<Robot>,
    val collectedBoosters: MutableMap<BoosterType, Int> = mutableMapOf(
        BoosterType.B to 0,
        BoosterType.F to 0,
        BoosterType.L to 0,
        BoosterType.R to 0,
        BoosterType.C to 0
    )
) {
    val maxPoints: Int = ceil(1000 * log2((grid.dim.x * grid.dim.y).toDouble())).toInt()

    val robot get(): Robot = robots.first()

    fun clone() = State(
        grid.clone(),
        HashMap(boosters),
        robots.map { it.clone() }.toMutableList(),
        collectedBoosters.toMutableMap())

    fun fakeClone(idx: Int) = State(
        grid.clone(),
        boosters,  // SHARE.
        mutableListOf(robots[idx]),
        collectedBoosters)  // SHARE.

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
                robots = mutableListOf(Robot(initialLoc))
            ).apply { wrap() }
        }
    }

    fun apply(actions: List<Action>) {
        require(actions.size == robots.size)
        for ((robot, action) in robots.zip(actions)) {
            apply(robot, action)
        }
    }

    private fun apply(robot: Robot, action: Action) {
        val boosterType = boosters.remove(robot.position)
        if (boosterType != null) {
            if (boosterType == BoosterType.X) {
                // put spawning point back
                boosters += robot.position to boosterType
            } else {
                collectedBoosters[boosterType] = collectedBoosters[boosterType]!! + 1
            }
        }
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
                val n = collectedBoosters[BoosterType.B]!!
                check(n > 0)
                collectedBoosters[BoosterType.B] = n - 1
                check(
                    robot.tentacles.map { it.rotate(robot.orientation) }
                        .map { it.manhattanDist(action.location) }.min()!! == 1
                )
                robot.attachTentacle(action.location)
                wrap()
            }
            is Clone -> {
                check(boosters[robot.position] == BoosterType.X) {
                    "${boosters[robot.position]} ${robot.position} ${boosters}"
                }
                val n = collectedBoosters[BoosterType.C]!!
                check(n > 0)
                collectedBoosters[BoosterType.C] = n - 1
                robots += Robot(robot.position)
                wrap()
            }
            is NoOp -> Unit
        }
    }

    fun hasBooster(boosterType: BoosterType): Boolean = nBoosters(boosterType) > 0

    fun nBoosters(boosterType: BoosterType): Int {
        return collectedBoosters[boosterType]!!
    }

    private fun wrap() {
        robots.forEach { robot ->
            check(!grid[robot.position].isObstacle)
            robot.getVisibleParts(grid).forEach { p ->
                if (grid[p].isWrapable) {
                    grid[p] = Cell.WRAPPED
                }
            }
        }
    }
}

fun Point.apply(move: Move) = Point(x + move.dx, y + move.dy)


enum class BoosterType {
    /**
     * Attach tentacle
     */
    B,
    F, L,
    /**
     * Spawn point
     */
    X,
    R,
    /**
     * Clone
     */
    C;
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

