package icfpc2019

import io.github.lambdallama.Orientation
import io.github.lambdallama.Point
import io.github.lambdallama.Robot
import org.junit.Test
import kotlin.test.assertEquals

class TestRobot {
    @Test
    fun testRotation() {
        val robot = Robot(
                position = Point(1, 1),
                tentacles = listOf(Point(1, 0), Point(1, 1), Point(1, -1)),
                orientation = Orientation.UP
        )
        assertEquals(
                listOf(Point(1, 1), Point(1, 2), Point(0, 2), Point(2, 2)),
                robot.parts
        )
    }
}