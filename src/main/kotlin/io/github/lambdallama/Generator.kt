package io.github.lambdallama

import io.github.lambdallama.ui.launchGui
import io.github.lambdallama.ui.visualize
import java.io.File

fun generatorMain(path: String) {
    val puzzle = File(path).readText()
    val (rawParams, rawInclude, rawExclude) = puzzle.split('#')
    val paramsList = rawParams.split(',')
    assert(paramsList.size == 11)

    // `split` unpacking doesn't work with more than 8 values.
    var i = 0
    val bNum = paramsList[i++].toInt()
    val eNum = paramsList[i++].toInt()
    val tSize = paramsList[i++].toInt()
    val vMin = paramsList[i++].toInt()
    val vMax = paramsList[i++].toInt()

    val mNum = paramsList[i++].toInt()
    val fNum = paramsList[i++].toInt()
    val dNum = paramsList[i++].toInt()
    val rNum = paramsList[i++].toInt()
    val cNum = paramsList[i++].toInt()
    val xNum = paramsList[i++].toInt()

    val boosterCount = HashMap<BoosterType, Int>()
    boosterCount[BoosterType.B] = mNum
    boosterCount[BoosterType.F] = fNum
    boosterCount[BoosterType.L] = dNum
    boosterCount[BoosterType.R] = rNum
    boosterCount[BoosterType.C] = cNum
    boosterCount[BoosterType.X] = xNum

    val includePoints = parseRepSepPoints(rawInclude, ',')
    val excludePoints = parseRepSepPoints(rawExclude, ',')

    var robot = Robot(Point(0, 0), mutableListOf(), Orientation.RIGHT)
    var state = State(
            ByteMatrix(tSize, tSize, Cell.FREE),
            mutableMapOf(), mutableListOf(), mutableMapOf()
    )
    for (p in includePoints) {
        state.grid[p] = Cell.WRAPPED
    }
    for (p in excludePoints) {
        state.grid[p] = Cell.VOID
    }
    launchGui()
    visualize(state, true)
}