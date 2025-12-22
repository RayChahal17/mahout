package com.mahout.app.domain.aim.usecase

import com.mahout.app.domain.aim.repository.ChiefAimRepository
import java.time.LocalDate
import javax.inject.Inject

class UpsertChiefAimUseCase @Inject constructor(
    private val repo: ChiefAimRepository
) {
    suspend operator fun invoke(title: String, description: String?, targetDate: LocalDate?) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Chief Aim title must not be blank." }

        val cleanDesc = description?.trim()?.takeIf { it.isNotBlank() }

        repo.upsert(
            title = cleanTitle,
            description = cleanDesc,
            targetDate = targetDate
        )
    }
}
