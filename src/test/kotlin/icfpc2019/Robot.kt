package icfpc2019

import io.github.lambdallama.*
import org.junit.Test
import kotlin.test.assertEquals

class TestRobot {
    @Test
    fun testRotation() {
        val robot = Robot(
                position = Point(2, 1),
                tentacles = mutableListOf(Point(1, 0), Point(1, 1), Point(1, -1), Point(1, 2)),
                orientation = Orientation.UP
        )
        assertEquals(
                listOf(Point(2, 1), Point(2, 2), Point(1, 2), Point(3, 2), Point(0, 2)),
                robot.getVisibleParts(ByteMatrix(4, 4, Cell.FREE))
        )
    }

    @Test
    fun testVisibleParts() {
        // pPPWp
        //  WR
        //
        // p - invisible part
        // P - visible part
        // R - robot
        // W - wall

        val robot = Robot(
                position = Point(2, 0),
                tentacles = mutableListOf(Point(1, 0), Point(1, 1), Point(1, -1),
                        Point(1, 2), Point(1, -2)),
                orientation = Orientation.UP
        )
        val grid = ByteMatrix(2, 5, Cell.FREE)
        grid[Point(1, 0)] = Cell.OBSTACLE
        grid[Point(3, 1)] = Cell.OBSTACLE
        assertEquals(
                listOf(Point(2, 0), Point(2, 1), Point(1, 1)),
                robot.getVisibleParts(grid)
        )
    }
}
