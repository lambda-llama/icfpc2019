package io.github.lambdallama

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
        // TODO(superbobry): apply shift if minX/minY are non-zero.
        check(minX == 0 && minY == 0)
        val numRows = maxY - minY
        val numCols = maxX - minX
        val grid = ByteMatrix(numRows, numCols, Cell.VOID)
        listOf(map).project(grid, Cell.FREE)
        obstacles.project(grid, Cell.OBSTACLE)
        for (booster in boosters) {
            grid[booster.loc] = booster.type.toCell()
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

enum class BoosterType {
    B, F, L, X, T;

    fun toCell(): Cell = when (this) {
        B -> Cell.B_EXTENSION
        F -> Cell.B_FAST_WHEELS
        L -> Cell.B_DRILL
        X -> Cell.B_MYSTERIOUS_POINT
        T -> Cell.B_TELEPORT
    }
}

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
