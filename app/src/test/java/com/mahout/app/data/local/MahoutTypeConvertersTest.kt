package com.mahout.app.data.local

import com.mahout.app.domain.path.model.ActionCadence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MahoutTypeConvertersTest {

    private val converters = MahoutTypeConverters()

    @Test
    fun instant_roundTrip_epochMillis() {
        val now = Instant.now()
        val millis = converters.instantToEpochMillis(now)
        val back = converters.epochMillisToInstant(millis)
        assertEquals(now.toEpochMilli(), back?.toEpochMilli())
    }

    @Test
    fun enum_roundTrip_actionCadence() {
        val value = ActionCadence.DAILY
        val stored = converters.actionCadenceToString(value)
        val back = converters.stringToActionCadence(stored)
        assertEquals(value, back)
    }
}
