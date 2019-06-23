package io.github.lambdallama.strategy

import io.github.lambdallama.ActionSink
import io.github.lambdallama.State

interface Strategy {
    fun run(state: State, sink: ActionSink)

    val name: String get() = javaClass.simpleName
}

fun phases(s1: Strategy, s2: Strategy, name: String? = null): Strategy = object : Strategy {
    override val name: String
        get() = name ?: "Phases<${s1.name}, ${s2.name}>"

    override fun run(state: State, sink: ActionSink) {
        s1.run(state, sink)
        s2.run(state, sink)
    }
}