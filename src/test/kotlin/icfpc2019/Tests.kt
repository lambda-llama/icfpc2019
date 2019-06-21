package io.github.lambdallama

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTest {
    @Test fun testPackUnpack() {
        val p = Point(42, 24)
        assertEquals(p.x, 42)
        assertEquals(p.y, 24)
    }
}