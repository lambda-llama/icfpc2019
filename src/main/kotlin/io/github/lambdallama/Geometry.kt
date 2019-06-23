package io.github.lambdallama

import com.carrotsearch.hppc.IntArrayDeque
import com.google.common.collect.ArrayListMultimap
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

data class Point(val x: Int, val y: Int) {
    operator fun unaryMinus() = Point(-x, -y)

    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)
    operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)
    operator fun times(other: Point): Int = x * other.x + y * other.y

    fun manhattanDist(other: Point): Int =
        (x - other.x).absoluteValue + (y - other.y).absoluteValue

    fun rotate(orientation: Orientation) = Point(orientation.ax * this, orientation.ay * this)

    fun reverseRotate(orientation: Orientation) = rotate(orientation.opposite)

    override fun toString() = "($x,$y)"

    companion object {
        fun parse(s: String): Point {
            check(s.first() == '(' && s.last() == ')')
            val (x, y) = s.slice(1 until s.length - 1).split(',', limit = 2)
            return Point(x.toInt(), y.toInt())
        }
    }
}

class PointSet(private val dim: Point) {
    private val buf = BitSet(dim.x * dim.y)

    fun add(p: Point) = buf.set(p.y * dim.x + p.x)

    fun remove(p: Point) = buf.clear(p.y * dim.x + p.x)

    operator fun contains(p: Point) = buf.get(p.y * dim.x + p.x)

    fun asSequence(): Sequence<Point> {
        var offset = 0
        return generateSequence {
            val idx = buf.nextSetBit(offset)
            if (idx < 0) null else {
                val y = idx / dim.x
                val x = idx - y * dim.x
                check(y * dim.x + x == idx)
                offset = idx + 1
                Point(x, y)
            }
        }
    }

    fun isEmpty() = buf.isEmpty
    fun isNotEmpty() = !isEmpty()
}

class PointDeque(capacity: Int = 4) {
    private val deque = IntArrayDeque(capacity)

    fun addFirst(p: Point) = deque.addFirst(p.xy)
    fun addLast(p: Point) = deque.addLast(p.xy)
    fun removeFirst() = Point(deque.removeFirst())
    fun removeLast() = Point(deque.removeLast())
    fun getFirst() = Point(deque.first)
    fun getLast() = Point(deque.last)

    fun isEmpty() = deque.isEmpty
    fun isNotEmpty() = !isEmpty()

    companion object {
        private inline val Point.xy: Int get() = (x shl 16) or (y and 0xffff)

        private operator fun Point.Companion.invoke(xy: Int): Point {
            return Point(x = xy shr 16, y = xy.toShort().toInt())
        }

    }
}

class PointIntMap(val dim: Point) {
    // Assumption: Int.MAX_VALUE is the missing value.
    private val buf = IntArray(dim.x * dim.y).apply { fill(MISSING) }

    private fun getInternal(p: Point) = buf[p.y * dim.x + p.x]

    operator fun get(p: Point): Int {
        val value = getInternal(p)
        return if (value != MISSING) value else {
            throw NoSuchElementException()
        }
    }

    fun getOrDefault(p: Point, default: Int): Int {
        val value = getInternal(p)
        return if (value != MISSING) value else default
    }

    operator fun set(p: Point, value: Int) {
        require(value != MISSING)
        buf[p.y * dim.x + p.x] = value
    }

    operator fun contains(p: Point) = getInternal(p) != MISSING

    companion object {
        private const val MISSING = Int.MAX_VALUE
    }
}

enum class Orientation(
        val ax: Point,
        val ay: Point
) {
    RIGHT(
            ax = Point(1, 0),
            ay = Point(0, 1)
    ),
    UP(
            ax = Point(0, -1),
            ay = Point(1, 0)
    ),
    LEFT(
            ax = Point(-1, 0),
            ay = Point(0, -1)
    ),
    DOWN(

            ax = Point(0, 1),
            ay = Point(-1, 0)
    );

    fun rotate(direction: Rotation): Orientation =
            if (direction == Rotation.COUNTERCLOCKWISE)
                Orientation.values()[(ordinal + 1) and 3]
            else
                Orientation.values()[(ordinal + 3) and 3]

    val opposite get(): Orientation = when (this) {
        Orientation.RIGHT -> Orientation.RIGHT
        Orientation.UP -> Orientation.DOWN
        Orientation.LEFT -> Orientation.LEFT
        Orientation.DOWN -> Orientation.UP
    }
}

enum class Rotation {
    COUNTERCLOCKWISE,
    CLOCKWISE
}

inline class Poly(val contour: List<Point>) {
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
            return Poly(parseRepSepPoints(s, sep=','))
        }
    }
}

data class Vertical(val x: Int, val ay: Int, val by: Int)

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
                    if (2 * y + 1 in 2 * v.ay..2 * v.by) {
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
