/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import android.util.Base64
import android.util.Base64.DEFAULT
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Seconds
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.nio.ByteBuffer

fun List<Long>.timestampsToIntervals(): List<Int> =
    this.mapIndexed { index, timestamp ->
        return@mapIndexed if (index == 0) 0
        else
            Seconds.secondsBetween(
                DateTime(this[index - 1]),
                DateTime(timestamp)
            ).seconds
    }

class CoLocationDataProvider(
    private val contactEventDao: ContactEventDao,
    private val base64encode: (ByteArray) -> String = { Base64.encodeToString(it, DEFAULT) }
) {

    suspend fun getEvents(): List<CoLocationEvent> =
        contactEventDao.getAll().map(::convert)

    private fun convert(contactEvent: ContactEvent): CoLocationEvent {
        val startTime = DateTime(contactEvent.timestamp, DateTimeZone.UTC)
        val rssiIntervals = contactEvent.rssiTimestamps.timestampsToIntervals()

        val rssiValues = contactEvent.rssiValues.map { it.toByte() }.toByteArray()
        return CoLocationEvent(
            encryptedRemoteContactId = base64encode(contactEvent.sonarId),
            rssiValues = base64encode(rssiValues),
            rssiIntervals = rssiIntervals,
            timestamp = startTime.toUtcIsoFormat(),
            duration = contactEvent.duration,
            txPowerInProtocol = contactEvent.txPowerInProtocol,
            txPowerAdvertised = contactEvent.txPowerAdvertised,
            hmacSignature = base64encode(contactEvent.hmacSignature),
            countryCode = ByteBuffer.wrap(contactEvent.countryCode).short,
            transmissionTime = contactEvent.transmissionTime
        )
    }

    suspend fun clearData() {
        contactEventDao.clearEvents()
    }
}
