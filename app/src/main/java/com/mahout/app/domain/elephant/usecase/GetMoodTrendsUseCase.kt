package com.mahout.app.domain.elephant.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.elephant.model.MoodLog
import com.mahout.app.domain.elephant.model.MoodTrends
import com.mahout.app.domain.elephant.repository.MoodLogRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetMoodTrendsUseCase @Inject constructor(
    private val repo: MoodLogRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(zone: ZoneId = ZoneId.systemDefault()): MoodTrends {
        val now = timeProvider.nowInstant()
        val today = now.atZone(zone).toLocalDate()
        val sevenDaysAgo = today.minusDays(6).atStartOfDay(zone).toInstant()
        val tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant()

        val last7 = repo.getBetween(sevenDaysAgo, tomorrow)
        val lifetime = repo.getAll()

        fun count(list: List<MoodLog>): Pair<Map<String, Int>, Int> {
            val map = list.groupingBy { it.feeling }.eachCount()
            val total = list.size
            return map to total
        }

        val (last7Count, last7Total) = count(last7)
        val (lifeCount, lifeTotal) = count(lifetime)

        return MoodTrends(
            last7Count = last7Count,
            lifetimeCount = lifeCount,
            last7Total = last7Total,
            lifetimeTotal = lifeTotal
        )
    }
}



