package com.birdbraintech.glowpixtest
// The evaluate() function evaluates the equation and returns one of four possible values:
// incomplete if any part of the expression is blank, incorrect if the answer is wrong,
// offGlowBoard if the answer is right but can't be displayed on the GlowBoard,
// and correct if the answer is right and can be displayed on the GlowBoard
enum class EvaluationOptions {
    correct, incorrect, incomplete, offGlowBoard
}