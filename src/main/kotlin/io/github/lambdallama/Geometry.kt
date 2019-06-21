package io.github.lambdallama

import com.google.common.collect.ArrayListMultimap
import kotlin.math.max
import kotlin.math.min

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

    operator fun plus(other: Point): Point = Point(xy + other.xy)

    override fun toString() = "($x,$y)"
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
