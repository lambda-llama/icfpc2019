package io.github.lambdallama

object TerminalColors {
    private const val ESCAPE = 27.toChar()

    const val GREEN = "$ESCAPE[32m"
    const val YELLOW = "$ESCAPE[33m"
    const val RED = "$ESCAPE[31m"
    const val DEFAULT = "$ESCAPE[39m"
}

fun String.colorize(color: String): String {
    return "$color$this${TerminalColors.DEFAULT}"
}
