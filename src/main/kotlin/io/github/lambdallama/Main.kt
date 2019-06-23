package io.github.lambdallama

import com.google.common.base.Stopwatch
import io.github.lambdallama.strategy.*
import io.github.lambdallama.ui.launchGui
import io.github.lambdallama.ui.visualize
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

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
//            GreedyUnorderedFBPartition, GreedyTurnoverFBPartition ->
//                if (state.grid.dim.x >= 100 || state.grid.dim.y >= 100) {
//                    return@map mutableListOf<List<Action?>>()
//                }
            CloneFactory ->
                // TODO(superbobry): HACK HACK HACK.
                if ("clone" !in path) {
                    return@map listOf<List<Action>>()
                }
        }

        val actions = mutableListOf<List<Action>>()
        val sw = Stopwatch.createStarted()
        strategy.run(state.clone()) { actions.add(it) }
        sw.stop()
        System.err.println("${strategy.javaClass.simpleName}: ${actions.size}" +
                " (${sw.toFormattedString()})")
        actions
    }.filter { it.isNotEmpty() }

    val tempSolutionPath = path.substring(0, path.length - 5) + ".sol.tmp"
    val solutionFile = File(tempSolutionPath)
    val best = solutions.minBy { it.size }!!
    val solutionTime = best.size
    solutionFile.writeText(
            best.transpose().joinToString("#") { it.filterNotNull().joinToString("") })

    if (validate) {
        if (!validateSolution(path, tempSolutionPath, solutionTime)) {
            return
        }
    }

    val metadata = SolutionMetadata.parse(path.substring(0, path.length - 5) + ".meta")
    when {
        solutionTime > metadata.bestTime ->
            println("WARNING: efficiency degradation ($solutionTime > ${metadata.bestTime})," +
                " NOT replacing the solution file")
        solutionTime < metadata.bestTime -> {
            println("Efficiency improvement ($solutionTime < ${metadata.bestTime}), " +
                    "replacing the solution file")
            if (!validate) {
                // Validate anyway, maybe have an extra flag to never ever validate?
                if (!validateSolution(path, tempSolutionPath, solutionTime)) {
                    return
                }
            }
            val solutionPath = path.substring(0, path.length - 5) + ".sol"
            Files.move(Paths.get(tempSolutionPath), Paths.get(solutionPath), StandardCopyOption.REPLACE_EXISTING)
            metadata.bestTime = solutionTime
            metadata.saveToDisk()
        }
        else ->
            // TODO: maybe check if the files are different and keep if they are?
            Files.delete(Paths.get(tempSolutionPath))
    }
}

fun validateSolution(path: String, solutionPath: String, solutionTime: Int): Boolean {
    print("Validating... ")
    val sw = Stopwatch.createStarted()
    val validationResult = validateWithJsValidator(path, solutionPath)
    sw.stop()
    return when (validationResult) {
        is JsValidatorResult.Success -> {
            println("OK, ${validationResult.time} (${sw.toFormattedString()})")
            check(solutionTime == validationResult.time)
            true
        }
        is JsValidatorResult.Failure -> {
            println("ERROR: ${validationResult.error} (${sw.toFormattedString()})")
            false
        }
    }
}

fun Stopwatch.toFormattedString(): String {
    return "${"%.3f".format(this.elapsed(TimeUnit.MILLISECONDS).toDouble() / 1000)}s"
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
    ClonePhase.run(state, visualize(state, false))
    WrapDistanceCount.run(state, visualize(state, true))
}
