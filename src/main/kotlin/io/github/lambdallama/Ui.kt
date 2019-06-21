package io.github.lambdallama

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


typealias Cell = Byte

class Map(
    val width: Int,
    val height: Int,
    init: (Int, Int) -> Cell = { _, _ -> FREE }
) {
    private val cells: Array<Cell> = Array(width * height) { i -> init(i / width, i % width) }
    operator fun get(row: Int, col: Int): Cell =
        cells[row + col * width]
}

private val FRAME = JFrame("HelloWorldSwing").apply {
    setSize(800, 600)
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    contentPane.add(Canvas())
}

private var STATE = Map(10, 10) { _, _ -> FREE }

private class Canvas : JPanel() {
    val cellSize = 40
    private val pad = 2

    override fun getPreferredSize(): Dimension = Dimension(800, 600)
    override fun paintComponent(g: Graphics) {
        g.color = Color.CYAN
        g.drawRect(10, 10, 100, 100)
        val map= STATE
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                g.color = when (map[row, col]) {
                    OBSTACLE -> Color.BLACK
                    FREE -> Color.GRAY
                    WRAPPED -> Color.YELLOW
                    VOID -> Color.WHITE
                    else -> kotlin.error("bad state")
                }
                g.fillRect(
                    col * cellSize + pad,
                    row * cellSize + pad,
                    cellSize - pad * 2,
                    cellSize - pad * 2
                )
            }
        }

    }
}



