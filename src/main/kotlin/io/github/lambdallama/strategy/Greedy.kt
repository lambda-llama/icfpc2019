package io.github.lambdallama.strategy

import com.google.common.collect.ComparisonChain
import io.github.lambdallama.*
import java.util.*

private inline fun State.bfs(
    initial: Point,
    shouldStop: (Point) -> Boolean): List<Point> {
    val backtrack = mutableMapOf<Point, Point?>(initial to null)
    val q = PointDeque()
    q.addLast(initial)
    var u: Point? = initial
    while (q.isNotEmpty()) {
        u = q.removeFirst()
        if (shouldStop(u)) {
            break
        }

        forEachReachable(u) {v ->
            if (v !in backtrack) {
                q.addLast(v)
                backtrack[v] = u
            }
        }
    }

    val path = mutableListOf<Point>()
    while (u != null && u in backtrack) {
        path.add(u)
        u = backtrack[u]
    }
    path.reverse()
    return if (path.isNotEmpty() && shouldStop(path.last())) {
        check(path.first() == initial)
        path
    } else {
        emptyList()
    }
}

interface Greedy : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val grid = state.grid
        sink(listOf(TurnClockwise))
        state.apply(listOf<Action>(TurnClockwise))
        while (true) {
            check(grid[state.robot.position] == Cell.WRAPPED)
            val path = route(state)
            if (path.isEmpty()) {
                break
            }

            follow(state, path, sink)
        }
    }

    fun route(state: State): List<Point> {
        val grid = state.grid
        return state.bfs(state.robot.position) { grid[it] == Cell.FREE }
    }

    fun follow(state: State, path: List<Point>, sink: ActionSink)
}

object GreedyUnordered: Greedy {
    override fun follow(state: State, path: List<Point>, sink: ActionSink) {
        for (v in path.drop(1)) {
            state.howTo(v).forEach {
                sink(listOf(it))
                state.apply(listOf(it))
            }
            if (state.hasBooster(BoosterType.B)) {
                val attach = Attach(state.robot.attachmentPoint())
                sink(listOf(attach))
                state.apply(listOf(attach))
            }
        }
        /*Uncomment when we are ready to do approx BFS in [route].
        if (state.hasBooster(BoosterType.F)) {
            state.apply(listOf(Accelerate))
            sink(listOf(Accelerate))
        }
        */
    }
}

fun Rotation.toMove() = when (this) {
    Rotation.CLOCKWISE -> TurnClockwise
    Rotation.COUNTERCLOCKWISE -> TurnCounter
}

object GreedyUnorderedTurnover: Greedy {
    override fun follow(state: State, path: List<Point>, sink: ActionSink) {
        val grid = state.grid

        // Find a starting rotation which maximizes number of wrapped cells per action.
        val rotation = arrayOf(null, Rotation.CLOCKWISE, Rotation.COUNTERCLOCKWISE).maxBy { candidate ->
            var wrapable = 0
            var score = 0
            val clone = state.robot.clone()
            val boosters = HashMap(state.collectedBoosters)
            candidate?.let {
                clone.rotate(it)
                score++
            }
            for (i in 1 until path.size) {
                val v = path[i]
                clone.position = v  // .move is mutating!
                score++
                val boosterType = state.boosters[v]
                if (boosterType != null && boosterType != BoosterType.X) {
                    boosters[boosterType] = boosters[boosterType]!! + 1
                }
                if (boosters[BoosterType.B]!! > 0) {
                    clone.attachTentacle(clone.attachmentPoint())
                    score++
                }
                wrapable += clone.getVisibleParts(grid).count { grid[it] == Cell.FREE }
            }

            wrapable / score.toDouble()
        }

        if (rotation != null) {
            sink(listOf(rotation.toMove()))
            state.apply(listOf(rotation.toMove()))
        }
        return GreedyUnordered.follow(state, path, sink)
    }
}

/** Partition the matrix into components of connected FREE/Booster cells. */
fun ByteMatrix.fbPartition(): List<PointSet> {
    val components = mutableListOf<PointSet>()
    val seen = PointSet(dim)
    for (x in 0 until dim.x) {
        for (y in 0 until dim.y) {
            val initial = Point(x, y)
            if (initial !in seen && this[initial] == Cell.FREE) {
                val component = PointSet(dim)
                component.add(initial)
                val q = PointDeque()
                q.addLast(initial)
                while (q.isNotEmpty()) {
                    val u = q.removeLast()
                    for (move in Move.ALL) {
                        val v = move(u)
                        if (v in this && v !in seen && this[v] == Cell.FREE) {
                            q.addLast(v)
                            component.add(v)
                            seen.add(v)
                        }
                    }
                }

                components.add(component)
            }
        }
    }
    return components
}

interface GreedyFBPartition : Greedy {
    private data class Cost(val distance: Int, val wrapable: Int) : Comparable<Cost> {
        override fun compareTo(other: Cost) = ComparisonChain.start()
            .compare(distance, other.distance)
            .compare(-wrapable, -other.wrapable)
            .result()
    }

    private fun Point.cost(state: State, distances: PointIntMap): Cost {
        val wrapable = state.robot.getVisiblePartsAt(state.grid, this)
            .count { state.grid[it] == Cell.FREE }
        return Cost(distances.getOrDefault(this, Int.MAX_VALUE), wrapable)
    }

    override fun route(state: State): List<Point> {
        val grid = state.grid
        val components = grid.fbPartition()
        return when (components.size) {
            0 -> emptyList()
            1 -> super.route(state)
            else -> {
                val distances = distanceToAll(state, state.robot.position)
                // XXX minBy/max calls bellow are sensitive to point
                //     order within a component due to collisions!
                val closestComponent = components.minBy { component ->
                    component.asSequence().map { it.cost(state, distances) }.max()!!
                }!!

                val closestPoint = closestComponent.asSequence().minBy { it.cost(state, distances) }!!
                val path = state.bfs(state.robot.position) { it == closestPoint }
                check(path.isNotEmpty())
                return path
            }
        }
    }
}

private fun State.howTo(v: Point): List<Action> {
    val u = robot.position
    return if (v in beacons) {
        listOf(Teleport(v))
    } else {
        val move = Move.ALL.firstOrNull { it(u) == v }
        if (move == null) {
            val moveMove = Move.ALL.first { it(it(u)) == v }
            if (robot.fuelLeft > 0) listOf(moveMove) else listOf(moveMove, moveMove)
        } else {
            listOf(move)
        }
    }
}

private inline fun State.forEachReachable(u: Point, block: (Point) -> Unit) {
    beacons.forEach(block)
    for (move in Move.ALL) {
        val v = move(u)
        if (v in grid && !grid[v].isObstacle) {
            if (robot.fuelLeft > 0) {  // Accelerated.
                val w = move(v)
                if (w in grid && !grid[w].isObstacle) {
                    block(w)
                } else {  // Second step is no-op.
                    block(v)
                }

            } else {
                block(v)
            }
        }
    }
}

private fun distanceToAll(state: State, initial: Point): PointIntMap {
    val grid = state.grid
    val distances = PointIntMap(grid.dim)
    distances[initial] = 0
    val q = PointDeque()
    q.addLast(initial)
    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        state.forEachReachable(u) { v ->
            val alternative = distances[u] + 1
            if (alternative < distances.getOrDefault(v, Int.MAX_VALUE)) {
                distances[v] = alternative
                q.addLast(v)
            }
        }
    }
    return distances
}

object GreedyUnorderedFBPartition : GreedyFBPartition {
    override fun follow(state: State, path: List<Point>, sink: ActionSink) {
        return GreedyUnordered.follow(state, path, sink)
    }
}

object GreedyTurnoverFBPartition : GreedyFBPartition {
    override fun follow(state: State, path: List<Point>, sink: ActionSink) {
        return GreedyUnorderedTurnover.follow(state, path, sink)
    }
}

object InstallUniformBeacons : Strategy {
    override fun run(state: State, sink: ActionSink) {
        val teleports = state.boosters.filterValues { it == BoosterType.R }.keys
        val fastWheels: Set<Point> = state.boosters.filterValues { it == BoosterType.F }.keys
        val extensions: Set<Point> = state.boosters.filterValues { it == BoosterType.B }.keys
        val targets = if (teleports.isNotEmpty()) {
            val grid = state.grid
            val distances = distanceToAll(state, state.robot.position)
            val targets = chunkify(grid, teleports.size).map { chunk ->
                chunk.last()
                val d = IntArray(chunk.size) { distances[chunk[it]] }
                d.sort()  // Poorman's median.
                val median = d[d.size / 2]
                chunk.first { distances[it] == median }
            }
            check(targets.none { grid[it].isObstacle || BoosterType.X == state.boosters[it] })
            check(targets.size == teleports.size)
            targets
        } else emptyList()

        // No beacons to install? Who cares!
        installBeacons(state, teleports, targets, fastWheels, extensions, sink)
    }

    private fun installBeacons(
        state: State,
        teleports: Set<Point>,
        targets: List<Point>,
        fastWheels: Set<Point>,
        extensions: Set<Point>,
        sink: ActionSink
    ) {
        val grid = state.grid
        var collected = 0
        var installed = 0
        var attached = 0
        val q = PointSet(grid.dim)
        teleports.forEach(q::add)
        fastWheels.forEach(q::add)
        extensions.forEach(q::add)
        while (installed < teleports.size && attached < extensions.size) {
            if (state.hasBooster(BoosterType.F)) {
                state.apply(listOf(Accelerate))
                sink(listOf(Accelerate))
            }

            val distances = distanceToAll(state, state.robot.position)
            val next = q.asSequence().minBy { distances.getOrDefault(it, Int.MAX_VALUE) }!!
            var path = state.bfs(state.robot.position) { it == next }
            if (state.robot.fuelLeft > 0 && path.isEmpty()) {
                // Hmm... are all paths of odd length? Try to reach next approx.
                val neighborhood = Move.ALL.map { it(next) }.toTypedArray()
                path = state.bfs(state.robot.position) { it in neighborhood }
                // [next] is either one but last or at Manhattan distance 1 from it.
                if (path[path.lastIndex - 1] != next) {
                    path += next
                }
            }
            check(path.isNotEmpty())
            GreedyUnordered.follow(state, path, sink)
            when (next) {
                in teleports -> q.add(targets[collected++])
                in targets -> {
                    state.apply(listOf(InstallBeacon))
                    sink(listOf(InstallBeacon))
                    installed++
                }
                in extensions -> {
                    val action = Attach(state.robot.attachmentPoint())
                    state.apply(listOf(action))
                    sink(listOf(action))
                    attached++
                }
            }

            q.remove(next)
        }
    }
}