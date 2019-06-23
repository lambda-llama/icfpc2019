package io.github.lambdallama

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

class ReversibleAction(val action: Action) {
    var pickedUpBooster: BoosterType? = null
    var pickedUpBoosterPosition: Point = Point(0, 0)
    var wrappedPoints: Map<Point, Cell> = mapOf()
    var robotFuelLeft: Int = 0
    var madeTwoMoves: Boolean = false
    var teleportedFrom: Point = Point(0, 0)
}

private const val FUEL_INITIAL_VALUE = 50

data class State(
    val grid: ByteMatrix,
    val boosters: MutableMap<Point, BoosterType>,
    val robots: MutableList<Robot>,
    val teleports: MutableList<Point>,
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
        teleports.toMutableList(),
        collectedBoosters.toMutableMap())

    fun fakeClone(idx: Int) = State(
        grid.clone(),
        boosters,  // SHARE.
        mutableListOf(robots[idx].clone()),
        teleports,  // SHARE.
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
                teleports = mutableListOf(),
                robots = mutableListOf(Robot(initialLoc))
            ).apply { wrap() }
        }
    }

    val hasWrappableCells get(): Boolean = grid.count(Cell.FREE) > 0

    fun apply(actions: List<Action>): List<ReversibleAction> {
        require(actions.size == robots.size)
        return robots.zip(actions).map { (robot, action) ->
            apply(robot, action)
        }
    }

    fun apply(robot: Robot, action: Action): ReversibleAction {
        val reverseAction = ReversibleAction(action)
        val boosterType = boosters.remove(robot.position)
        if (boosterType != null) {
            if (boosterType == BoosterType.X) {
                // put spawning point back
                boosters += robot.position to boosterType
            } else {
                reverseAction.pickedUpBooster = boosterType
                reverseAction.pickedUpBoosterPosition = robot.position
                collectedBoosters[boosterType] = collectedBoosters[boosterType]!! + 1
            }
        }

        reverseAction.robotFuelLeft = robot.fuelLeft

        when (action) {
            is Move -> {
                robot.position = robot.position.apply(action)
                check(!grid[robot.position].isObstacle)
                reverseAction.wrappedPoints = wrap()
                if (robot.fuelLeft > 0) {
                    val newPosition = robot.position.apply(action)
                    if (newPosition in grid && !grid[newPosition].isObstacle) {
                        robot.position = newPosition
                        reverseAction.wrappedPoints += wrap()
                        reverseAction.madeTwoMoves = true
                    }
                }
            }
            is TurnClockwise -> {
                robot.rotate(Rotation.CLOCKWISE)
                reverseAction.wrappedPoints = wrap()
            }
            is TurnCounter -> {
                robot.rotate(Rotation.COUNTERCLOCKWISE)
                reverseAction.wrappedPoints = wrap()
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
                reverseAction.wrappedPoints = wrap()
            }
            is Accelerate -> {
                val n = collectedBoosters[BoosterType.F]!!
                check(n > 0)
                collectedBoosters[BoosterType.F] = n - 1
                robot.fuelLeft = FUEL_INITIAL_VALUE + 1 // accounting for decrement below
            }
            is Clone -> {
                check(boosters[robot.position] == BoosterType.X) {
                    "${boosters[robot.position]} ${robot.position} $boosters"
                }
                val n = collectedBoosters[BoosterType.C]!!
                check(n > 0)
                collectedBoosters[BoosterType.C] = n - 1
                robots += Robot(robot.position)
                reverseAction.wrappedPoints = wrap()
            }
            is InstallBeacon -> {
                check(boosters[robot.position] == BoosterType.X) {
                    "${boosters[robot.position]} ${robot.position} $boosters"
                }
                val n = collectedBoosters[BoosterType.R]!!
                check(n > 0)
                collectedBoosters[BoosterType.R] = n - 1
                teleports += robot.position
            }
            is Teleport -> {
                check(action.location in teleports)
                reverseAction.teleportedFrom = robot.position
                robot.position = action.location
                reverseAction.wrappedPoints = wrap()
            }
            is NoOp -> Unit
            else -> TODO("Not supported yet")
        }

        robot.fuelLeft = max(0, robot.fuelLeft - 1)

        return reverseAction
    }

    fun unapply(reverseActions: List<ReversibleAction>): List<Action> {
        return robots.zip(reverseActions).reversed().map { (robot, action) ->
            unapply(robot, action)
        }.reversed()
    }

    fun unapply(robot: Robot, reverseAction: ReversibleAction): Action {
        val boosterType = reverseAction.pickedUpBooster
        if (boosterType != null) {
            boosters[reverseAction.pickedUpBoosterPosition] = boosterType
            collectedBoosters[boosterType] = collectedBoosters[boosterType]!! - 1
        }
        when (reverseAction.action) {
            is Move -> {
                robot.position = robot.position.unapply(reverseAction.action)
                if (reverseAction.madeTwoMoves) {
                    robot.position = robot.position.unapply(reverseAction.action)
                }
                unwrap(reverseAction.wrappedPoints)
            }
            is TurnClockwise -> {
                robot.rotate(Rotation.COUNTERCLOCKWISE)
                unwrap(reverseAction.wrappedPoints)
            }
            is TurnCounter -> {
                robot.rotate(Rotation.CLOCKWISE)
                unwrap(reverseAction.wrappedPoints)
            }
            is Attach -> {
                collectedBoosters[BoosterType.B] = collectedBoosters[BoosterType.B]!! + 1
                robot.detachLastTentacle()
                unwrap(reverseAction.wrappedPoints)
            }
            is Accelerate -> {
                collectedBoosters[BoosterType.F] = collectedBoosters[BoosterType.F]!! + 1
            }
            is Clone -> {
                collectedBoosters[BoosterType.C] = collectedBoosters[BoosterType.C]!! + 1
                robots.removeAt(robots.count() - 1)
                unwrap(reverseAction.wrappedPoints)
            }
            is InstallBeacon -> {
                collectedBoosters[BoosterType.R] = collectedBoosters[BoosterType.R]!! + 1
                teleports.remove(robot.position)
            }
            is Teleport -> {
                robot.position = reverseAction.teleportedFrom
                unwrap(reverseAction.wrappedPoints)
            }
            is NoOp -> Unit
            else -> TODO("Not supported yet")
        }
        robot.fuelLeft = reverseAction.robotFuelLeft
        return reverseAction.action
    }

    fun hasBooster(boosterType: BoosterType): Boolean = nBoosters(boosterType) > 0

    fun nBoosters(boosterType: BoosterType): Int {
        return collectedBoosters[boosterType]!!
    }

    private fun wrap(): MutableMap<Point, Cell> {
        val wrappedPoints = mutableMapOf<Point, Cell>()
        robots.forEach { robot ->
            check(!grid[robot.position].isObstacle)
            robot.getVisibleParts(grid).forEach { p ->
                if (grid[p].isWrapable) {
                    wrappedPoints[p] = grid[p]
                    grid[p] = Cell.WRAPPED
                }
            }
        }
        return wrappedPoints
    }

    private fun unwrap(wrappedPoints: Map<Point, Cell>) {
        wrappedPoints.forEach { (point, cell) ->
            grid[point] = cell
        }
    }

    fun canApply(robot: Robot, action: Action): Boolean {
        return when (action) {
            is Move -> {
                val newPosition = robot.position.apply(action)
                newPosition in grid && !grid[newPosition].isObstacle
            }
            is TurnClockwise -> {
                true
            }
            is TurnCounter -> {
                true
            }
            is Attach -> {
                collectedBoosters[BoosterType.B]!! > 0
                        && robot.tentacles.map { it.rotate(robot.orientation) }
                                .map { it.manhattanDist(action.location) }.min()!! == 1
            }
            is Accelerate -> {
                collectedBoosters[BoosterType.F]!! > 0
            }
            is Clone -> {
                collectedBoosters[BoosterType.C]!! > 0
                        && boosters[robot.position] == BoosterType.X
            }
            is NoOp -> true
            else -> TODO("Not supported yet")
        }
    }
}

fun Point.apply(move: Move) = Point(x + move.dx, y + move.dy)
fun Point.unapply(move: Move) = Point(x - move.dx, y - move.dy)

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

