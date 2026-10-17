package com.yashd.assignment2

internal fun readPriority(value: Any?): Int = when (value) {
    true -> 1
    is Number -> value.toInt().coerceIn(0, 2)
    else -> 0
}

internal fun priorityLabel(level: Int): String = when (level) {
    2 -> "Super important"
    1 -> "Important"
    else -> "None"
}
