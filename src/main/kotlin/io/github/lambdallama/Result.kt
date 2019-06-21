package io.github.lambdallama

sealed class Action
data class Move(
        val direction: Int // [0, 4)
): Action() {
    override fun toString(): String {
        return DIR_NAMES[direction].toString()
    }
}
data class Turn(
        val direction: Int // {-1, +1}
): Action() {
    override fun toString(): String {
        return if (direction == 1) "E" else "Q"
    }
}
data class Attach(val location: Point): Action() {
    override fun toString(): String {
        return "B(${location.x}, ${location.y})"
    }
}
class Accelerate(): Action() {
    override fun toString(): String {
        return "F"
    }
}
class Drill(): Action() {
    override fun toString(): String {
        return "L"
    }
}
class NoOp(): Action() {
    override fun toString(): String {
        return "Z"
    }
}
