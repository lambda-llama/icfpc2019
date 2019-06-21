package io.github.lambdallama

const val DIR_NAMES = "AWDS"

sealed class Action

data class Move(
    val direction: Int // [0, 4)
) : Action() {
    override fun toString() = DIR_NAMES[direction].toString()
}

data class Turn(
    val direction: Int // {-1, +1}
) : Action() {
    override fun toString() = if (direction == 1) "E" else "Q"
}

data class Attach(val location: Point) : Action() {
    override fun toString() = "B$location"
}

object Accelerate : Action() {
    override fun toString() = "F"
}

object Drill : Action() {
    override fun toString() = "L"
}

class Teleport(private val location: Point) : Action() {
    override fun toString() = "R$location"
}

object NoOp : Action() {
    override fun toString() = "Z"
}