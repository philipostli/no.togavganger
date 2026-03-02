package no.togavganger.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import no.togavganger.data.Departure
import no.togavganger.data.StationSearchResult
import no.togavganger.data.TrainData
import no.togavganger.presentation.theme.TogavgangerTheme

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Train List - Success (Small)")
@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Train List - Success (Large)")
@Composable
fun TrainListSuccessPreview() {
    TogavgangerTheme {
        val mockTrainData =
            TrainData(
                stopName = "Haugenstua stasjon",
                lineCode = "L1",
                departures =
                    listOf(
                        Departure(
                            "Spikkestad",
                            "21:55",
                            "21:56",
                            true,
                            "1"
                        ),
                        Departure(
                            "Oslo S",
                            "22:10",
                            "22:10",
                            false,
                            "2",
                            "Buss erstatter tog"
                        ),
                        Departure(
                            "Asker",
                            "22:25",
                            "22:25",
                            false,
                            "3"
                        ),
                        Departure(
                            "Drammen",
                            "22:40",
                            "22:40",
                            false,
                            "1"
                        ),
                        Departure(
                            "Oslo S",
                            "22:55",
                            "22:55",
                            false,
                            "2"
                        ),
                        Departure(
                            "Spikkestad",
                            "23:10",
                            "23:12",
                            true,
                            "3"
                        )
                    )
            )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            TimeText()
            TrainListContent(trainData = mockTrainData, onDepartureClick = {}, onSettingsClick = {})
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Train List - Error (Small)")
@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Train List - Error (Large)")
@Composable
fun TrainListErrorPreview() {
    TogavgangerTheme {
        TrainDeparturesScreenError(errorText = "Kunne ikke hente toginformasjon")
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    name = "Departure Details Dialog (Small)"
)
@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true,
    name = "Departure Details Dialog (Large)"
)
@Composable
fun DepartureDetailsDialogPreview() {
    TogavgangerTheme {
        val mockDeparture = Departure(
            destination = "Oslo S",
            aimedTime = "22:10",
            expectedTime = "22:12",
            isDelayed = true,
            platformCode = "2"
        )
        DepartureDetailsDialog(
            departure = mockDeparture,
            stopName = "Haugenstua stasjon",
            lineCode = "L1",
            stopPlaceId = "NSR:StopPlace:59653",
            activity = null,
            onDismiss = {}
        )
    }
}
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    name = "Departure Details Dialog (Small)"
)
@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true,
    name = "Departure Details Dialog (Large)"
)
@Composable
fun DepartureDetailsDialogPreview2() {
    TogavgangerTheme {
        val mockDeparture = Departure(
            destination = "Oslo S",
            aimedTime = "22:10",
            expectedTime = "22:10",
            isDelayed = false,
            platformCode = "2",
            summary = "Buss erstatter tog"
        )
        DepartureDetailsDialog(
            departure = mockDeparture,
            stopName = "Haugenstua stasjon",
            lineCode = "L1",
            stopPlaceId = "NSR:StopPlace:59653",
            activity = null,
            onDismiss = {}
        )
    }
}
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    name = "Departure Details Dialog (Small)"
)
@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true,
    name = "Departure Details Dialog (Large)"
)
@Composable
fun DepartureDetailsDialogPreview3() {
    TogavgangerTheme {
        val mockDeparture = Departure(
            destination = "Oslo S",
            aimedTime = "22:10",
            expectedTime = "22:10",
            isDelayed = false,
            platformCode = "2"
        )
        DepartureDetailsDialog(
            departure = mockDeparture,
            stopName = "Haugenstua stasjon",
            lineCode = "L1",
            stopPlaceId = "NSR:StopPlace:59653",
            activity = null,
            onDismiss = {}
        )
    }
}

// @Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    TogavgangerTheme {
        TrainDeparturesScreen()
        // TestScreen("Android",  {} )
    }
}

//@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Train List - Loading (Small)")
//@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Train List - Loading (Large)")
@Composable
fun TrainListLoadingPreview() {
    TogavgangerTheme {
        TrainDeparturesScreen()
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Settings Screen - No Selection (Small)")
@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Settings Screen - No Selection (Large)")
@Composable
fun SettingsScreenNoSelectionPreview() {
    TogavgangerTheme {
        SettingsScreen(
            selectedStation = null,
            recentStations = emptyList(),
            isSearching = false,
            searchQuery = "",
            searchResults = emptyList(),
            isSearchLoading = false,
            onToggleSearch = {},
            onStationSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Settings Screen - With Selection (Small)")
@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Settings Screen - With Selection (Large)")
@Composable
fun SettingsScreenWithSelectionPreview() {
    TogavgangerTheme {
        SettingsScreen(
            selectedStation = "Haugenstua stasjon",
            recentStations = listOf(
                StationSearchResult("NSR:StopPlace:59653", "Haugenstua stasjon"),
                StationSearchResult("NSR:StopPlace:59620", "Grorud stasjon")
            ),
            isSearching = false,
            searchQuery = "",
            searchResults = emptyList(),
            isSearchLoading = false,
            onToggleSearch = {},
            onStationSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Settings Screen - Search Results (Small)")
@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Settings Screen - Search Results (Large)")
@Composable
fun SettingsScreenSearchResultsPreview() {
    TogavgangerTheme {
        SettingsScreen(
            selectedStation = null,
            recentStations = emptyList(),
            isSearching = true,
            searchQuery = "Haugen",
            searchResults = listOf(
                StationSearchResult("NSR:StopPlace:59653", "Haugenstua stasjon"),
                StationSearchResult("NSR:StopPlace:59620", "Grorud stasjon"),
                StationSearchResult("NSR:StopPlace:12345", "Haugenstua torg")
            ),
            isSearchLoading = false,
            onToggleSearch = {},
            onStationSelected = {},
            onDismiss = {}
        )
    }
}