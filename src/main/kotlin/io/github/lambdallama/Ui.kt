package io.github.lambdallama

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.paint.Color
import tornadofx.*
import kotlin.concurrent.thread

class UI : View() {
    init {
        println("launching")
        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: UI
    }

    val cellSize = 40.0
    private val pad = 2.0
    val map = SimpleObjectProperty<Map?>(null)

    override val root = pane {
        val map = map.get() ?: return@pane
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                rectangle {
                    x = col * cellSize + pad
                    y = row * cellSize + pad
                    width = cellSize - pad * 2
                    height = cellSize - pad * 2
                    fill = when (map[row, col]) {
                        WALL -> Color.GRAY
                        EMPTY -> Color.GRAY
                        WRAPPED -> Color.BLUEVIOLET
                        else -> kotlin.error("bad state")

                    }
                }
            }
        }
    }
}

class UIApp : App(UI::class, Styles::class)

class Styles : Stylesheet() {
    init {
        label {
            fontSize = 20.px
        }
    }
}

fun startUI() {
    thread {
        launch<UIApp>()
    }
}

fun setMap(map: Map) {
    println("setMap")
    UI.INSTANCE.map.set(map)
}


typealias State = Byte

const val EMPTY: State = 0
const val WALL: State = 1
const val WRAPPED: State = 2

class Map(
    val width: Int,
    val height: Int,
    init: (Int, Int) -> State = { _, _ -> EMPTY }
) {
    private val cells: Array<State> = Array(width * height) { i -> init(i / width, i % width) }
    operator fun get(row: Int, col: Int): State =
        cells[row + col * width]
}