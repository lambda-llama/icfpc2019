package io.github.lambdallama

import java.io.File

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
    val type: Char,
    val loc: Point) {
    companion object {
        fun parse(s: String): Booster {
            val type = s.first().also { ch ->
                require(ch in "BFLX") { "invalid booster type: $ch" }
            }
            return Booster(type, Point.parse(s.slice(1 until s.length)))
        }
    }
}

data class Task(
    val map: Poly,
    val initialLoc: Point,
    val obstacles: List<Poly>,
    val boosters: List<Booster>
) {
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
