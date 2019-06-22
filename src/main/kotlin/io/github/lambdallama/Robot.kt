package io.github.lambdallama

import kotlin.math.abs

data class Robot(var position: Point,
                 val tentacles: MutableList<Point>,
                 val orientation: Orientation) {

    var boosters: MutableMap<BoosterType, Int> = mutableMapOf(BoosterType.B to 0, BoosterType.F to 0,
            BoosterType.L to 0, BoosterType.R to 0, BoosterType.C to 0)

    fun clone() = Robot(position, tentacles.toMutableList(), orientation)

    fun move(grid: ByteMatrix, position: Point) {
        require(position in grid)
        val cell = grid[position]
        require(cell != Cell.OBSTACLE && cell != Cell.VOID)
        val d = this.position - position
        require(d.x == 0 && abs(d.y) == 1 || abs(d.x) == 1 && d.y == 0)
        this.position = position

        if (cell.isBooster) {
            val type = BoosterType.fromCell(cell)
            this.boosters[type] = this.boosters[type]!! + 1
            grid[position] = Cell.FREE
        }
    }

    fun getVisibleParts(grid: ByteMatrix): List<Point> {
        // TODO: this is a quick, dirty visibility check with false negatives
        // 31024 <- tentacle indices, the algorithm depends on this order
        //   R
        // For each direction, if we encounter a wall on either the tentacles' level or robot level,
        // consider the rest of the tentacles in that direction to be invisible.
        // Tentacles 0-2 are checked in a normal way.
        //
        // Example:
        // 310W4 <- here only 0 and 1 are visible, since
        //  WR

        val parts =
                tentacles.map { tentacle ->
                    tentacle.rotate(orientation) + position
                }

        var idx = 0
        var hitWall = mutableListOf(false, false)
        val robotDelta = this.position - parts[0]
        return listOf(this.position) + parts.filter { p ->
            val wall = p !in grid || grid[p].isObstacle
            val pr = p + robotDelta
            val robotLevelWall = pr !in grid || grid[pr].isObstacle
             if (idx > 0) {
                hitWall[idx % 2] = hitWall[idx % 2] || wall || robotLevelWall
            }
            val result = !wall && (idx < 3 || !hitWall[idx % 2])
            idx++
            result
        }
    }

    fun wrap(grid: ByteMatrix) {
        getVisibleParts(grid).forEach { p ->
            if (grid[p].isWrapable) {
                grid[p] = Cell.WRAPPED
            }
        }
    }

    fun extendReach(): Point {
        require(this.boosters[BoosterType.B]!! > 0)
        this.boosters[BoosterType.B] = this.boosters[BoosterType.B]!! - 1
        val last = this.tentacles.last()
        val newTentacle = Point(1, if (last.y > 0) -last.y else -last.y + 1)
        this.tentacles.add(newTentacle)
        return newTentacle
    }
}
