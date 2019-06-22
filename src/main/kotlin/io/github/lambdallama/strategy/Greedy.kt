package io.github.lambdallama.strategy

import com.google.common.collect.ComparisonChain
import io.github.lambdallama.*
import java.util.*

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)

interface Greedy : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid
        sink(TurnClockwise)
        state.robot.rotate(Rotation.CLOCKWISE)
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

fun Rotation.toMove() = when (this) {
    Rotation.CLOCKWISE -> TurnClockwise
    Rotation.COUNTERCLOCKWISE -> TurnCounter
}

object GreedySMFTurnover: Greedy {
    override fun candidates(u: Point, backtrack: Map<Point, Point?>): Array<Move> {
        val origin = backtrack[u] ?: return MOVES
        val move = MOVES.first { it(origin) == u }
        return arrayOf(move) + MOVES.filterNot { it == move }
    }

    override fun follow(state: State, path: List<Point>, sink: ActionSink) {
        val grid = state.grid

        // Find a starting rotation which maximizes number of wrapped cells per action.
        val rotation = arrayOf(null, Rotation.CLOCKWISE, Rotation.COUNTERCLOCKWISE).maxBy { candidate ->
            var wrapped = 0
            var score = 0
            val clone = state.robot.clone()
            candidate?.let {
                clone.rotate(it)
                score++
            }
            for (i in 1 until path.size) {
                val v = path[i]
                clone.position = v  // .move is mutating!
                score++
                if (grid[v].isBooster) {
                    val type = BoosterType.fromCell(grid[v])
                    clone.boosters[type] = clone.boosters[type]!! + 1
                }
                if (state.robot.boosters[BoosterType.B]!! > 0) {
                    clone.extendReach()
                    score++
                }
                wrapped += clone.getVisibleParts(grid).count { grid[it].isWrapable }
            }

            wrapped / score.toDouble()
        }

        if (rotation != null) {
            sink(rotation.toMove())
            state.robot.rotate(rotation)
        }
        for (i in 1 until path.size) {
            val v = path[i]
            sink(MOVES.first { it(state.robot.position) == v })
            state.robot.move(grid, v)
            if (state.robot.boosters[BoosterType.B]!! > 0) {
                sink(Attach(state.robot.extendReach()))
            }
            state.robot.wrap(grid)
        }
    }
}