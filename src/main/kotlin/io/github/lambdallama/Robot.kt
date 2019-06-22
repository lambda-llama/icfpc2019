package io.github.lambdallama

import kotlin.math.abs

data class Robot(var position: Point,
                 val tentacles: List<Point>,
                 val orientation: Orientation) {

    val parts: List<Point> get() = listOf(position) +
        tentacles.map { tentacle ->
            tentacle.rotate(orientation) + position
        }

    fun move(grid: ByteMatrix, position: Point) {
        require(position in grid)
        val cell = grid[position]
        require(cell != Cell.OBSTACLE && cell != Cell.VOID)
        val d = this.position - position
        require(d.x == 0 && abs(d.y) == 1 || abs(d.x) == 1 && d.y == 0)
        this.position = position
    }

    fun wrap(grid: ByteMatrix) {
        parts.forEach { p ->
            if (p in grid && grid[p].isWrapable) {
                grid[p] = Cell.WRAPPED
            }
        }
    }
}
