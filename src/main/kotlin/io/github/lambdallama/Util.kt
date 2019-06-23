package io.github.lambdallama

fun<E> MutableList<E>.removeLast(): E {
    return this.removeAt(this.count() - 1)
}
