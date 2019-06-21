package io.github.lambdallama

import java.io.File
import kotlin.math.max
import kotlin.math.min

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
    val bbox: Pair<Point, Point> get() {
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
    numRows: Int,
    private val numCols: Int,
    value: Byte
) {
    private val buf: ByteArray = ByteArray(numRows * numCols).apply { fill(value) }

    operator fun set(p: Poly, value: Byte) {

    }

    operator fun get(p: Point) = get(p.y, p.x)

    operator fun set(p: Point, value: Byte) = set(p.y, p.x, value)

    operator fun get(i: Int, j: Int) = buf[i * numCols + j]

    operator fun set(i: Int, j: Int, value: Byte) {
        buf[i * numCols + j] = value
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
        val numRows = maxY - minY
        val numCols = maxX - minX
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


fun main(args: Array<String>) {
    println(Task.parse(File("part-1-initial/prob-001.desc").readText()))
}
