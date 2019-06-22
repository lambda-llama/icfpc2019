package io.github.lambdallama.strategy

import io.github.lambdallama.ActionSink
import io.github.lambdallama.State

interface Strategy {
    fun run(state: State, sink: ActionSink)
}
