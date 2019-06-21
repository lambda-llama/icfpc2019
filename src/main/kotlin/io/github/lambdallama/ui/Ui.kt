package io.github.lambdallama.ui

import io.github.lambdallama.*
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.Semaphore
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

fun launchGui() {
    SwingUtilities.invokeLater {
        UI.launch()
    }
}

fun draw(newMap: Map) {
    UI.modifyState {
        map = newMap
    }
}

private const val frameLenMs = 300

fun visualize(initialState: State, step: Boolean = false): ActionSink {
    var state = initialState
    var lastFrame = now()
    draw(state.toMap())
    return { action ->
        println("action = ${action}")
        if (step) {
            UI.stepSemaphore.acquire()
        } else {
            val delta = now() - lastFrame
            if (delta < frameLenMs) {
                Thread.sleep(frameLenMs - delta)
            }
        }
        lastFrame = now()

        if (action is Move) {
            state = state.copy(robot = state.robot.copy(position = state.robot.position.move(action)))
        }
        draw(initialState.toMap())
    }
}
private fun now(): Long  = System.currentTimeMillis()

private fun Point.move(move: Move): Point {
    return Point(x + move.dx, y + move.dy)
}

private fun State.toMap(): Map {
    val pills = mutableListOf<Pair<Point, Pill>>()
    return Map(grid.dim,
        { p ->
            val c = grid[p]
            if (robot.parts.contains(p)) {
                pills += p to Pill.ROBOT
            }
            when (c) {
                Cell.OBSTACLE -> UiCell.WALL
                Cell.OBSTACLE -> UiCell.WALL
                Cell.FREE -> UiCell.FREE
                Cell.WRAPPED -> UiCell.WRAPPED
                Cell.VOID -> UiCell.VOID
                else -> {
                    when (c) {
                        Cell.B_EXTENSION -> pills += p to Pill.BOOST_B
                        Cell.B_DRILL -> pills += p to Pill.BOOST_L
                        Cell.B_FAST_WHEELS -> pills += p to Pill.BOOST_F
                        Cell.B_MYSTERIOUS_POINT -> pills += p to Pill.BOOST_X
                    }
                    UiCell.FREE
                }
            }

        }, pills)
}


inline class UiCell(private val value: Byte) {
    companion object {
        val FREE = UiCell(0)
        val WALL = UiCell(1)
        val WRAPPED = UiCell(2)
        val VOID = UiCell(3)
        val ROBOT = UiCell(4)
    }
}

inline class Pill(private val value: Byte) {
    companion object {
        val ROBOT = Pill(0)
        val BOOST_B = Pill(1)
        val BOOST_F = Pill(2)
        val BOOST_L = Pill(3)
        val BOOST_X = Pill(4)
        val BOOST_T = Pill(5)
    }
}

class ViewState(
    var map: Map?,
    var dx: Int,
    var dy: Int
)

private class Ui {
    var stepSemaphore: Semaphore = Semaphore(1)
    private val viewState: ViewState = ViewState(null, 0, 0)

    fun modifyState(f: ViewState.() -> Unit) {
        f(viewState)
        frame.repaint()
    }

    val frame = JFrame("HelloWorldSwing").apply {
        setSize(800, 600)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        contentPane.add(Canvas(viewState) { this.size })
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {}

            override fun keyPressed(e: KeyEvent) {
                modifyState {
                    when (e.keyCode) {
                        KeyEvent.VK_LEFT -> dx -= 10
                        KeyEvent.VK_RIGHT -> dx += 10
                        KeyEvent.VK_UP -> dy -= 10
                        KeyEvent.VK_DOWN -> dy += 10
                        KeyEvent.VK_SPACE -> stepSemaphore.release()
                    }

                }
            }

            override fun keyReleased(e: KeyEvent?) {
            }
        })
    }

    fun launch() {
        frame.pack()
        frame.isVisible = true
    }
}

class Map(
    val dim: Point,
    init: (Point) -> UiCell = { _ -> UiCell.FREE },
    val pills: List<Pair<Point, Pill>>
) {
    private val cells: Array<UiCell> = Array(height * width) { i -> init(Point(i % width, i / width)) }
    operator fun get(x: Int, y: Int): UiCell =
        cells[x + y * width]
}

private val Map.width: Int get() = dim.x
private val Map.height: Int get() = dim.y

private val UI = Ui()

private class Canvas(val viewState: ViewState, val frameSize: () -> Dimension) : JPanel() {
    val cellSize: Int
        get() {
            val map = viewState.map ?: return 80
            val size = min(
                frameSize().width / map.width - 1,
                frameSize().height / map.height - 1
            )
            return min(max(size, 5), 80)
        }

    private val pad = 1

    override fun getPreferredSize(): Dimension {
        val map = viewState.map ?: return Dimension(800, 600)
        return Dimension(cellSize * map.width, cellSize * map.height)
    }

    override fun paintComponent(g: Graphics) {
        g.translate(viewState.dx, viewState.dy)
        val map = viewState.map ?: return
        for (x in 0 until map.width) {
            for (y in 0 until map.height) {
                g.color = when (map[x, y]) {
                    UiCell.WALL -> Color.DARK_GRAY
                    UiCell.FREE -> Color.WHITE
                    UiCell.WRAPPED -> Color.YELLOW
                    UiCell.VOID -> Color.GRAY
                    UiCell.ROBOT -> Color.ORANGE
                    else -> kotlin.error("bad cell")
                }
                val (px, py) = map.topLeftCorner(x, y)
                g.fillRect(
                    px + pad,
                    py + pad,
                    cellSize - pad * 2,
                    cellSize - pad * 2
                )
            }
        }

        for ((point, pill) in map.pills) {
            val (x, y) = point
            val color = when (pill) {
                Pill.ROBOT -> Color.RED
                Pill.BOOST_B -> Color.YELLOW
                Pill.BOOST_F -> Color(210, 105, 30)
                Pill.BOOST_L -> Color.GREEN
                Pill.BOOST_X -> Color.BLUE
                Pill.BOOST_T -> Color.MAGENTA
                else -> error("bad pill")
            }
            g.color = color
            val (px, py) = map.topLeftCorner(x, y)
            if (pill == Pill.ROBOT) {
                g.fillRect(
                    px + cellSize / 4,
                    py + cellSize / 4,
                    cellSize / 2,
                    cellSize / 2
                )
            } else {
                if (cellSize >= 10) {
                    g.color = Color.BLACK
                    g.fillOval(
                        px + cellSize / 4,
                        py + cellSize / 4,
                        cellSize / 2,
                        cellSize / 2
                    )
                    g.color = color
                    g.fillOval(
                        px + cellSize / 4 + 2,
                        py + cellSize / 4 + 2,
                        cellSize / 2 - 4,
                        cellSize / 2 - 4
                    )
                } else {
                    g.fillOval(
                        px + cellSize / 4,
                        py + cellSize / 4,
                        cellSize / 2,
                        cellSize / 2
                    )

                }
            }
        }
    }

    fun Map.topLeftCorner(x: Int, y: Int): Pair<Int, Int> =
        x * cellSize to (height - y - 1) * cellSize
}



