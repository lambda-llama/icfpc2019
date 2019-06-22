package io.github.lambdallama

data class Robot(
    var position: Point,
    val tentacles: MutableList<Point>,
    var orientation: Orientation
) {

    fun clone() = Robot(position, tentacles.toMutableList(), orientation)

    fun rotate(rotation: Rotation) {
        orientation = orientation.rotate(rotation)
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
        val hitWall = mutableListOf(false, false)
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

    fun attachTentacle(at: Point) {
        tentacles += at.reverseRotate(orientation)
    }

    fun attachmentPoint(): Point {
        val last = this.tentacles.last()
        return Point(1, if (last.y > 0) -last.y else -last.y + 1).rotate(orientation)
    }
}
