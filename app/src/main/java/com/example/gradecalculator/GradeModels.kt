package com.example.gradecalculator

data class Subject(
    val id: String,
    val name: String,
    val grades: List<Grade> = emptyList(),
    val coefficient: Float = 1.0f
) {
    val average: Float
        get() = if (grades.isEmpty()) 0f else grades.map { it.value }.average().toFloat()
}

data class Grade(
    val id: String,
    val value: Float,
    val coefficient: Float = 1.0f,
    val date: Long = System.currentTimeMillis()
)
