package io.github.lambdallama

data class Robot(val position: Point,
                 val tentacles: List<Point>,
                 val orientation: Orientation) {
    val parts: List<Point>
        get() {
            return listOf(position) +
                    tentacles.map { tentacle ->
                        tentacle.rotate(orientation) + position
                    }
        }
}
