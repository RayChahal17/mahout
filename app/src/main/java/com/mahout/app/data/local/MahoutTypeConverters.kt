package com.mahout.app.data.local

import androidx.room.TypeConverter
import com.mahout.app.domain.aim.model.GoalHorizon
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.mahout.model.JournalEntryType
import com.mahout.app.domain.northstar.model.MemoryPeriodType
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.ActionTrackingType
import com.mahout.app.domain.path.model.SessionSource
import com.mahout.app.domain.path.model.TimerStatus
import java.time.Instant
import java.time.LocalDate

/**
 * Room can only store "primitive-ish" types (String/Long/Int/etc).
 * TypeConverters teach Room how to store richer types (enums, Instant, LocalDate).
 */
class MahoutTypeConverters {

    // region Instant / LocalDate

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? =
        value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    // endregion

    // region Aim enums

    @TypeConverter
    fun goalHorizonToString(value: GoalHorizon?): String? = value?.name

    @TypeConverter
    fun stringToGoalHorizon(value: String?): GoalHorizon? =
        value?.let { GoalHorizon.valueOf(it) }

    @TypeConverter
    fun goalStatusToString(value: GoalStatus?): String? = value?.name

    @TypeConverter
    fun stringToGoalStatus(value: String?): GoalStatus? =
        value?.let { GoalStatus.valueOf(it) }

    // endregion

    // region Path enums

    @TypeConverter
    fun actionTrackingTypeToString(value: ActionTrackingType?): String? = value?.name

    @TypeConverter
    fun stringToActionTrackingType(value: String?): ActionTrackingType? =
        value?.let { ActionTrackingType.valueOf(it) }

    @TypeConverter
    fun actionCadenceToString(value: ActionCadence?): String? = value?.name

    @TypeConverter
    fun stringToActionCadence(value: String?): ActionCadence? =
        value?.let { ActionCadence.valueOf(it) }

    @TypeConverter
    fun sessionSourceToString(value: SessionSource?): String? = value?.name

    @TypeConverter
    fun stringToSessionSource(value: String?): SessionSource? =
        value?.let { SessionSource.valueOf(it) }

    @TypeConverter
    fun timerStatusToString(value: TimerStatus?): String? = value?.name

    @TypeConverter
    fun stringToTimerStatus(value: String?): TimerStatus? =
        value?.let { TimerStatus.valueOf(it) }

    // endregion

    // region Mahout enums

    @TypeConverter
    fun journalEntryTypeToString(value: JournalEntryType?): String? = value?.name

    @TypeConverter
    fun stringToJournalEntryType(value: String?): JournalEntryType? =
        value?.let { JournalEntryType.valueOf(it) }

    // endregion

    // region North Star enums

    @TypeConverter
    fun memoryPeriodTypeToString(value: MemoryPeriodType?): String? = value?.name

    @TypeConverter
    fun stringToMemoryPeriodType(value: String?): MemoryPeriodType? =
        value?.let { MemoryPeriodType.valueOf(it) }

    // endregion
}
