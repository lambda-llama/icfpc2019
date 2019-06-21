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
            if (map.width < 20 && map.height < 20) return 80
            if (map.width < 80 && map.height < 80) return 40
            return 20
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
                Pill.BOOST_F -> Color(210,105,30)
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
            }
        }
    }

    fun Map.topLeftCorner(x: Int, y: Int): Pair<Int, Int> =
        x * cellSize to (height - y - 1) * cellSize
}



