package io.github.lambdallama

import java.util.*

interface Strategy {
    fun run(state: State, sink: ActionSink)
}

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

object Naive : Strategy {
    private fun go(grid: ByteMatrix, u: Point, sink: ActionSink) {
        grid[u] = Cell.WRAPPED
        for (move in MOVES) {
            val v = move(u)
            if (v in grid && grid[v].isWrapable) {
                sink(move)
                go(grid, v, sink)
                sink(move.flipped)
            }
        }
    }

    override fun run(state: State, sink: ActionSink) {
        go(state.grid, state.robot.position, sink)
    }
}

object NaiveIterative : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid
        val initial = state.robot.position
        val q = ArrayDeque<Point>()
        q.addFirst(initial)
        while (q.isNotEmpty()) {
            val u = q.first
            grid[u] = Cell.WRAPPED
            val move = MOVES.firstOrNull { move ->
                move(u).let { it in grid && grid[it].isWrapable }
            }

            if (move == null) {
                q.removeFirst()
                if (q.isNotEmpty()) {
                    val v = q.peekFirst()
                    sink(MOVES.first { it(v) == u }.flipped)
                }
            } else {
                sink(move)
                val v = move(u)
                q.addFirst(v)
            }
        }
    }
}

interface Greedy : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid
        state.robot.wrap(grid)
        while (true) {
            check(grid[state.robot.position] == Cell.WRAPPED) {
                grid[state.robot.position]
            }
            val path = closestFree(grid, state.robot.position)
            if (!grid[path.last()].isWrapable) {
                break
            }

            check(path.first() == state.robot.position)
            follow(state, path, sink)
        }
    }

    fun follow(state: State, path: List<Point>, sink: ActionSink) {
        val grid = state.grid
        for (v in path.drop(1)) {
            sink(MOVES.first { it(state.robot.position) == v })
            state.robot.position = v
            state.robot.wrap(grid)
        }
    }

    fun candidates(u: Point, backtrack: Map<Point, Point?>): Array<Move>

    private fun closestFree(grid: ByteMatrix, initial: Point): List<Point> {
        val backtrack = mutableMapOf<Point, Point?>(initial to null)
        val q = ArrayDeque<Point>()
        q.addLast(initial)
        var u: Point? = initial
        while (q.isNotEmpty()) {
            u = q.removeFirst()
            if (grid[u].isWrapable) {
                break
            }

            for (candidate in candidates(u, backtrack)) {
                val v = candidate(u)
                if (v in grid
                    && grid[v] != Cell.OBSTACLE && grid[v] != Cell.VOID
                    && v !in backtrack) {
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
        return path.reversed()
    }
}

object GreedyUnordered: Greedy {
    override fun candidates(u: Point, backtrack: Map<Point, Point?>) = MOVES
}

object GreedySameMoveFirst: Greedy {
    override fun candidates(u: Point, backtrack: Map<Point, Point?>): Array<Move> {
        val origin = backtrack[u] ?: return MOVES
        val move = MOVES.first { it(origin) == u }
        return arrayOf(move) + MOVES
    }
}

object GreedyUnorderedTurnover: Greedy {
    override fun candidates(u: Point, backtrack: Map<Point, Point?>) = MOVES

    override fun follow(state: State, path: List<Point>, sink: ActionSink) {
        val grid = state.grid
        val moves = Array(path.size - 1) { i ->
            MOVES.first { it(path[i]) == path[i + 1] }
        }

        for (i in 0 until moves.size) {
            // If the direction changed and the next two moves are the same, turn.
            if (i < moves.size - 1 && moves[i] == moves[i + 1]) {
            }
        }

        for (v in path.drop(1)) {
            state.robot.position = v
            state.robot.wrap(grid)
        }
    }
}