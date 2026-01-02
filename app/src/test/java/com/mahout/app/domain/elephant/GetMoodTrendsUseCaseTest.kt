package com.mahout.app.domain.elephant

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.elephant.model.MoodLog
import com.mahout.app.domain.elephant.repository.MoodLogRepository
import com.mahout.app.domain.elephant.usecase.GetMoodTrendsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.io.File

class GetMoodTrendsUseCaseTest {

    @Test
    fun `counts last7 and lifetime`() = runBlocking {
        val now = Instant.parse("2025-12-28T10:00:00Z")
        val repo = FakeMoodRepo()
        val tp = object : TimeProvider {
            override fun nowInstant(): Instant = now
        }
        // last 7 days
        repeat(3) { repo.add("happy", now.minusSeconds((it * 3600).toLong())) }
        // older than 7 days
        repo.add("sad", now.minusSeconds(9 * 24 * 3600L))

        val useCase = GetMoodTrendsUseCase(repo, tp)
        val trends = useCase(ZoneId.of("UTC"))

        assertEquals(3, trends.last7Total)
        assertEquals(4, trends.lifetimeTotal)
        assertEquals(3, trends.last7Count["happy"])
        assertEquals(1, trends.lifetimeCount["sad"])
    }

    private class FakeMoodRepo : MoodLogRepository {
        private val items = mutableListOf<MoodLog>()
        override fun observeMoodLogs() = throw NotImplementedError()
        override suspend fun getAll(): List<MoodLog> = items.toList()
        override suspend fun getBetween(from: Instant, to: Instant): List<MoodLog> =
            items.filter { it.at >= from && it.at <= to }

        override suspend fun upsert(log: MoodLog) {
            items.removeAll { it.id == log.id }
            items.add(log)
        }

        override suspend fun deleteById(id: String) {
            val removed = items.removeAll { it.id == id }
            // #region agent log
            try {
                File("c:\\Users\\raych\\AndroidStudioProjects\\Mahout\\.cursor\\debug.log")
                    .appendText(
                        """{"sessionId":"debug-session","runId":"post-fix","hypothesisId":"H1","location":"GetMoodTrendsUseCaseTest.FakeMoodRepo.deleteById","message":"deleteById called","data":{"id":"$id","removed":$removed},"timestamp":${System.currentTimeMillis()}}""" + "\n"
                    )
            } catch (_: Exception) { }
            // #endregion
        }

        fun add(feeling: String, at: Instant) {
            items.add(
                MoodLog(
                    id = feeling + at.toString(),
                    feeling = feeling,
                    intensity = null,
                    note = null,
                    at = at,
                    createdAt = at,
                    updatedAt = at
                )
            )
        }
    }
}


