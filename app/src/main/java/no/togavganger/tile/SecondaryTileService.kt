package no.togavganger.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.button
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.TrainRepository

private const val RESOURCES_VERSION = "0"

private fun resources(): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

@OptIn(ExperimentalHorologistApi::class)
class SecondaryTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        @Suppress("UNUSED_PARAMETER") requestParams: RequestBuilders.ResourcesRequest
    ) = resources()

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val repository = TrainRepository()
        val stationPreferences = StationPreferences(this, slot = 2)
        val selectedStationId = stationPreferences.getSelectedStationId()
        if (selectedStationId == null) {
            return setupTile(requestParams.deviceConfiguration)
        }
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
                        trainData,
                        tileSource = "tile2"
                    )
                )
            )
            .build()
    }

    private fun setupTile(deviceConfiguration: DeviceParametersBuilders.DeviceParameters): TileBuilders.Tile {
        val layout = setupTileLayout(this, deviceConfiguration)
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(60 * 60_000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

}

fun setupTileLayout(
    context: Context,
    deviceConfiguration: DeviceParametersBuilders.DeviceParameters
) = materialScope(
    context = context,
    deviceConfiguration = deviceConfiguration,
    allowDynamicTheme = false
) {
    primaryLayout(
        mainSlot = {
            LayoutElementBuilders.Column.Builder()
                .setWidth(expand())
                .apply {
                    addContent(
                        text(
                            "Togavganger 2".layoutString,
                            typography = Typography.TITLE_MEDIUM,
                            color = colorScheme.primary
                        )
                    )
                    addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
                    addContent(
                        button(
                            onClick = clickable(
                                id = "setup_tile2",
                                action = ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setClassName("no.togavganger.presentation.MainActivity")
                                            .setPackageName(context.packageName)
                                            .addKeyToExtraMapping(
                                                "tile_source",
                                                ActionBuilders.AndroidStringExtra.Builder()
                                                    .setValue("tile2")
                                                    .build()
                                            )
                                            .addKeyToExtraMapping(
                                                "open_station2_settings",
                                                ActionBuilders.AndroidBooleanExtra.Builder()
                                                    .setValue(true)
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            ),
                            labelContent = {
                                text(
                                    "Velg avgang".layoutString,
                                    typography = Typography.TITLE_SMALL,
                                    color = colorScheme.onTertiary
                                )
                            },
                            width = expand(),
                            height = dp(38f)
                        )
                    )
                }
                .build()
        }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, name = "Small Round - Stasjon 2 (ikke valgt)")
@Preview(device = WearDevices.LARGE_ROUND, name = "Large Round - Stasjon 2 (ikke valgt)")
internal fun secondaryTileSetupPreview(context: Context): TilePreviewData {
    return TilePreviewData(
        onTileRequest = { requestParams ->
            TilePreviewHelper.singleTimelineEntryTileBuilder(
                setupTileLayout(context, requestParams.deviceConfiguration)
            ).build()
        }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, name = "Small Round - Stasjon 2")
@Preview(device = WearDevices.LARGE_ROUND, name = "Large Round - Stasjon 2")
internal fun secondaryTilePreview(context: Context): TilePreviewData {
    val mockData = TrainData(
        "Nationaltheatret",
        "L2",
        listOf(
            no.togavganger.data.Departure("Ski", "08:10", "08:10", false, "3"),
            no.togavganger.data.Departure("Moss", "08:25", "08:28", true, "3"),
            no.togavganger.data.Departure("Mysen", "08:40", "08:40", false, "4")
        )
    )
    return TilePreviewData(
        onTileRequest = { requestParams ->
            TilePreviewHelper.singleTimelineEntryTileBuilder(
                tileLayout(context, requestParams.deviceConfiguration, mockData, tileSource = "tile2")
            ).build()
        }
    )
}
