package com.mahout.app.domain.elephant.model

data class MoodTrends(
    val last7Count: Map<String, Int>,
    val lifetimeCount: Map<String, Int>,
    val last7Total: Int,
    val lifetimeTotal: Int
)



