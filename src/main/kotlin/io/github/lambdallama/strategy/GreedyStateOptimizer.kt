package io.github.lambdallama.strategy

import io.github.lambdallama.*

typealias StateFunction = (State, Robot) -> Double
typealias ActionPolicy = (State, Robot) -> List<Action>
typealias StateChangeCallback = (State) -> Unit

fun noOpStateChangeCallback(state: State) {
}

class GreedyStateOptimizer(
    val stateFunction: StateFunction,
    val actionPolicy: ActionPolicy,
    val stateChangeCallback: StateChangeCallback = ::noOpStateChangeCallback,
    private val strategyName: String? = null
) : Strategy {
    override val name: String get() = strategyName ?: super.name

    override fun run(state: State, sink: ActionSink) {
        stateChangeCallback(state)

        var isTerminal = false
        while (!isTerminal) {
            isTerminal = true
            val robotMoves = mutableListOf<Action>()
            var robotIndex = 0
            while (robotIndex < state.robots.size) {
                val robot = state.robots[robotIndex]
                val moves = actionPolicy(state, robot)
                val best = moves
                    .map { move ->
                        val unmove = state.apply(robot, move)
                        val q = stateFunction(state, robot)
                        state.unapply(robot, unmove)
                        q to move
                    }.maxBy { it.first }
                if (best == null) {
                    robotMoves.add(NoOp)
                } else {
                    val bestMove = best.second
                    robotMoves.add(bestMove)
                    isTerminal = false
                    state.apply(robot, bestMove)
                    stateChangeCallback(state)
                }
                robotIndex += 1
            }
            if (!isTerminal) {
                sink(robotMoves)
            }
        }
    }
}

private val MOVES = arrayOf(MoveUp, MoveDown, MoveLeft, MoveRight)
private val TURNS = arrayOf(TurnClockwise, TurnCounter)

fun accessibleOnMap(state: State, p: Point): Boolean {
    return p in state.grid && !state.grid[p].isObstacle
}

fun allAllowedMovesPolicy(state: State, robot: Robot): List<Action> {
    if (!state.hasWrappableCells) {
        return listOf()
    }
    return MOVES.filter { move ->
        accessibleOnMap(state, move(robot.position))
    }
}

fun moveAndTurn(state: State, robot: Robot): List<Action> {
    if (state.hasBooster(BoosterType.B)) {
        return listOf(Attach(robot.attachmentPoint()))
    }
    var moves = allAllowedMovesPolicy(state, robot)
    if (moves.isEmpty()) {
        return moves
    }
    moves += TURNS
    return moves
}

fun wrappedRatio(state: State): Double {
    val wrapped = state.grid.count(Cell.WRAPPED)
    val free = state.grid.count(Cell.FREE)
    return wrapped / (wrapped + free).toDouble()
}

private typealias PointPredicate = (State, Point) -> Boolean

private fun noEarlyStopping(state: State, point: Point): Boolean {
    return false
}

private typealias OnDistanceUpdateCallback = (Point, Point) -> Unit

private fun noOpOnDistanceUpdate(from: Point, to: Point) {

}

private fun bfs(state: State, from: List<Point>,
                dist: MutableMap<Point, Int>,
                canVisit: PointPredicate,
                earlyStopping: PointPredicate = ::noEarlyStopping,
                onDistanceUpdate: OnDistanceUpdateCallback = ::noOpOnDistanceUpdate
): List<Point> {
    val q = PointDeque()
    for (u in from) {
        dist[u] = 0
        q.addLast(u)
    }

    val visited = mutableListOf<Point>()
    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        visited.add(u)
        // Remaining nodes in the queue won't be marked as visited!
        if (earlyStopping(state, u)) {
            break
        }
        for (move in MOVES) {
            val v = move(u)
            if (canVisit(state, v)) {
                var du = dist[u]!! + 1
                if (dist.getOrDefault(v, Int.MAX_VALUE) > du) {
                    dist[v] = du
                    q.addLast(v)
                    onDistanceUpdate(u, v)
                }
            }
        }
    }
    return visited
}

fun distance(state: State, from: Robot,
             targetPointPredicate: PointPredicate,
             canVisit: PointPredicate
): Int? {
    val d = HashMap<Point, Int>()
    bfs(state, from = listOf(from.position), dist = d, earlyStopping = targetPointPredicate, canVisit = canVisit)
    for ((u, du) in d) {
        if (targetPointPredicate(state, u)) {
            return du
        }
    }
    return null
}

fun isWrapable(state: State, p: Point): Boolean {
    return state.grid[p].isWrapable
}

val WrapDistanceCount = GreedyStateOptimizer(
    stateFunction = { state: State, robot: Robot ->
        val dw = distance(
            state, from = robot,
            targetPointPredicate = ::isWrapable,
            canVisit = ::accessibleOnMap
        )?.toDouble() ?: 0.0
        val nDw = dw / (state.grid.dim.x * state.grid.dim.y)

        state.grid.count(Cell.WRAPPED) - nDw
    },
    actionPolicy = ::moveAndTurn,
    strategyName = "SimpleStateOptimizer"
)

fun inComponent(s: State, p: Point): Boolean {
    return p in s.grid && isWrapable(s, p)
}


data class ComponentInfo(
    var id: Int,
    var size: Int, var robot: Robot?,
    var farawayPoint: Point?, var farawayDist: Int,
    var points: List<Point>
)

private object UnwrappedComponents {

    private val cellComponent = HashMap<Point, Int>()
    private val d = HashMap<Point, Int>()
    private val parent = HashMap<Point, Point?>()
    private val robotComponents = HashMap<Int, MutableList<Int>>()
    private var componentInfo = mutableListOf<ComponentInfo>()
    private var robotLocalPoints = HashMap<Int, List<Point>>()

    fun getRobotComponents(robot: Robot): List<ComponentInfo> {
        val componentInds = robotComponents[robot.id]!!
        return componentInds.map { componentInfo[it] }
    }

    fun getComponentId(p: Point): Int {
        return cellComponent[p] ?: -1
    }

    fun getRobotLocalPoints(robot: Robot) = robotLocalPoints[robot.id]!!

    fun updateState(state: State) {
        val grid = state.grid
        d.clear()
        cellComponent.clear()
        componentInfo.clear()
        var componentsCount =  0
        for (x in 0 until grid.dim.x) {
            for (y in 0 until grid.dim.y) {
                val p = Point(x, y)
                if (!isWrapable(state, p) || p in d) {
                    continue
                }

                val component = bfs(state, from = listOf(p), dist = d, canVisit = ::inComponent)
                for (q in component) {
                    cellComponent[q] = componentsCount
                }
                componentInfo.add(
                    ComponentInfo(
                        id = componentsCount,
                        size = 0, robot = null,
                        farawayPoint = null, farawayDist = 0,
                        points = component
                    )
                )
                componentsCount += 1
            }
        }
        d.clear()
        parent.clear()
        val robotPositions = state.robots.map { it.position }
        for (p in robotPositions) {
            parent[p] = p
        }
        bfs(
            state, from = robotPositions,
            dist = d, canVisit = ::accessibleOnMap,
            onDistanceUpdate = { from, to -> parent[to] = parent[from] }
        )
        for ((p, cInd) in cellComponent) {
            val info = componentInfo[cInd]
            val dist = d[p]!!
            info.size += 1
            if (info.farawayDist < dist) {
                info.farawayDist = dist
                info.farawayPoint = p
            }
        }
        robotComponents.clear()
        for (robot in state.robots) {
            robotComponents[robot.id] = mutableListOf()
            for (cInd in 0 until componentsCount) {
                val info = componentInfo[cInd]
                if (info.robot != null) {
                    continue
                }
                if (parent[info.farawayPoint] == robot.position) {
                    info.robot = robot
                    (robotComponents[robot.id]!!).add(cInd)
                }
            }
        }

        robotLocalPoints.clear()
        for (robot in state.robots) {
            robotLocalPoints[robot.id] = robot.pointsOfInterest(state)
        }
    }
}

fun componentSize(state: State, component: ComponentInfo, robot: Robot): Int {
    var count = 0;
    for (p in UnwrappedComponents.getRobotLocalPoints(robot)) {
        if (UnwrappedComponents.getComponentId(p) == component.id &&
            state.grid[p] == Cell.FREE) {
            count += 1
        }
    }
    return count
}

fun companentBasedCost(state: State, robot: Robot): Double {
    val components = UnwrappedComponents.getRobotComponents(robot)
    val component = components.minBy { component -> component.farawayDist }
    if (component == null) {
        // Explore without assignment
        val dw = distance(
            state, from = robot,
            targetPointPredicate = ::isWrapable,
            canVisit = ::accessibleOnMap
        )?.toDouble() ?: 0.0
        val nDw = dw / (state.grid.dim.x * state.grid.dim.y)

        // TODO(alexeyc): Use FREE as unwrapped synonym as optimization.
        // TODO(alexeyc): Inverse state cost function here and above.
        return -state.grid.count(Cell.FREE) - nDw
    } else {
        // Explore without assignment
        val dw = distance(
            state, from = robot,
            targetPointPredicate = { state: State, p: Point ->
                UnwrappedComponents.getComponentId(p) == component.id
            },
            canVisit = ::accessibleOnMap
        )?.toDouble() ?: 0.0
        val nDw = dw / (state.grid.dim.x * state.grid.dim.y)
        val componentLocalSize = componentSize(state, component, robot)

        return  -componentLocalSize - nDw
    }
}


val FillAndExplore = GreedyStateOptimizer(
    stateFunction = ::companentBasedCost,
    actionPolicy = ::moveAndTurn,
    stateChangeCallback = UnwrappedComponents::updateState,
    strategyName = "FillAndExplore"
)