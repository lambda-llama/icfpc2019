package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

typealias StateFunction = (State) -> Double
typealias ActionPolicy = (State, Robot) -> List<Action>

class GreedyStateOptimizer(
        val stateFunction: StateFunction,
        val actionPolicy: ActionPolicy
) : Strategy {

    override fun run(state: State, sink: ActionSink) {
        val currentState = state.clone()
        var isTerminal = false
        while (!isTerminal) {
            isTerminal = true
            val robotMoves = mutableListOf<Action>()
            var robotIndex = 0
            while (robotIndex < currentState.robots.size) {
                val robot = currentState.robots[robotIndex]
                val moves = actionPolicy(currentState, robot)
                val bestMove = moves.maxBy { move ->
                    val nextState = currentState.clone()
                    nextState.apply(nextState.robots[robotIndex], move)
                    val q = stateFunction(nextState)
                    q
                }
                robotMoves.add(bestMove ?: NoOp)
                if (bestMove != null) {
                    isTerminal = false
                    currentState.apply(robot, bestMove)
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

fun allAllowedMovesPolicy(state: State, robot: Robot): List<Action> {
    if (!state.hasWrappableCells) {
        return listOf()
    }
    return MOVES.filter { move ->
        val nextPostion = move(robot.position)
        nextPostion in state.grid && !state.grid[nextPostion].isObstacle
    }
}

fun wrappedCount(state: State): Int {
    return state.grid.cellCounts[Cell.WRAPPED] ?: 0
}

fun wrappedRatio(state: State): Double {
    val wrapped = state.grid.cellCounts[Cell.WRAPPED] ?: 0
    val free = state.grid.cellCounts[Cell.FREE] ?: 0
    return wrapped / (wrapped + free).toDouble()
}

fun distanceToWrappable(state: State): Int? {
    val grid = state.grid
    val d = mutableMapOf<Point, Int>()
    val q = ArrayDeque<Point>()
    for (robot in state.robots) {
        d[robot.position] = 0
        q.addLast(robot.position)
    }
    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        if (grid[u].isWrapable) {
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


val WrapDistanceCount = GreedyStateOptimizer(
        stateFunction = { state: State ->
            val dw = distanceToWrappable(state)?.toDouble() ?: 0.0
            val d = dw / (state.grid.dim.x * state.grid.dim.y)
            wrappedCount(state) - d
        },
        actionPolicy = ::allAllowedMovesPolicy
)