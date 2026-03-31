package com.example.gradecalculator

object GradeCalculator {
    fun calculateGrade(score: Float): String {
        return when (score.toInt()) {
            in 80..100 -> "A"
            in 70..79 -> "B+"
            in 60..69 -> "B"
            in 55..59 -> "C+"
            in 50..54 -> "C"
            in 45..49 -> "D+"
            in 40..44 -> "D"
            in 0..39 -> "F"
            else -> "N/A"
        }
    }
}
