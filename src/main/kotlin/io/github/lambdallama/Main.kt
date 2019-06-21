package io.github.lambdallama

import com.google.common.collect.ArrayListMultimap
import io.github.lambdallama.ui.*
import io.github.lambdallama.ui.Map
import java.io.File
import kotlin.math.max
import kotlin.math.min

// TODO(superbobry): make these inline classes.
inline class Cell(val byte: Byte) {
    /** It this cell inside an obstacle? */
    inline val isObstacle: Boolean get() = this == OBSTACLE
    /** Has the cell been wrapped by Wrappy? */
    inline val isWrapped: Boolean get() = this == WRAPPED
    /** Is it out of bounds of the map? */
    inline val isVoid: Boolean get() = this == VOID
    /** Is it within bounds of the map and could be wrapped? */
    inline val isFree: Boolean get() = this == FREE

    companion object {
        val OBSTACLE = Cell('O'.toByte())
        val WRAPPED = Cell('W'.toByte())
        val FREE = Cell(' '.toByte())
        val VOID = Cell('V'.toByte())

        val B_EXTENSION: Cell get() = Cell('B'.toByte())
        val B_FAST_WHEELS: Cell get() = Cell('F'.toByte())
        val B_DRILL: Cell get() = Cell('L'.toByte())
        val B_MYSTERIOUS_POINT: Cell get() = Cell('X'.toByte())
    }
}

inline class Point(val xy: Long) {
    inline val x: Int get() = xy.toInt()
    inline val y: Int get() = (xy shr 32).toInt()

    operator fun component1(): Int = x
    operator fun component2(): Int = y

    companion object {
        operator fun invoke(x: Int, y: Int) = Point(x.toLong() or (y.toLong() shl Integer.SIZE))

        fun parse(s: String): Point {
            check(s.first() == '(' && s.last() == ')')
            val (x, y) = s.slice(1 until s.length - 1).split(',', limit = 2)
            return Point(x.toInt(), y.toInt())
        }
    }
}

data class Poly(val contour: List<Point>) {
    /** Bottom-left and top-right corners of the bounding box. */
    val bbox: Pair<Point, Point>
        get() {
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = 0
            var maxY = 0
            for ((x, y) in contour) {
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
            }

            return Point(minX, minY) to Point(maxX, maxY)
        }

    operator fun contains(p: Point): Boolean {
        var count = 0
        for (i in 0 until contour.size) {
            val a = contour[i]
            val b = contour[(i + 1) % contour.size]
            if (a.x == b.x) {  // vertical.
                if (p.x < a.x && p.y in a.y..b.y) {
                    count++
                }
            } else {  // horizontal.
                check(a.y == b.y)
            }
        }

        return count % 2 == 0
    }

    companion object {
        fun parse(s: String): Poly {
            val contour = mutableListOf<Point>()
            var offset = 0
            while (offset < s.length) {
                val i = s.indexOf('(', offset)
                val j = s.indexOf(')', i + 1)
                contour.add(Point.parse(s.slice(i..j)))
                offset = j + 1
                check(offset == s.length || s[offset] == ',')
                offset += 1
            }
            return Poly(contour)
        }
    }
}

data class Vertical(val x: Int, val ay: Int, val by: Int) {
}

fun List<Poly>.project(buf: ByteMatrix, value: Cell) {
    if (isEmpty()) {
        return
    }

    val verticals = ArrayListMultimap.create<Int, Vertical>()
    for (poly in this) {
        val contour = poly.contour
        for (i in 0 until contour.size) {
            val a = contour[i]
            val b = contour[(i + 1) % contour.size]
            if (a.x == b.x) {
                verticals.put(a.x, Vertical(a.x, min(a.y, b.y), max(a.y, b.y)))
            }
        }
    }
    check(!verticals.isEmpty)

    val (mx, my) = buf.dim
    for (y in 0 until my) {
        var count = 0
        for (x in 0 until mx) {
            if (verticals.containsKey(x)) {
                for (v in verticals[x]) {
                    // ay <= y + 1/2 <= by => 2 ay <= 2 y + 1 <= 2 by
                    if (2 * y + 1 in 2*v.ay..2*v.by) {
                        count++
                    }
                }
            }

            if (count % 2 > 0) {
                buf[Point(x, y)] = value
            }
        }
    }
}

enum class BoosterType {
    B, F, L, X;

    fun toCell(): Cell = when (this) {
        B -> Cell.B_EXTENSION
        F -> Cell.B_FAST_WHEELS
        L -> Cell.B_DRILL
        X -> Cell.B_MYSTERIOUS_POINT
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
    val task = Task.parse(File("part-1-initial/prob-003.desc").readText())
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
