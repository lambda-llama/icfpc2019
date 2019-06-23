package io.github.lambdallama

fun<E> MutableList<E>.removeLast(): E {
    return this.removeAt(this.count() - 1)
}

fun<K, V> Map<K, V>.invert(): Map<V, List<K>> {
    val result = mutableMapOf<V, List<K>>()
    this.forEach { (k, v) ->
        val list = result.getOrPut(v, { mutableListOf() }) as MutableList<K>
        list.add(k)
    }
    return result
}
