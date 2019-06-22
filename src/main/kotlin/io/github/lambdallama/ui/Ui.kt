package io.github.lambdallama.ui

import io.github.lambdallama.*
import io.github.lambdallama.Point
import java.awt.*
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

private const val frameLenMs = 8

fun visualize(initialState: State, step: Boolean = false): ActionSink {
    val state = initialState.clone()
    var lastFrame = now()
    draw(state.toMap())
    return { action ->
        if (step) {
            UI.stepSemaphore.acquire()
        } else {
            val delta = now() - lastFrame
            if (delta < frameLenMs) {
                Thread.sleep(frameLenMs - delta)
            }
        }
        lastFrame = now()
        state.apply(action)
        draw(state.toMap().apply { lastAction = action.toString() })
    }
}

private fun now(): Long = System.currentTimeMillis()

private fun State.toMap(): Map {
    return Map(grid.dim,
        { p ->
            val c = grid[p]
            when (c) {
                Cell.OBSTACLE -> UiCell.WALL
                Cell.OBSTACLE -> UiCell.WALL
                Cell.FREE -> UiCell.FREE
                Cell.WRAPPED -> UiCell.WRAPPED
                Cell.VOID -> UiCell.VOID
                else -> error("bad cell")
            }

        },
        robot.getVisibleParts(grid).map { it to Pill.ROBOT } + boosters.map { it.key to it.value.toPill() },
        robot.boosters.asSequence().flatMap { generateSequence { it.key }.take(it.value) }.toList()
    )
}

fun BoosterType.toPill(): Pill = when (this) {
    BoosterType.B -> Pill.BOOST_B
    BoosterType.F -> Pill.BOOST_F
    BoosterType.L -> Pill.BOOST_L
    BoosterType.X -> Pill.BOOST_X
    BoosterType.R -> Pill.BOOST_R
    BoosterType.C -> Pill.BOOST_C
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
        val BOOST_R = Pill(6)
        val BOOST_C = Pill(7)
    }
}

class ViewState(
    var map: Map?,
    var dx: Int,
    var dy: Int,
    var lastAction: String = "N/A"
)

private class Ui {
    var stepSemaphore: Semaphore = Semaphore(1).apply { acquire() }
    private val viewState: ViewState = ViewState(null, 0, 0)

    fun modifyState(f: ViewState.() -> Unit) {
        f(viewState)
        label.text = "Last Action: ${viewState.map?.lastAction ?: "N/A"}; Boosters: ${viewState.map?.boosers.orEmpty().joinToString(", ")}"
        frame.repaint()
    }

    val label = JLabel("Last Action: N/A", Label.LEFT)

    val frame = JFrame("HelloWorldSwing").apply {
        setSize(800, 600)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val gb = GridBagLayout()
        layout = gb

        contentPane.add(label, GridBagConstraints().apply { gridy = 0; anchor = GridBagConstraints.NORTHWEST })

        val canvas = Canvas(viewState) { this.size }
        contentPane.add(canvas, GridBagConstraints().apply {
            gridy = 1; fill = GridBagConstraints.BOTH; weightx = 10.0; weighty = 10.0;
        })

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
    val pills: List<Pair<Point, Pill>>,
    var boosers: List<BoosterType> = emptyList(),
    var lastAction: String? = null
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
                Pill.BOOST_R -> Color.CYAN
                Pill.BOOST_C -> Color.ORANGE
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



