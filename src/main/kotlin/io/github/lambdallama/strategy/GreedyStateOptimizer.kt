package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

typealias StateFunction = (State, Robot) -> Double
typealias ActionPolicy = (State, Robot) -> List<Action>

class GreedyStateOptimizer(
    val stateFunction: StateFunction,
    val actionPolicy: ActionPolicy
) : Strategy {

    override fun run(state: State, sink: ActionSink) {
        ClonePhase.run(state, sink)

        var isTerminal = false
        while (!isTerminal) {
            isTerminal = true
            val robotMoves = mutableListOf<Action>()
            var robotIndex = 0
            while (robotIndex < state.robots.size) {
                val robot = state.robots[robotIndex]
                val moves = actionPolicy(state, robot)
                val best = moves
                    .map { move ->
                        val unmove = state.apply(robot, move)
                        val q = stateFunction(state, robot)
                        state.unapply(robot, unmove)
                        q to move
                    }.maxBy { it.first }
                if (best == null) {
                    robotMoves.add(NoOp)
                } else {
                    val bestMove = best.second
                    robotMoves.add(bestMove)
                    isTerminal = false
                    state.apply(robot, bestMove)
                }
                robotIndex += 1
            }
            if (!isTerminal) {
                sink(robotMoves)
            }
        }
    }
}

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)
private val TURNS = arrayOf(TurnClockwise, TurnCounter)

fun allAllowedMovesPolicy(state: State, robot: Robot): List<Action> {
    if (!state.hasWrappableCells) {
        return listOf()
    }
    return MOVES.filter { move ->
        val nextPostion = move(robot.position)
        nextPostion in state.grid && !state.grid[nextPostion].isObstacle
    }
}

fun moveAndTurn(state: State, robot: Robot): List<Action> {
    if (state.hasBooster(BoosterType.B)) {
        return listOf(Attach(robot.attachmentPoint()))
    }
    var moves = allAllowedMovesPolicy(state, robot)
    if (moves.isEmpty()) {
        return moves
    }
    moves += TURNS
    return moves
}

fun wrappedRatio(state: State): Double {
    val wrapped = state.grid.count(Cell.WRAPPED)
    val free = state.grid.count(Cell.FREE)
    return wrapped / (wrapped + free).toDouble()
}

typealias PointPredicate = (State, Point) -> Boolean

fun distance(state: State, from: Robot, targetPointPredicate: PointPredicate): Int? {
    val grid = state.grid
    val d = mutableMapOf<Point, Int>()
    val q = ArrayDeque<Point>()
    d[from.position] = 0
    q.addLast(from.position)
    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        if (targetPointPredicate(state, u)) {
            return d[u]!!
        }
        for (move in MOVES) {
            val v = move(u)
            if (v in grid && !grid[v].isObstacle) {
                var du = d[u]!! + 1
                if (d.getOrDefault(v, Int.MAX_VALUE) > du + 1) {
                    d[v] = du + 1
                    q.addLast(v)
                }
            }
        }
    }
    return null
}

fun isWrapable(state: State, p: Point): Boolean {
    return state.grid[p].isWrapable
}

fun hasTentacleBonus(state: State, p: Point): Boolean {
    return state.boosters[p] == BoosterType.B
}

val WrapDistanceCount = GreedyStateOptimizer(
    stateFunction = { state: State, robot: Robot ->
        val dw = distance(
            state, from = robot,
            targetPointPredicate = ::isWrapable
        )?.toDouble() ?: 0.0
        val nDw = dw / (state.grid.dim.x * state.grid.dim.y)

        val dB = distance(
            state, from = robot,
            targetPointPredicate = ::hasTentacleBonus
        )?.toDouble() ?: 0.0
        val nDb = dB / (2 * state.grid.dim.x * state.grid.dim.y)

        state.grid.count(Cell.WRAPPED) - nDw - nDb
    },
    actionPolicy = ::moveAndTurn
)