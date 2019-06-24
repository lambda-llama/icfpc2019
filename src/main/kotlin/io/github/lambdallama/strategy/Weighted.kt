package io.github.lambdallama.strategy

import io.github.lambdallama.*

// Copied over from Greedy.kt
private inline fun ByteMatrix.bfs(
        initial: Point,
        shouldStop: (Point) -> Boolean): List<Point> {
    val backtrack = mutableMapOf<Point, Point?>(initial to null)
    val q = PointDeque()
    q.addLast(initial)
    var u: Point? = initial
    while (q.isNotEmpty()) {
        u = q.removeFirst()
        if (shouldStop(u)) {
            break
        }

        for (move in MOVES) {
            val v = move(u)
            if (v in this && !this[v].isObstacle && v !in backtrack) {
                q.addLast(v)
                backtrack[v] = u
            }
        }
    }

    val path = mutableListOf<Point>()
    while (u != null && u in backtrack) {
        path.add(u)
        u = backtrack[u]
    }
    path.reverse()
    return if (path.isNotEmpty() && shouldStop(path.last())) {
        check(path.first() == initial)
        path
    } else {
        emptyList()
    }
}

private val ACTIONS = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight, TurnClockwise, TurnCounter)
private val MOVES = ACTIONS.filterIsInstance<Move>()

private const val DEPTH_MIN = 3
private const val DEPTH_MAX = 5
private const val EXTENDER_PICKUP_SCORE = 1000
private const val ACCELERATION_PICKUP_SCORE = 500
private const val DEFAULT_CELL_WEIGHT: Byte = 5
private const val BORDER_CELL_INITIAL_WEIGHT: Byte = 25
private const val CELL_WEIGHT_DECAY = 0.35
private const val PREPROCESS_PASS_COUNT = 20

// Set to true for better visualizer support
private const val LIVE = false

open class WeightedBase(private val enableAcceleration: Boolean): Strategy {
    private var weights: ByteMatrix = ByteMatrix(0, 0, Cell.VOID)

    override fun run(state: State, sink: ActionSink) {
        if (!LIVE) {
            (DEPTH_MIN until DEPTH_MAX + 1).map { depth ->
                val allActions = mutableListOf<List<Action>>()
                runWithDepth(depth, state.clone()) { actions ->
                    allActions.add(actions)
                }
                allActions
            }.minBy { it.size }!!
                    .forEach { sink(it) }
        } else {
            runWithDepth(DEPTH_MIN, state, sink)
        }
    }

    fun runWithDepth(depth: Int, state: State, sink: ActionSink) {
        precomputeWeights(state)

        val pathsToNextFreeCell = state.robots.map {
            Pair(it.id, mutableListOf<Move>())
        }.toMap()
        while (state.hasWrappableCells) {
            val actions = mutableListOf<Action>()
            for (robot in state.robots) {
                val action = getNextMove(state, robot, pathsToNextFreeCell.getValue(robot.id), depth)
                state.apply(robot, action)
                actions.add(action)
            }
            sink(actions)
        }
    }

    private fun getNextMove(state: State, robot: Robot, pathToNextFreeCell: MutableList<Move>, depth: Int): Action {
        if (!state.hasWrappableCells) {
            return NoOp
        }

        if (state.hasBooster(BoosterType.B)) {
            return Attach(robot.attachmentPoint())
        }

        if (enableAcceleration && state.hasBooster(BoosterType.F)) {
            return Accelerate
        }

        if (pathToNextFreeCell.isNotEmpty()) {
            if (robot.fuelLeft > 0) {
                val next = pathToNextFreeCell.last()
                // If we want to move twice - do it
                if (pathToNextFreeCell.size >= 2 && next == pathToNextFreeCell[pathToNextFreeCell.size - 2]) {
                    pathToNextFreeCell.removeLast()
                    return pathToNextFreeCell.removeLast()
                }
                // If we want to move once and we know we'll hit wall - do it
                val newPosition = next.invoke(next.invoke(robot.position))
                if (newPosition in state.grid && state.grid[newPosition].isObstacle) {
                    return pathToNextFreeCell.removeLast()
                }
                // TODO: take acceleration into account when building path?
                // For now, just burn fuel
                return NoOp
            }
            return pathToNextFreeCell.removeLast()
        }

        val bestPath = getBestWeightedPath(state, robot, depth)
        if (bestPath.isNotEmpty()) {
            return bestPath[0]
        }

        pathToNextFreeCell.addAll(state.grid
                .bfs(robot.position) { state.grid[it].isWrapable }
                .drop(1)
                .fold(Pair(robot.position, mutableListOf<Move>()), { (prev, list), curr ->
                    list.add(MOVES.first { it(prev) == curr })
                    Pair(curr, list)
                }).second)
        pathToNextFreeCell.reverse()
        // This won't go deeper
        return getNextMove(state, robot, pathToNextFreeCell, depth)
    }

    private fun getBestWeightedPath(state: State, robot: Robot, depth: Int): List<Action> {
        val moves = mutableListOf<Pair<Int, Action>>()
        ACTIONS.forEach { m -> moves.add(Pair(1, m)) }
        val possiblePath = mutableListOf<ReversibleAction>()
        var maxScore = 0
        var bestPath = listOf<ReversibleAction>()
        while (moves.isNotEmpty()) {
            val (level, action) = moves.removeLast()

            while (possiblePath.count() >= level) {
                state.unapply(robot, possiblePath.removeLast())
            }

            if (!state.canApply(robot, action)) {
                continue
            }
            possiblePath.add(state.apply(robot, action))
            if (level < depth) {
                ACTIONS.forEach { m -> moves.add(Pair(level + 1, m)) }
                continue
            }

            val score = possiblePath.sumBy {
                getActionWeight(it)
            }

            if (score > maxScore) {
                maxScore = score
                bestPath = possiblePath.toList()
            } else if (score > 0 && score == maxScore) {
                for (i in 0 until possiblePath.size) {
                    val wBest = getActionWeight(bestPath[i])
                    val wPossible = getActionWeight(possiblePath[i])
                    if (wBest > wPossible) {
                        break
                    } else if (wBest < wPossible) {
                        bestPath = possiblePath.toList()
                        break
                    }
                }
            }
        }

        while (possiblePath.isNotEmpty()) {
            state.unapply(robot, possiblePath.removeLast())
        }
        return bestPath.map { it.action }
    }

    private fun getActionWeight(action: ReversibleAction): Int {
        var score = action.wrappedPoints
                .map { it.key }
                .sumBy { weights[it].byte.toInt() }
        if (action.pickedUpBooster == BoosterType.B) {
            score += EXTENDER_PICKUP_SCORE
        }
        if (enableAcceleration && action.pickedUpBooster == BoosterType.F) {
            score += ACCELERATION_PICKUP_SCORE
        }
        return score
    }

    private fun precomputeWeights(state: State) {
        weights = ByteMatrix(state.grid.dim.y, state.grid.dim.x, Cell(0))

        for (x in 0 until weights.dim.x) {
            for (y in 0 until weights.dim.y) {
                val u = Point(x, y)
                if (state.grid[u].isWrapable) {
                    if (state.grid.freeNeighbours(u).count() < 4) {
                        val bordersCount = 4 - state.grid.freeNeighbours(u).count()
                        weights[u] = Cell((BORDER_CELL_INITIAL_WEIGHT * bordersCount).toByte())
                    }
                }
            }
        }

        for (i in 1 until PREPROCESS_PASS_COUNT) {
            val nextLevelWeights = mutableMapOf<Point, Cell>()
            for (x in 0 until weights.dim.x) {
                for (y in 0 until weights.dim.y) {
                    val u = Point(x, y)
                    if (weights[u] == Cell(0) && state.grid[u].isWrapable) {
                        nextLevelWeights[u] = Cell((weights.neighbours(u).sumBy { weights[it].byte.toInt() } * CELL_WEIGHT_DECAY).toByte())
                    }
                }
            }
            nextLevelWeights.forEach { (p, w) ->
                weights[p] = w
            }
        }

        for (x in 0 until weights.dim.x) {
            for (y in 0 until weights.dim.y) {
                val u = Point(x, y)
                if (weights[u] == Cell(0) && state.grid[u].isWrapable) {
                    weights[u] = Cell(DEFAULT_CELL_WEIGHT)
                }
            }
        }
    }
}

object Weighted: WeightedBase(enableAcceleration = false)
object WeightedAccelerated: WeightedBase(enableAcceleration = true)

fun ByteMatrix.neighbours(u: Point) = Move.ALL.map { it(u) }.filter { it in this }
