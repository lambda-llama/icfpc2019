package io.github.lambdallama

fun parseRepSepPoints(s: String, sep: Char): List<Point> {
    val contour = mutableListOf<Point>()
    var offset = 0
    while (offset < s.length) {
        val i = s.indexOf('(', offset)
        val j = s.indexOf(')', i + 1)
        contour.add(Point.parse(s.slice(i..j)))
        offset = j + 1
        check(offset == s.length || s[offset] == sep)
        offset += 1
    }
    return contour
}