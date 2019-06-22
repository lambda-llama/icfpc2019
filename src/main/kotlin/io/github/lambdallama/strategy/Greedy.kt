package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

interface Greedy : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid
        state.robot.wrap(grid)
        while (true) {
            check(grid[state.robot.position] == Cell.WRAPPED)
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
            state.robot.move(grid, v)
            if (state.robot.boosters[BoosterType.B]!! > 0) {
                sink(Attach(state.robot.extendReach()))
            }
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

            for (move in candidates(u, backtrack)) {
                val v = move(u)
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
        return arrayOf(move) + MOVES.filterNot { it == move }
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
            state.robot.move(grid, v)
            state.robot.wrap(grid)
        }
    }
}
