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

        for (move in route.toMoves()) {
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

        val chunks = chunkify(state.grid, state.robots.size)
        val paths = chunks.map { chunk ->
            val start = state.robot.position
            val end = chunk.last()
            val mid = chunk.dropLast(1).toMutableSet()
            travelingSalesman(start, end, mid, state.grid)!!
        }
        val moves = paths.map { it.toMoves() }
        check(moves.size == state.robots.size)
        val len = moves.map { it.size }.max()!!
        for (i in 0 until len) {
            val actions = moves.map { it.getOrNull(i) }
            state.apply(actions)
            sink(actions)
        }
    }
}

fun List<Point>.toMoves(): List<Move> =
    this.zip(this.drop(1)).map { Move(it.first, it.second) }

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
    Move.ALL.map { it(u) }.filter { it in this && !this[it].isObstacle }

fun chunkify(grid: ByteMatrix, n: Int): List<List<Point>> {
    val wrappable = mutableSetOf<Point>()
    for (x in 0 until grid.dim.x) {
        for (y in 0 until grid.dim.y) {
            val p = Point(x, y)
            if (grid[p].isWrapable) {
                wrappable += p
            }
        }
    }
    val chunkSize = wrappable.size / n

    val chunks = mutableListOf<List<Point>>()
    for (i in 0 until (n - 1)) {
        val chunk = mutableListOf<Point>()
        val seed = wrappable.first()
        for (p in bfs(seed, grid)) {
            if (grid[p].isWrapable) {
                wrappable.remove(p)
                chunk.add(p)
            }
            if (chunk.size >= chunkSize) break
        }
        chunks.add(chunk)
    }
    chunks.add(wrappable.toList())
    check(chunks.size == n)
    return chunks
}

fun bfs(point: Point, grid: ByteMatrix): Sequence<Point> = sequence {
    val visited = mutableSetOf<Point>()
    val work = ArrayDeque<Point>()
    work.addLast(point)
    while (!work.isEmpty()) {
        val u = work.pollFirst()
        yield(u)
        for (v in grid.freeNeighbours(u).filter { it !in visited }) {
            work.addLast(v)
            visited += v
        }
    }
}