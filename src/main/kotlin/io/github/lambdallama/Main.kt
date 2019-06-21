package io.github.lambdallama

import io.github.lambdallama.ui.*
import io.github.lambdallama.ui.Map
import java.io.File
import java.lang.StringBuilder
import kotlin.math.max
import kotlin.math.min

// TODO(superbobry): make these inline classes.
const val OBSTACLE = 'O'.toByte()
const val WRAPPED = 'W'.toByte()
const val FREE = ' '.toByte()
const val VOID = 'V'.toByte()

data class Point(val x: Int, val y: Int) {
    companion object {
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

enum class BoosterType {
    B, F, L, X;

    fun toByte(): Byte = when (this) {
        B -> 'B'.toByte()
        F -> 'F'.toByte()
        L -> 'L'.toByte()
        X -> 'X'.toByte()
    }
}

inline val Byte.isExtension: Boolean get() = this == BoosterType.B.toByte()
inline val Byte.isFastWheels: Boolean get() = this == BoosterType.F.toByte()
inline val Byte.isDrill: Boolean get() = this == BoosterType.L.toByte()
inline val Byte.isMysteriousPoint: Boolean get() = this == BoosterType.X.toByte()

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

class ByteMatrix(
    val numRows: Int,
    val numCols: Int,
    value: Byte
) {
    private val buf: ByteArray = ByteArray(numRows * numCols).apply { fill(value) }

    operator fun set(poly: Poly, value: Byte) {
        val (bottomLeft, topRight) = poly.bbox
        val (minX, minY) = bottomLeft
        val (maxX, maxY) = topRight

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val p = Point(x, y)
                if (p in poly) {
                    set(p, value)
                }
            }
        }
    }

    operator fun get(p: Point) = get(p.y, p.x)

    operator fun set(p: Point, value: Byte) = set(p.y, p.x, value)

    fun contains(p: Point): Boolean {
        return p.x >= 0 && p.y >= 0 && p.x < numRows && p.y < numCols
    }

    private operator fun get(i: Int, j: Int) = buf[i * numCols + j]

    private operator fun set(i: Int, j: Int, value: Byte) {
        buf[i * numCols + j] = value
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (y in 0 until numRows) {
            for (x in 0 until numCols) {
                sb.append(get(y, x).toChar())
            }

            sb.append('\n')
        }

        return sb.toString()
    }
}

/** It this cell inside an obstacle? */
inline val Byte.isObstacle: Boolean get() = this == OBSTACLE
/** Has the cell been wrapped by Wrappy? */
inline val Byte.isWrapped: Boolean get() = this == WRAPPED
/** Is it out of bounds of the map? */
inline val Byte.isVoid: Boolean get() = this == VOID
/** Is it within bounds of the map and could be wrapped? */
inline val Byte.isFree: Boolean get() = this == FREE

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
        val numRows = (maxY + 1) - minY
        val numCols = (maxX + 1) - minX
        val grid = ByteMatrix(numRows, numCols, VOID)
        grid[map] = FREE
        for (obstacle in obstacles) {
            grid[obstacle] = OBSTACLE
        }
        for (booster in boosters) {
            grid[booster.loc] = booster.type.toByte()
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
        board[u] = WRAPPED
        for (i in 0..3) {
            val v = Point(u.x + DX[i], u.y + DY[i])
            println(v)
            if (board.contains(v) && board[v] == FREE) {
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
//    val naive = Naive(state)
//    // TODO(alexeyc): change to the real start point
//    println(naive.go(Point(0, 0)))

    launchGui()
    val pills: MutableList<Pair<Point, Pill>> = mutableListOf()
    draw(Map(state.grid.numRows, state.grid.numCols,
        { p ->
            val c = state.grid[p]
            if (state.wrappy.contains(p)) {
                pills += p to Pill.ROBOT
            }
            when {
                c.isObstacle -> Cell.WALL
                c.isFree -> Cell.FREE
                c.isWrapped -> Cell.WRAPPED
                c.isVoid -> Cell.VOID
                else -> {
                    when {
                        c.isExtension -> pills += p to Pill.BOOST_B
                        c.isDrill -> pills += p to Pill.BOOST_L
                        c.isFastWheels -> pills += p to Pill.BOOST_F
                        c.isMysteriousPoint -> pills += p to Pill.BOOST_X
                    }
                    Cell.FREE
                }
            }

        }, pills))
    for (i in 0 until 100) {
        Thread.sleep(300)
    }

    println(state.grid)
}
