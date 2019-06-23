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
        strategyRegex: Regex,
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
            phases(ExtenderPhase, GreedyUnorderedFBPartition),
            GreedyTurnoverFBPartition,
            CloneFactory,
            phases(ClonePhase, WrapDistanceCount),
            phases(CloneExtenderPhase, WrapDistanceCount),
            Weighted,
            WeightedAccelerated
    ).filter { strategyRegex.matches(it.name) } .map { strategy ->
        val name = strategy.name
        val actions = mutableListOf<List<Action>>()
        val sw = Stopwatch.createStarted()
        strategy.run(state.clone()) { actions.add(it) }
        sw.stop()
        println("$name: ${actions.size}" +
                " (${sw.toFormattedString()})")
        Triple(name, actions.size, actions)
    }.filter { it.second != 0 }

    val tempSolutionPath = path.substring(0, path.length - 5) + ".sol.tmp"
    val solutionFile = File(tempSolutionPath)
    val best = solutions.minBy { it.second }!!
    val solutionTime = best.second
    println("Best: $solutionTime (" + best.first.colorize(TerminalColors.BLUE) + ")")

    solutionFile.writeText(
            best.third.transpose().joinToString("#") { it.filterNotNull().joinToString("") })

    if (validate) {
        if (!validateSolution(path, tempSolutionPath, solutionTime)) {
            return
        }
    }

    val metadata = SolutionMetadata.parse(path.substring(0, path.length - 5) + ".meta")

    solutions.forEach { (strategy, time, _) ->
        val oldTime = metadata.getTime(strategy)
        when {
            oldTime == null ->
                metadata.setTime(strategy, time)
            time > oldTime  ->
                println("WARNING: efficiency degradation for $strategy ($oldTime => $time)".colorize(TerminalColors.YELLOW))
            time < oldTime -> {
                println("Efficiency improvement for $strategy ($oldTime => $time)".colorize(TerminalColors.GREEN))
                metadata.setTime(strategy, time)
            }
        }
    }

    when {
        solutionTime > metadata.bestTime ->
            println(("WARNING: efficiency degradation for best solution (${metadata.bestTime} => $solutionTime)," +
                " NOT replacing the solution file").colorize(TerminalColors.YELLOW))
        solutionTime < metadata.bestTime -> {
            println(("Efficiency improvement for best solution (${metadata.bestTime} => $solutionTime), " +
                    "replacing the solution file").colorize(TerminalColors.GREEN))
            if (!validate) {
                // Validate anyway, maybe have an extra flag to never ever validate?
                if (!validateSolution(path, tempSolutionPath, solutionTime)) {
                    return
                }
            }
            val solutionPath = path.substring(0, path.length - 5) + ".sol"
            Files.move(Paths.get(tempSolutionPath), Paths.get(solutionPath), StandardCopyOption.REPLACE_EXISTING)
            metadata.bestTime = solutionTime
        }
        else ->
            // TODO: maybe check if the files are different and keep if they are?
            Files.delete(Paths.get(tempSolutionPath))
    }
    metadata.saveToDisk()
}

fun validateSolution(path: String, solutionPath: String, solutionTime: Int): Boolean {
    print("Validating... ")
    val sw = Stopwatch.createStarted()
    val validationResult = validateWithJsValidator(path, solutionPath)
    sw.stop()
    return when (validationResult) {
        is JsValidatorResult.Success -> {
            println("OK, ${validationResult.time} (${sw.toFormattedString()})".colorize(TerminalColors.GREEN))
            check(solutionTime == validationResult.time)
            true
        }
        is JsValidatorResult.Failure -> {
            println("ERROR: ${validationResult.error} (${sw.toFormattedString()})".colorize(TerminalColors.RED))
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
            var strategyRegex = ".*"
            val extra = args.dropLast(1).toMutableList()
            while (extra.isNotEmpty()) {
                when (extra.removeAt(0)) {
                    "--validate" -> validate = true
                    "--show_bonus_count" ->  showBonusCount = true
                    "--info_only" -> infoOnly = true
                    "--strategy" -> {
                        strategyRegex = extra.removeAt(0)
                    }
                }
            }
            return nonInteractiveMain(
                    path=args.last(),
                    validate=validate,
                    showBonusCount = showBonusCount,
                    infoOnly = infoOnly,
                    strategyRegex = Regex(strategyRegex, RegexOption.IGNORE_CASE)
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
    WrapDistanceCount.run(state, visualize(state, true))
}
