package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

// Copied over from Greedy.kt
private inline fun ByteMatrix.bfs(
        initial: Point,
        shouldStop: (Point) -> Boolean): List<Point> {
    val backtrack = mutableMapOf<Point, Point?>(initial to null)
    val q = ArrayDeque<Point>()
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

object Weighted: Strategy {
    private var weights: ByteMatrix = ByteMatrix(0, 0, Cell.VOID)
    private var wrapableCellsLeft: Int = 0

    override fun run(state: State, sink: ActionSink) {
        val execute = { a: Action ->
            sink(listOf(a))
            wrapableCellsLeft -= state.apply(state.robot, a).wrappedPoints.count()
        }

        precomputeWeights(state)

        var pathToNextFreeCell = mutableListOf<Action>()
        while (wrapableCellsLeft > 0) {
            if (state.hasBooster(BoosterType.B)) {
                execute(Attach(state.robot.attachmentPoint()))
                continue
            }

            if (pathToNextFreeCell.isNotEmpty()) {
                execute(pathToNextFreeCell.removeLast())
                continue
            }

            val bestPath = getBestWeightedPath(state)
            if (bestPath.isNotEmpty()) {
                execute(bestPath[0])
            } else {
                pathToNextFreeCell = state.grid
                        .bfs(state.robot.position) { state.grid[it].isWrapable }
                        .drop(1)
                        .fold(Pair(state.robot.position, mutableListOf<Action>()), { (prev, list), curr ->
                            list.add(MOVES.first { it(prev) == curr })
                            Pair(curr, list)
                        }).second
                pathToNextFreeCell.reverse()
            }
        }
    }

    private fun getBestWeightedPath(state: State): List<Action> {
        val depth = 3

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

            val score = possiblePath.sumBy { getActionWeight(it) }

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
        return action.wrappedPoints
                .map { it.key }
                .sumBy { weights[it].byte.toInt() }
    }

    private fun precomputeWeights(state: State) {
        weights = ByteMatrix(state.grid.dim.y, state.grid.dim.x, Cell(0))

        for (x in 0 until weights.dim.x) {
            for (y in 0 until weights.dim.y) {
                val u = Point(x, y)
                if (state.grid[u].isWrapable) {
                    wrapableCellsLeft++
                    if (state.grid.freeNeighbours(u).count() < 4) {
                        weights[u] = Cell(100)
                    }
                }
            }
        }

        for (i in 1 until 10) {
            val nextLevelWeights = mutableMapOf<Point, Cell>()
            for (x in 0 until weights.dim.x) {
                for (y in 0 until weights.dim.y) {
                    val u = Point(x, y)
                    if (weights[u] == Cell(0) && state.grid[u].isWrapable) {
                        nextLevelWeights[u] = Cell((weights[weights.neighbours(u).maxBy { weights[it].byte }!!].byte * 0.9).toByte())
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
                    weights[u] = Cell(10)
                }
            }
        }
    }
}

fun ByteMatrix.neighbours(u: Point) = Move.ALL.map { it(u) }.filter { it in this }
