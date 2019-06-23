package io.github.lambdallama.strategy

import io.github.lambdallama.*
import java.util.*

object ClonePhase : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val clones = state.boosters.filter { it.value == BoosterType.C }
            .map { it.key }
        val spawner = state.boosters.filter { it.value == BoosterType.X }
            .keys.firstOrNull() ?: return

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
            val actions = (0 until nRobots).map { if (it < nBoosters) Clone else NoOp }
            state.apply(actions)
            sink(actions)
        }
    }
}

object CloneExtenderPhase : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val spawner = state.boosters.filter { it.value == BoosterType.X }
            .keys.firstOrNull() ?: return ExtenderPhase.run(state, sink)

        var hasClones = false
        val clones = state.boosters.filter { (it.value == BoosterType.C && run { hasClones = true; true }) || it.value == BoosterType.B }
            .map { it.key }
        if (!hasClones) return ExtenderPhase.run(state, sink)

        val route = travelingSalesman(
            state.robot.position, spawner, clones.toMutableSet(), state.grid
        )!!

        for (move in route.toMoves()) {
            sink(listOf(move))
            state.apply(listOf(move))
            if (state.hasBooster(BoosterType.B)) {
                val action = Attach(state.robots.first().attachmentPoint())
                sink(listOf(action))
                state.apply(listOf(action))
            }
        }
        while (state.hasBooster(BoosterType.C)) {
            val nRobots = state.robots.size
            val nBoosters = state.nBoosters(BoosterType.C)
            val actions = (0 until nRobots).map { if (it < nBoosters) Clone else NoOp }
            state.apply(actions)
            sink(actions)
        }
    }
}

object ExtenderPhase : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val clones = state.boosters.filter { it.value == BoosterType.B }
            .map { it.key }
        if (clones.isEmpty()) return

        val route = travelingSalesman(
            state.robot.position, clones.last(), clones.dropLast(1).toMutableSet(), state.grid
        )!!

        for (move in route.toMoves()) {
            sink(listOf(move))
            state.apply(listOf(move))
            if (state.hasBooster(BoosterType.B)) {
                val action = Attach(state.robots.first().attachmentPoint())
                sink(listOf(action))
                state.apply(listOf(action))
            }
        }
    }
}


object CloneFactory : Strategy {
    override fun run(state: State, sink: ActionSink) {
        ClonePhase.run(state, sink)

        // Clone the state to capture the initial state of boosters.
        val cloneState = state.clone()
        val chunks = chunkify(cloneState.grid, cloneState.robots.size)
        val moves = chunks.withIndex().map { (idx, chunk) ->
            val fakeState = cloneState.fakeClone(idx)
            val fakeGrid = fakeState.grid
            for (other in chunks) {
                if (other === chunk) continue
                other.forEach { fakeGrid[it] = Cell.WRAPPED }
            }
            val moves = mutableListOf<Action>()
            GreedyUnorderedFBPartition.run(fakeState) { moves += it.single()!! }
            moves
        }
        check(moves.size == state.robots.size)
        val len = moves.map { it.size }.max()!!
        for (i in 0 until len) {
            val actions = moves.map { it.getOrNull(i) ?: NoOp }
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
    val work = PointDeque()
    work.addLast(start)

    outer@
    while (work.isNotEmpty()) {
        val u = work.removeFirst()
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
            if (grid[p].isWrapable && p in wrappable) {
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
    val work = PointDeque()
    work.addLast(point)
    while (!work.isEmpty()) {
        val u = work.removeFirst()
        yield(u)
        for (v in grid.freeNeighbours(u).filter { it !in visited }) {
            work.addLast(v)
            visited += v
        }
    }
}