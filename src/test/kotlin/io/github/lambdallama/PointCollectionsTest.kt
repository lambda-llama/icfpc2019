package io.github.lambdallama

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PointSetTest {
    @Test fun testWholeGrid() {
        val ps = PointSet(Point(3, 4))

        for (x in 0 until ps.dim.x) {
            for (y in 0 until ps.dim.y) {
                val p = Point(x, y)
                assertFalse(p in ps)
                ps.add(p)
                assertTrue(p in ps)
            }
        }

        assertEquals(ps.asSequence().toSet().size, ps.dim.x * ps.dim.y)
    }

    @Test fun testAddContains() {
        val ps = PointSet(Point(3, 4))
        val p = Point(1, 2)
        assertFalse(p in ps)
        ps.add(p)
        assertTrue(p in ps)
    }

    @Test fun testAsSequenceEmpty() {
        val ps = PointSet(Point(3, 4))
        assertEquals(ps.asSequence().count(), 0)
    }

    @Test fun testAsSequence() {
        val p1 = Point(1, 2)
        val p2 = Point(0, 3)
        val ps = PointSet(Point(3, 4)).apply {
            add(p1)
            add(p2)
        }

        assertEquals(ps.asSequence().toList(), listOf(p1, p2))
    }
}

class PointIntMapTest {
    @Test fun testWholeGrid() {
        val ps = PointIntMap(Point(3, 4))
        for (x in 0 until ps.dim.x) {
            for (y in 0 until ps.dim.y) {
                val p = Point(x, y)
                assertFalse(p in ps)
                ps[p] = 42
                assertEquals(ps[p], 42)
            }
        }
    }

    @Test fun testGetOrDefault() {
        val ps = PointIntMap(Point(3, 4))
        val p = Point(1, 2)
        assertFalse(p in ps)
        assertEquals(ps.getOrDefault(p, 42), 42)
        assertFalse(p in ps)
        ps[p] = 24
        assertEquals(ps.getOrDefault(p, 42), 24)
    }
}