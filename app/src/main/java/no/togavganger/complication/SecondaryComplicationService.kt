package no.togavganger.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import androidx.core.app.TaskStackBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.togavganger.R
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.TrainRepository
import no.togavganger.presentation.MainActivity
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SecondaryComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortText("8", "8 minutter til neste tog")
            ComplicationType.LONG_TEXT -> createLongText("8 min L2", "8 minutter til L2")
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = StationPreferences(this, slot = 2)
        val stationId = prefs.getSelectedStationId() ?: return null
        val lineId = prefs.getSelectedLineId()
        val destinations = prefs.getSelectedDestinations().takeIf { it.isNotEmpty() }

        val trainData = withContext(Dispatchers.IO) {
            TrainRepository().fetchTrainData(stationId, lineId, destinations, cacheContext = this@SecondaryComplicationService)
        }

        return buildComplicationData(trainData, request.complicationType)
    }

    private fun buildComplicationData(trainData: TrainData, type: ComplicationType): ComplicationData? {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val departures = trainData.departures
        val tapAction = createTapAction()

        if (departures.isEmpty()) {
            return when (type) {
                ComplicationType.SHORT_TEXT -> createShortText("--", "Ingen avganger", tapAction)
                ComplicationType.LONG_TEXT -> createLongText("--", "Ingen avganger for ${trainData.lineCode}", tapAction)
                else -> null
            }
        }

        val firstDep = departures.first()
        val depEpoch = parseExpectedTimeToEpoch(firstDep.expectedTime, now) ?: return when (type) {
            ComplicationType.SHORT_TEXT -> createShortText("--", "Ingen avganger", tapAction)
            ComplicationType.LONG_TEXT -> createLongText("--", "Ingen avganger", tapAction)
            else -> null
        }
        val nowEpoch = now.toInstant().toEpochMilli()
        if (depEpoch <= nowEpoch) {
            return when (type) {
                ComplicationType.SHORT_TEXT -> createShortText("Nå", "Nå", tapAction)
                ComplicationType.LONG_TEXT -> createLongText("Nå ${trainData.lineCode}", "Nå", tapAction)
                else -> null
            }
        }
        val minutesRemaining = ((depEpoch - nowEpoch) / 60_000).toInt()

        val (shortText, longText) = if (minutesRemaining == 0) {
            "Nå" to "Nå ${trainData.lineCode}"
        } else {
            minutesRemaining.toString() to "$minutesRemaining min ${trainData.lineCode}"
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortText(
                shortText,
                if (minutesRemaining == 0) "Nå" else "$minutesRemaining minutter til ${trainData.lineCode}",
                tapAction
            )
            ComplicationType.LONG_TEXT -> createLongText(
                longText,
                if (minutesRemaining == 0) "Nå" else "$minutesRemaining minutter til ${trainData.lineCode}",
                tapAction
            )
            else -> null
        }
    }

    private fun parseExpectedTimeToEpoch(timeStr: String, refNow: ZonedDateTime): Long? {
        return try {
            val localTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
            var localDate = refNow.toLocalDate()
            var zdt = ZonedDateTime.of(localDate, localTime, refNow.zone)
            val millisInPast = refNow.toInstant().toEpochMilli() - zdt.toInstant().toEpochMilli()
            if (millisInPast > 60 * 60_000) {
                localDate = localDate.plusDays(1)
                zdt = ZonedDateTime.of(localDate, localTime, refNow.zone)
            }
            zdt.toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun createTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)!!
        }
    }

    private fun complicationIcon(): MonochromaticImage {
        val icon = Icon.createWithResource(this, R.drawable.ic_complication_train)
        return MonochromaticImage.Builder(icon).build()
    }

    private fun createShortText(
        text: String,
        contentDesc: String,
        tapAction: PendingIntent? = null
    ): ShortTextComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDesc).build()
        ).apply {
            setMonochromaticImage(complicationIcon())
            tapAction?.let { setTapAction(it) }
        }.build()
    }

    private fun createLongText(
        text: String,
        contentDesc: String,
        tapAction: PendingIntent? = null
    ): LongTextComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDesc).build()
        ).apply {
            setMonochromaticImage(complicationIcon())
            tapAction?.let { setTapAction(it) }
        }.build()
    }
}
