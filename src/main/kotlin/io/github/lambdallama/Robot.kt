package io.github.lambdallama

data class Robot(val position: Point, val tentacles: List<Point>) {
    val parts get() = arrayOf(position) + tentacles.map{tentacle -> tentacle + position}
}
