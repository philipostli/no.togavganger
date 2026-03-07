package no.togavganger.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.TrainRepository
import no.togavganger.presentation.MainActivity
import no.togavganger.R
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortText("5", "5 minutter til neste tog")
            ComplicationType.LONG_TEXT -> createLongText("5 min L1", "5 minutter til L1")
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = StationPreferences(this)
        val stationId = prefs.getSelectedStationId() ?: "NSR:StopPlace:59653"
        val lineId = prefs.getSelectedLineId()
        val destinations = prefs.getSelectedDestinations().takeIf { it.isNotEmpty() }

        val trainData = withContext(Dispatchers.IO) {
            TrainRepository().fetchTrainData(stationId, lineId, destinations, cacheContext = this@MainComplicationService)
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
            if (zdt.toInstant().toEpochMilli() <= refNow.toInstant().toEpochMilli()) {
                localDate = localDate.plusDays(1)
                zdt = ZonedDateTime.of(localDate, localTime, refNow.zone)
            }
            zdt.toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    private fun createTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
