package io.github.lambdallama

data class Robot(var position: Point,
                 val tentacles: List<Point>,
                 val orientation: Orientation) {

    fun wrap(grid: ByteMatrix) {
        parts.forEach { p ->
            if (p in grid && grid[p] == Cell.FREE) {
                grid[p] = Cell.WRAPPED
            }
        }
    }

    val parts: List<Point> get() = listOf(position) +
        tentacles.map { tentacle ->
            tentacle.rotate(orientation) + position
        }
}
