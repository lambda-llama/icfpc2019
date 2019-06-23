package io.github.lambdallama

data class Robot(
        var id: Int,
        var position: Point,
        val tentacles: MutableList<Point>,
        var orientation: Orientation,
        var fuelLeft: Int = 0
) {
    constructor(position: Point, id: Int)
        : this(
            id,
            position,
            tentacles = mutableListOf(
                    Point(1, 0),
                    Point(1, 1),
                    Point(1, -1)
            ),
            orientation = Orientation.RIGHT,
            fuelLeft = 0
    )


    fun clone() = Robot(id, position, tentacles.toMutableList(), orientation, fuelLeft)

    fun rotate(rotation: Rotation) {
        orientation = orientation.rotate(rotation)
    }

    fun getVisibleParts(grid: ByteMatrix) = getVisiblePartsAt(grid, position)

    fun getVisiblePartsAt(grid: ByteMatrix, position: Point): Sequence<Point> {
        // TODO: this is a quicker, dirtier visibility check
        //   4
        //   3
        //  102 <- tentacle indices, the algorithm depends on this order
        //   R

        val res = mutableListOf<Point>()
        res += position
        var p = tentacles[1].rotate(orientation) + position
        if (p in grid && !grid[p].isObstacle) {
            res += p
        }
        p = tentacles[2].rotate(orientation) + position
        if (p in grid && !grid[p].isObstacle) {
            res += p
        }

        for (t in listOf(tentacles[0]) + tentacles.drop(3)) {
            p = t.rotate(orientation) + position
            if (p in grid && !grid[p].isObstacle) {
                res += p
            } else break
        }
        return res.asSequence()
    }

    fun attachTentacle(at: Point) {
        tentacles += at.reverseRotate(orientation)
    }

    fun detachLastTentacle() {
        tentacles.removeAt(tentacles.count() - 1)
    }

    fun attachmentPoint(): Point {
        val x = this.tentacles.map { it.x }.max()!!
        return Point(x + 1, 0).rotate(orientation)
    }
}
