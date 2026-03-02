package no.togavganger.tile

import android.content.Context
// import android.util.Log
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import androidx.wear.protolayout.material3.button
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.types.layoutString
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.TrainRepository

private const val RESOURCES_VERSION = "0"

/**
 * Skeleton for a tile with no images.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        @Suppress("UNUSED_PARAMETER") requestParams: RequestBuilders.ResourcesRequest
    ) = resources()

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val repository = TrainRepository()
        val stationPreferences = StationPreferences(this)
        val selectedStationId = stationPreferences.getSelectedStationId() ?: "NSR:StopPlace:59653"
        val lineId = stationPreferences.getSelectedLineId()
        val destinations = stationPreferences.getSelectedDestinations().takeIf { it.isNotEmpty() }
        val trainData = repository.fetchTrainData(selectedStationId, lineId, destinations)
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(60 * 1000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    tileLayout(
                        this,
                        requestParams.deviceConfiguration,
                        trainData
                    )
                )
            )
            .build()
    }
}


private fun resources(): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

fun tileLayout(
    context: Context,
    deviceConfiguration: DeviceParametersBuilders.DeviceParameters,
    trainData: TrainData
) = materialScope(
    context = context,
    deviceConfiguration = deviceConfiguration,
    allowDynamicTheme = false
) {
    // Log the actual color values used in tile - always log these
    // Log.d("TileColors", "tertiaryContainer: ${colorScheme.tertiaryContainer}")
    // Log.d("TileColors", "onTertiaryContainer: ${colorScheme.onTertiaryContainer}")
    // Log.d("TileColors", "surfaceContainer: ${colorScheme.surfaceContainer}")
    // Log.d("TileColors", "onSurface: ${colorScheme.onSurface}")
    primaryLayout(
        mainSlot = {
            LayoutElementBuilders.Column.Builder()
                .setWidth(expand())
                .apply {
                    if (trainData.departures.isEmpty()) {
                        addContent(
                            text(
                                trainData.stopName.layoutString, // "API Feil" eller "Feil"
                                typography = Typography.TITLE_MEDIUM,
                                color = colorScheme.error
                            )
                        )
                        addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
                        addContent(
                            text(
                                trainData.lineCode.layoutString, // HTTP-statuskode eller Exception-navn
                                typography = Typography.BODY_MEDIUM,
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        addContent(
                            text(
                                trainData.lineCode.layoutString,
                                typography = Typography.TITLE_LARGE,
                                color = colorScheme.primary
                            )
                        )
                        addContent(
                            text(
                                trainData.stopName.layoutString,
                                typography = Typography.LABEL_MEDIUM,
                                color = colorScheme.secondary
                            )
                        )

                        addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

                        val maxDepartures = if (deviceConfiguration.screenHeightDp < 220) 2 else 3
                        trainData.departures.take(maxDepartures).forEachIndexed { index, departure ->
                            addContent(
                                button(
                                    onClick = androidx.wear.protolayout.modifiers.clickable(
                                        id = "go_${departure.destination}_${departure.aimedTime}_$index",
                                        action = androidx.wear.protolayout.ActionBuilders.LaunchAction.Builder()
                                            .setAndroidActivity(
                                                androidx.wear.protolayout.ActionBuilders.AndroidActivity.Builder()
                                                    .setClassName("no.togavganger.presentation.MainActivity")
                                                    .setPackageName(context.packageName)
                                                    .addKeyToExtraMapping(
                                                        "departure_index",
                                                        androidx.wear.protolayout.ActionBuilders.AndroidIntExtra.Builder()
                                                            .setValue(index)
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                            .build()
                                    ),
                                    labelContent = {
                                        LayoutElementBuilders.Row.Builder()
                                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .apply {
                                                if (!departure.summary.isNullOrBlank()) {
                                                    addContent(
                                                        text(
                                                            "⚠".layoutString,
                                                            typography = Typography.TITLE_SMALL,
                                                            color = if (departure.isDelayed) colorScheme.onTertiaryContainer else colorScheme.primary
                                                        )
                                                    )
                                                    addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build())
                                                }
                                                addContent(
                                                    text(
                                                        departure.destination.layoutString,
                                                        typography = Typography.TITLE_SMALL,
                                                        color = if (departure.isDelayed) colorScheme.onTertiaryContainer else colorScheme.onSurface
                                                    )
                                                )

                                                addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())

                                                if (departure.isDelayed) {
                                                    addContent(
                                                        text(
                                                            departure.expectedTime.layoutString,
                                                            color = colorScheme.tertiary,
                                                            typography = Typography.BODY_MEDIUM
                                                        )
                                                    )
                                                } else {
                                                    addContent(
                                                        text(
                                                            departure.aimedTime.layoutString,
                                                            typography = Typography.BODY_MEDIUM,
                                                            color = colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }
                                            .build()
                                    },
                                    width = expand(),
                                    height = dp(38f),
                                    colors = if (departure.isDelayed) {
                                        androidx.wear.protolayout.material3.ButtonColors(
                                            containerColor = colorScheme.tertiaryContainer,
                                            labelColor = colorScheme.onTertiaryContainer
                                        )
                                    } else {
                                        androidx.wear.protolayout.material3.ButtonColors(
                                            containerColor = colorScheme.surfaceContainer,
                                            labelColor = colorScheme.onSurface
                                        )
                                    }
                                )
                            )
                            addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(2f)).build())
                        }

                        addContent(LayoutElementBuilders.Spacer.Builder().setHeight(expand()).build())
                    }
                }
                .build()
        }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, name = "Small Round")
@Preview(device = WearDevices.LARGE_ROUND, name = "Large Round")
internal fun trainTilePreview(context: Context): TilePreviewData {
    val mockData = TrainData(
        "Haugenstua stasjon",
        "L1",
        listOf(
            no.togavganger.data.Departure("Spikkestad", "21:55", "21:56", true, "1", "Færre vogner"),
            no.togavganger.data.Departure("Oslo S", "22:10", "22:10", false, "2"),
            no.togavganger.data.Departure("Asker", "22:25", "22:25", false, "3", "Vedlikehold")
        )
    )
    return TilePreviewData(
        onTileRequest = { requestParams ->
            TilePreviewHelper.singleTimelineEntryTileBuilder(
                tileLayout(context, requestParams.deviceConfiguration, mockData)
            ).build()
        }
    )
}