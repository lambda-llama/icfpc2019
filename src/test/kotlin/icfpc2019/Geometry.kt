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
}

class OrientationTest {
    @Test
    fun testDirections() {
        assertEquals(Point(1, 1).rotate(Orientation.UP), actual = Point(-1, 1))
    }
}