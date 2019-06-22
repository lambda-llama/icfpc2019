package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

object CloneFactory : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val clones = state.boosters.filter { it.value == BoosterType.C }
            .map { it.key }
        val spawner = state.boosters.filter { it.value == BoosterType.X }
            .keys.first()

        val route = travelingSalesman(
            state.robot.position, spawner, clones.toMutableSet(), state.grid
        )!!

        for ((p1, p2) in route.zip(route.drop(1))) {
            val move = Move(p1, p2)
            sink(listOf(move))
            state.apply(listOf(move))
        }
        while (state.hasBooster(BoosterType.C)) {
            val nRobots = state.robots.size
            val nBoosters = state.nBoosters(BoosterType.C)
            val actions = (0 until nRobots).map { if (it < nBoosters) Clone else null }
            state.apply(actions)
            sink(actions)
        }
    }
}

fun travelingSalesman(
    start: Point,
    end: Point,
    intermediate: MutableSet<Point>,
    grid: ByteMatrix
): List<Point>? {
    val path = mutableListOf(start)
    while (true) {
        val prev = path.last()
        val next = intermediate.minBy { it.manhattanDist(prev) } ?: break
        intermediate.remove(next)
        path += (shortestPath(prev, next, grid) ?: return null).drop(1)
    }
    path += (shortestPath(path.last(), end, grid) ?: return null).drop(1)
    assert(path.first() == start && path.last() == end)
    return path
}

fun shortestPath(start: Point, end: Point, grid: ByteMatrix): List<Point>? {
    val prev = mutableMapOf(start to start)
    val work = ArrayDeque<Point>()
    work.add(start)

    outer@
    while (work.isNotEmpty()) {
        val u = work.pollFirst()
        for (v in grid.freeNeighbours(u).filter { it !in prev }) {
            prev[v] = u
            work.addLast(v)
            if (v == end) break@outer
        }
    }

    if (end !in prev) return null

    val path = mutableListOf(end)
    while (true) {
        val u = prev[path.last()]!!
        path += u
        if (u == start) break
    }
    path.reverse()
    return path
}

fun ByteMatrix.freeNeighbours(u: Point): List<Point> =
    Move.ALL.map { it(u) }.filter { u in this && !this[u].isObstacle }
