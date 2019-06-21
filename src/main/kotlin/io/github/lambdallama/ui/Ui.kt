package io.github.lambdallama.ui

import io.github.lambdallama.Point
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

fun launchGui() {
    SwingUtilities.invokeLater {

        //Display the window.
        FRAME.pack()
        FRAME.isVisible = true
    }
}

fun draw(map: Map) {
    STATE = map
    FRAME.repaint()
}

inline class Cell(private val value: Byte) {
    companion object {
        val FREE = Cell(0)
        val WALL = Cell(1)
        val WRAPPED = Cell(2)
        val VOID = Cell(3)
        val ROBOT = Cell(4)
    }
}

inline class Pill(private val value: Byte) {
    companion object {
        val ROBOT = Pill(0)
        val BOOST_B = Pill(1)
        val BOOST_F = Pill(2)
        val BOOST_L = Pill(3)
        val BOOST_X = Pill(4)
    }
}


class Map(
    val dim: Point,
    init: (Point) -> Cell = { _ -> Cell.FREE },
    val pills: List<Pair<Point, Pill>>
) {
    private val cells: Array<Cell> = Array(height * width) { i -> init(Point(i % width, i / width)) }
    operator fun get(x: Int, y: Int): Cell =
        cells[x + y * width]
}

private val Map.width: Int get() = dim.x
private val Map.height: Int get() = dim.y


private val FRAME = JFrame("HelloWorldSwing").apply {
    setSize(800, 600)
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    contentPane.add(Canvas())
}

private var STATE: Map? = null

private class Canvas : JPanel() {
    val cellSize: Int
        get() {
            val map = STATE ?: return 80
            return if (map.width < 100 && map.height < 100) 80 else 20
        }

    private val pad = 1

    override fun getPreferredSize(): Dimension {
        val map = STATE ?: return Dimension(800, 600)
        return Dimension(cellSize * map.width, cellSize * map.height)
    }

    override fun paintComponent(g: Graphics) {
        val map = STATE ?: return
        for (x in 0 until map.width) {
            for (y in 0 until map.height) {
                g.color = when (map[x, y]) {
                    Cell.WALL -> Color.BLACK
                    Cell.FREE -> Color.GRAY
                    Cell.WRAPPED -> Color.YELLOW
                    Cell.VOID -> Color.WHITE
                    Cell.ROBOT -> Color.ORANGE
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
            g.color = when (pill) {
                Pill.ROBOT -> Color.RED
                Pill.BOOST_B -> Color.MAGENTA
                Pill.BOOST_F -> Color.BLUE
                Pill.BOOST_L -> Color.GREEN
                Pill.BOOST_X -> Color.PINK
                else -> error("bad pill")
            }
            val (px, py) = map.topLeftCorner(x, y)
            if (pill == Pill.ROBOT) {
                g.fillRect(
                    px + cellSize / 4,
                    py + cellSize / 4,
                    cellSize / 2,
                    cellSize / 2
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

    fun Map.topLeftCorner(x: Int, y: Int): Pair<Int, Int> =
        x * cellSize to (height - y - 1) * cellSize
}



