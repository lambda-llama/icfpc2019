package io.github.lambdallama

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTest {
    @Test
    fun testPackUnpack() {
        val p = Point(42, 24)
        assertEquals(p.x, 42)
        assertEquals(p.y, 24)
    }

    @Test
    fun testPlus() {
        val p = Point(1, 1)
        val q = Point(-1, 1)
        assertEquals(Point(0, 2), p + q)
    }

    @Test fun testRange() {
        for (x in -200..200) {
            for (y in -200..200) {
                val p = Point(x, y)
                assertEquals(p.x, x)
                assertEquals(p.y, y)
            }
        }
    }
}

class OrientationTest {
    @Test
    fun testDirections() {
        assertEquals(Point(1, 1).rotate(Orientation.UP), actual = Point(-1, 1))
    }

    @Test
    fun testRotate() {
        var orientation = Orientation.RIGHT

        orientation = orientation.rotate(Rotation.COUNTERCLOCKWISE)
        assertEquals(Orientation.UP, orientation)
        orientation = orientation.rotate(Rotation.COUNTERCLOCKWISE)
        assertEquals(Orientation.LEFT, orientation)
        orientation = orientation.rotate(Rotation.COUNTERCLOCKWISE)
        assertEquals(Orientation.DOWN, orientation)
        orientation = orientation.rotate(Rotation.COUNTERCLOCKWISE)
        assertEquals(Orientation.RIGHT, orientation)

        orientation = orientation.rotate(Rotation.CLOCKWISE)
        assertEquals(Orientation.DOWN, orientation)
        orientation = orientation.rotate(Rotation.CLOCKWISE)
        assertEquals(Orientation.LEFT, orientation)
        orientation = orientation.rotate(Rotation.CLOCKWISE)
        assertEquals(Orientation.UP, orientation)
        orientation = orientation.rotate(Rotation.CLOCKWISE)
        assertEquals(Orientation.RIGHT, orientation)
    }
}
