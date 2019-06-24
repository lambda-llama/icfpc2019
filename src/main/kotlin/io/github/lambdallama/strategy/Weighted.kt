package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

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
private const val DEFAULT_CELL_WEIGHT: Byte = 1
private const val BORDER_CELL_INITIAL_WEIGHT: Byte = 25
private const val CELL_WEIGHT_DECAY = 0.4
private const val PREPROCESS_PASS_COUNT = 10

open class WeightedBase(private val enableAcceleration: Boolean): Strategy {
    private var weights: ByteMatrix = ByteMatrix(0, 0, Cell.VOID)
    private var wrapableCellsLeft: Int = 0

    override fun run(state: State, sink: ActionSink) {
        (DEPTH_MIN until DEPTH_MAX + 1).map { depth ->
            val actions = mutableListOf<Action>()
            val execute = { s: State, a: Action ->
                actions.addAll(listOf(a))
                wrapableCellsLeft -= s.apply(s.robot, a).wrappedPoints.count()
            }
            runWithDepth(depth, state.clone(), execute)
            actions
        }.minBy { it.size }!!
            .forEach { sink(listOf(it)) }
    }

    private fun runWithDepth(depth: Int, state: State, execute: (State, Action) -> Unit) {
        precomputeWeights(state)

        var pathToNextFreeCell = mutableListOf<Move>()
        while (wrapableCellsLeft > 0) {
            if (state.hasBooster(BoosterType.B)) {
                execute(state, Attach(state.robot.attachmentPoint()))
                continue
            }

            if (enableAcceleration && state.hasBooster(BoosterType.F)) {
                execute(state, Accelerate)
                continue
            }

            if (pathToNextFreeCell.isNotEmpty()) {
                if (state.robot.fuelLeft > 0) {
                    val next = pathToNextFreeCell.last()
                    // If we want to move twice - do it
                    if (pathToNextFreeCell.size >= 2 && next == pathToNextFreeCell[pathToNextFreeCell.size - 2]) {
                        execute(state, pathToNextFreeCell.removeLast())
                        pathToNextFreeCell.removeLast()
                        continue
                    }
                    // If we want to move once and we know we'll hit wall - do it
                    val newPosition = next.invoke(next.invoke(state.robot.position))
                    if (newPosition in state.grid && state.grid[newPosition].isObstacle) {
                        execute(state, pathToNextFreeCell.removeLast())
                        continue
                    }
                    // TODO: take acceleration into account when building path?
                    // For now, just burn fuel
                    while (state.robot.fuelLeft > 0) {
                        execute(state, NoOp)
                    }
                }
                execute(state, pathToNextFreeCell.removeLast())
                continue
            }

            val bestPath = getBestWeightedPath(state, depth)
            if (bestPath.isNotEmpty()) {
                execute(state, bestPath[0])
            } else {
                pathToNextFreeCell = state.grid
                        .bfs(state.robot.position) { state.grid[it].isWrapable }
                        .drop(1)
                        .fold(Pair(state.robot.position, mutableListOf<Move>()), { (prev, list), curr ->
                            list.add(MOVES.first { it(prev) == curr })
                            Pair(curr, list)
                        }).second
                pathToNextFreeCell.reverse()
            }
        }
    }

    private fun getBestWeightedPath(state: State, depth: Int): List<Action> {
        val moves = mutableListOf<Pair<Int, Action>>()
        ACTIONS.forEach { m -> moves.add(Pair(1, m)) }
        val possiblePath = mutableListOf<ReversibleAction>()
        var maxScore = 0
        var bestPath = listOf<ReversibleAction>()
        while (moves.isNotEmpty()) {
            val (level, action) = moves.removeLast()

            while (possiblePath.count() >= level) {
                state.unapply(state.robot, possiblePath.removeLast())
            }

            if (!state.canApply(state.robot, action)) {
                continue
            }
            possiblePath.add(state.apply(state.robot, action))
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
            state.unapply(state.robot, possiblePath.removeLast())
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
                    wrapableCellsLeft++
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
                        nextLevelWeights[u] = Cell((weights[weights.neighbours(u).maxBy { weights[it].byte }!!].byte * CELL_WEIGHT_DECAY).toByte())
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
