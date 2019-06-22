package io.github.lambdallama

import io.github.lambdallama.strategy.*
import io.github.lambdallama.ui.launchGui
import io.github.lambdallama.ui.visualize
import java.io.File

fun nonInteractiveMain(
        path: String,
        validate: Boolean,
        showBonusCount: Boolean,
        infoOnly: Boolean
) {
    val state = State.parse(File(path).readText())
    println("Map: $path, dim: ${state.grid.dim}, max points: ${state.maxPoints}")
    if (showBonusCount) {
        println("Bonus count: ${bonusCount(state)}")
    }
    if (infoOnly) {
        return
    }

    val solutions = arrayOf(
            NaiveIterative,
            GreedyUnordered,
            GreedyUnorderedTurnover,
            GreedyUnorderedFBPartition,
            GreedyTurnoverFBPartition,
            CloneFactory,
            WrapDistanceCount
    ).map { strategy ->
        when (strategy) {
            GreedyUnorderedFBPartition, GreedyTurnoverFBPartition ->
                if (state.grid.dim.x >= 100 || state.grid.dim.y >= 100) {
                    return@map mutableListOf<List<Action?>>()
                }
            CloneFactory ->
                // TODO(superbobry): HACK HACK HACK.
                if ("clone" !in path) {
                    return@map mutableListOf<List<Action?>>()
                }
        }

        val actions = mutableListOf<List<Action?>>()
        strategy.run(state.clone()) { actions.add(it) }
        System.err.println("${strategy.javaClass.simpleName}: ${actions.size}")
        actions
    }.filter { it.isNotEmpty() }

    val solutionPath = path.substring(0, path.length - 5) + ".sol"
    val solutionFile = File(solutionPath)
    val best: List<List<Action?>> = solutions.minBy { it.size }!!.transpose()
    solutionFile.writeText(
        best.joinToString("#") { it.filterNotNull().joinToString("") })

    if (validate) {
        print("Validating... ")
        when (val validationResult = validateWithJsValidator(path, solutionPath)) {
            is JsValidatorResult.Success -> println("OK, ${validationResult.time}")
            is JsValidatorResult.Failure -> println("ERROR: ${validationResult.error}")
        }
    }
}

fun<T> List<List<T>>.transpose(): List<List<T?>> {
    val n = map { it.size}.max()!!
    val res = (0 until n).map { mutableListOf<T?>() }
    for (xs in this) {
        for (i in 0 until n) {
            res[i] += xs.getOrNull(i)
        }
    }

    return res
}

fun main(args: Array<String>) {
    when (args[0]) {
        "--non-interactive" -> {
            var validate = false
            var showBonusCount = false
            var infoOnly = false
            for (arg in args.dropLast(1)) {
                when (arg) {
                    "--validate" -> validate = true
                    "--show_bonus_count" ->  showBonusCount = true
                    "--info_only" -> infoOnly = true
                }
            }
            return nonInteractiveMain(
                    path=args.last(),
                    validate=validate,
                    showBonusCount = showBonusCount,
                    infoOnly = infoOnly
            )
        }
        "--generate" -> {
            val path = args.drop(1).firstOrNull() ?: throw IllegalStateException(
                    "path/to/block/XX/puzzle.cond")
            return generatorMain(path)
        }
    }

    // ./gradlew run --args=path/to/prob-XXX.desc
    val path = args.firstOrNull() ?: throw IllegalStateException(
        "path/to/prob-XXX.desc")
    val state = State.parse(File(path).readText())

    launchGui()
    ClonePhase.run(state, visualize(state, true))
    WrapDistanceCount.run(state, visualize(state, true))
}
