package io.github.lambdallama

fun bonusCount(state: State): Map<BoosterType, Int> {
    val count = HashMap<BoosterType, Int>()
    for (booster in state.boosters.values){
        count[booster] = (count[booster] ?: 0) + 1
    }
    return count
}