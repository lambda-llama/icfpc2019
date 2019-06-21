package io.github.lambdallama

import java.io.File
import kotlin.math.max
import kotlin.math.min

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

data class Booster(
    val type: Type,
    val loc: Point
) {

    companion object {
        enum class Type {
            B, F, L, X
        }

        fun parse(s: String): Booster = Booster(
            type = Type.valueOf(s.take(1)),
            loc = Point.parse(s.drop(1)))
    }
}

class ByteMatrix(
    numRows: Int,
    private val numCols: Int
) {
    private val buf: ByteArray = ByteArray(numRows * numCols)

    operator fun get(i: Int, j: Int) = buf[i * numCols + j]

    operator fun set(i: Int, j: Int, value: Byte) {
        buf[i * numCols + j] = value
    }
}

data class State(
    val buf: ByteMatrix
) {

}

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
        return State(ByteMatrix(numRows, numCols))
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
    startUI()
    for (i in 0..1000) {
        Thread.sleep(1000)
        setMap(Map(10, 10) { _, _ ->
            when (i % 3) {
                0 -> EMPTY
                1 -> WALL
                else -> WRAPPED
            }
        })
    }
    println(Task.parse(File("part-1-initial/prob-001.desc").readText()))
}
