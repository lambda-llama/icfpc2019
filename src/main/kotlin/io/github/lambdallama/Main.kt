package io.github.lambdallama

import io.github.lambdallama.ui.*
import io.github.lambdallama.ui.Map
import java.io.File

// TODO(superbobry): make these inline classes.
inline class Cell(val byte: Byte) {
    companion object {
        val OBSTACLE = Cell('O'.toByte())
        val WRAPPED = Cell('W'.toByte())
        val FREE = Cell(' '.toByte())
        val VOID = Cell('V'.toByte())

        val B_EXTENSION: Cell get() = Cell('B'.toByte())
        val B_FAST_WHEELS: Cell get() = Cell('F'.toByte())
        val B_DRILL: Cell get() = Cell('L'.toByte())
        val B_MYSTERIOUS_POINT: Cell get() = Cell('X'.toByte())
        val B_TELEPORT: Cell get() = Cell('T'.toByte())
    }
}

enum class BoosterType {
    B, F, L, X, T;

    fun toCell(): Cell = when (this) {
        B -> Cell.B_EXTENSION
        F -> Cell.B_FAST_WHEELS
        L -> Cell.B_DRILL
        X -> Cell.B_MYSTERIOUS_POINT
        T -> Cell.B_TELEPORT
    }
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


data class State(
    val grid: ByteMatrix,
    val wrappy: List<Point>
)

data class Task(
    val map: Poly,
    val initialLoc: Point,
    val obstacles: List<Poly>,
    val boosters: List<Booster>
) {
    fun toState(): State {
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
        for (booster in boosters) {
            grid[booster.loc] = booster.type.toCell()
        }

        val (initialX, initialY) = initialLoc
        // TODO(superbobry): filter invalid points.
        val wrappy = listOf(
            Point(initialX + 1, initialY),
            Point(initialX + 1, initialY + 1),
            Point(initialX + 1, initialY - 1))
        return State(grid, wrappy)
    }

    companion object {
        fun parse(s: String): Task {
            val (rawMap, rawInitial, rawObstacles, rawBoosters) = s.split('#')
            val obstacles = rawObstacles.splitToSequence(';')
                .filter { it.isNotEmpty() }
                .map { Poly.parse(it) }
                .toList()
            val boosters = rawBoosters.splitToSequence(';')
                .filter { it.isNotEmpty() }
                .map { Booster.parse(it) }
                .toList()
            return Task(
                map = Poly.parse(rawMap),
                initialLoc = Point.parse(rawInitial),
                obstacles = obstacles,
                boosters = boosters)
        }
    }
}

val DX = arrayOf(-1, 0, 1, 0)
val DY = arrayOf(0, -1, 0, 1)
val DIR_NAMES = "AWDS"

class Naive(var state: State) {
    private lateinit var board: ByteMatrix
    private lateinit var path: String
    fun go(u: Point) {
        board[u] = Cell.WRAPPED
        for (i in 0..3) {
            val v = Point(u.x + DX[i], u.y + DY[i])
            println(v)
            if (board.contains(v) && board[v] == Cell.FREE) {
                path += DIR_NAMES[i]
                go(v)
                path += DIR_NAMES[i xor 2]
            }
        }
    }

    fun traverse(start: Point): String {
        board = state.grid
        path = ""
        go(start)
        return path
    }
}

fun main(args: Array<String>) {
    val task = Task.parse(File("part-1-initial/prob-002.desc").readText())
    val state = task.toState()

    launchGui()
    val pills: MutableList<Pair<Point, Pill>> = mutableListOf()
    draw(Map(state.grid.dim,
        { p ->
            val c = state.grid[p]
            if (state.wrappy.contains(p)) {
                pills += p to Pill.ROBOT
            }
            when (c) {
                Cell.OBSTACLE -> UiCell.WALL
                Cell.OBSTACLE -> UiCell.WALL
                Cell.FREE -> UiCell.FREE
                Cell.WRAPPED -> UiCell.WRAPPED
                Cell.VOID -> UiCell.VOID
                else -> {
                    when (c) {
                        Cell.B_EXTENSION -> pills += p to Pill.BOOST_B
                        Cell.B_DRILL -> pills += p to Pill.BOOST_L
                        Cell.B_FAST_WHEELS -> pills += p to Pill.BOOST_F
                        Cell.B_MYSTERIOUS_POINT-> pills += p to Pill.BOOST_X
                    }
                    UiCell.FREE
                }
            }

        }, pills))
    for (i in 0 until 100) {
        Thread.sleep(300)
    }

    println(state.grid)
}
