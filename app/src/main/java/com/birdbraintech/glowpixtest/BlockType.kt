package com.birdbraintech.glowpixtest

enum class BlockType {
    start, addition, addition1, addition10, doubleAddition, subtraction, subtraction1, subtraction10, multiplication, division, equals;

    val mathOperator: String
        get() = when (this) {
            addition, addition1, addition10, doubleAddition -> "+"
            subtraction, subtraction1, subtraction10 -> "−"
            multiplication -> "×"
            division -> "÷"
            else -> ""
        }
}