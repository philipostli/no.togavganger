package no.togavganger.presentation

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.widget.EditText
import androidx.wear.tiles.TileService
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import no.togavganger.data.Departure
import no.togavganger.data.LineInfo
import no.togavganger.data.TrainData
import no.togavganger.presentation.theme.TogavgangerTheme
import no.togavganger.tile.MainTileService
import no.togavganger.presentation.theme.getTertiaryContainerColor
import no.togavganger.presentation.theme.getOnTertiaryContainerColor
import no.togavganger.presentation.theme.getSurfaceContainerColor
import no.togavganger.presentation.theme.getOnSurfaceColor
import no.togavganger.presentation.viewmodel.TrainEvent
import no.togavganger.presentation.viewmodel.TrainViewModel
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        val departureIndex = intent?.getIntExtra("departure_index", -1)
            ?.takeIf { it >= 0 }
        val tileSource = intent?.getStringExtra("tile_source")
        val openStation2Settings = intent?.getBooleanExtra("open_station2_settings", false) == true
        setContent {
            TogavgangerTheme {
                TrainDeparturesScreen(
                    activity = this,
                    initialDepartureIndex = departureIndex,
                    tileSource = tileSource,
                    openStation2Settings = openStation2Settings
                )
            }
        }
    }
    override fun onPause() {
        super.onPause()
        TileService.getUpdater(this).requestUpdate(MainTileService::class.java)
        TileService.getUpdater(this).requestUpdate(no.togavganger.tile.SecondaryTileService::class.java)
    }
}

@Composable
fun TrainDeparturesScreenError(
    errorText: String,
    onRetry: () -> Unit = {},
    scrollState: androidx.wear.compose.foundation.lazy.TransformingLazyColumnState = rememberTransformingLazyColumnState()
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp <= 210
    val retryButtonText = if (isSmallScreen) "Hent" else "Prøv igjen"
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onRetry,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text(retryButtonText)
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 45.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = errorText,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error
                    )
                }
            }
        }
    }
}

@Composable
fun TrainDeparturesScreen(
    viewModel: TrainViewModel = viewModel(),
    activity: ComponentActivity? = null,
    initialDepartureIndex: Int? = null,
    tileSource: String? = null,
    openStation2Settings: Boolean = false
) {
    LaunchedEffect(Unit) {
        if (initialDepartureIndex != null) {
            if (tileSource == "tile2") {
                viewModel.handleEvent(TrainEvent.SelectDepartureFromStation2(initialDepartureIndex))
            } else {
                viewModel.handleEvent(TrainEvent.SelectDeparture(initialDepartureIndex))
            }
        }
        if (openStation2Settings) {
            viewModel.handleEvent(TrainEvent.ShowStation2Settings)
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberTransformingLazyColumnState()
    if (uiState.showSettings || uiState.selectedStationId == null) {
        SettingsScreen(
            selectedStation = uiState.selectedStationName,
            recentStations = uiState.recentStations,
            isSearching = uiState.isSearching,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearchLoading = uiState.isSearchLoading,
            onToggleSearch = { viewModel.handleEvent(TrainEvent.ToggleSearch) },
            onStationSelected = { result -> viewModel.handleEvent(TrainEvent.SelectSearchResult(result)) },
            onDismiss = {
                if (uiState.selectedStationId != null) {
                    viewModel.handleEvent(TrainEvent.DismissSettings)
                }
            }
        )
        if (uiState.isSearching && activity != null) {
            SearchInputDialog(
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                isSearchLoading = uiState.isSearchLoading,
                onSearchQueryChanged = { query -> viewModel.handleEvent(TrainEvent.UpdateSearchQuery(query)) },
                onSearchResultSelected = { result -> viewModel.handleEvent(TrainEvent.SelectSearchResult(result)) },
                onDismiss = { viewModel.handleEvent(TrainEvent.ToggleSearch) }
            )
        }
    } else if (uiState.showLineSelection) {
        LineSelectionScreen(
            availableLines = uiState.availableLines,
            isLoadingLines = uiState.isLoadingLines,
            onLineSelected = { line -> viewModel.handleEvent(TrainEvent.SelectLine(line)) },
            onDismiss = { viewModel.handleEvent(TrainEvent.DismissLineSelection) }
        )
    } else if (uiState.showDestinationStationSelection) {
        DestinationStationSelectionScreen(
            selectedDestinationStation = uiState.destinationStationName,
            searchQuery = uiState.destinationStationSearchQuery,
            searchResults = uiState.destinationStationSearchResults,
            isSearchLoading = uiState.isDestinationStationSearchLoading,
            onSearchQueryChanged = { viewModel.handleEvent(TrainEvent.UpdateDestinationStationSearchQuery(it)) },
            onStationSelected = { viewModel.handleEvent(TrainEvent.SelectDestinationStation(it)) },
            onDismiss = { viewModel.handleEvent(TrainEvent.DismissDestinationStationSelection) }
        )
    } else if (uiState.showDestinationSelection) {
        DestinationSelectionScreen(
            availableDestinations = uiState.availableDestinations,
            selectedDestinations = uiState.selectedDestinations,
            isLoadingDestinations = uiState.isLoadingDestinations,
            onToggleDestination = { dest -> viewModel.handleEvent(TrainEvent.ToggleDestination(dest)) },
            onConfirm = { viewModel.handleEvent(TrainEvent.ConfirmDestinations) },
            onSearchClick = { viewModel.handleEvent(TrainEvent.ShowDestinationSearch) }
        )
        if (uiState.showDestinationSearch) {
            DestinationSearchDialog(
                searchQuery = uiState.destinationSearchQuery,
                onSearchQueryChanged = { viewModel.handleEvent(TrainEvent.UpdateDestinationSearchQuery(it)) },
                onAddDestination = { viewModel.handleEvent(TrainEvent.AddCustomDestination(it)) },
                onDismiss = { viewModel.handleEvent(TrainEvent.DismissDestinationSearch) }
            )
        }
    } else if (uiState.station2ShowLineSelection) {
        LineSelectionScreen(
            availableLines = uiState.station2AvailableLines,
            isLoadingLines = uiState.station2IsLoadingLines,
            onLineSelected = { line -> viewModel.handleEvent(TrainEvent.Station2SelectLine(line)) },
            onDismiss = { viewModel.handleEvent(TrainEvent.Station2DismissLineSelection) }
        )
    } else if (uiState.station2ShowDestinationSelection) {
        DestinationSelectionScreen(
            availableDestinations = uiState.station2AvailableDestinations,
            selectedDestinations = uiState.station2SelectedDestinations,
            isLoadingDestinations = uiState.station2IsLoadingDestinations,
            onToggleDestination = { dest -> viewModel.handleEvent(TrainEvent.Station2ToggleDestination(dest)) },
            onConfirm = { viewModel.handleEvent(TrainEvent.Station2ConfirmDestinations) },
            onSearchClick = { viewModel.handleEvent(TrainEvent.Station2ShowDestinationSearch) }
        )
        if (uiState.station2ShowDestinationSearch) {
            DestinationSearchDialog(
                searchQuery = uiState.station2DestinationSearchQuery,
                onSearchQueryChanged = { viewModel.handleEvent(TrainEvent.Station2UpdateSearchQuery(it)) },
                onAddDestination = { viewModel.handleEvent(TrainEvent.Station2AddCustomDestination(it)) },
                onDismiss = { viewModel.handleEvent(TrainEvent.Station2DismissDestinationSearch) }
            )
        }
    } else if (uiState.showStation2Settings) {
        Station2SettingsScreen(
            selectedStation = uiState.station2Name,
            isSearching = uiState.station2IsSearching,
            searchQuery = uiState.station2SearchQuery,
            searchResults = uiState.station2SearchResults,
            isSearchLoading = uiState.station2IsSearchLoading,
            selectedLinePublicCode = uiState.station2SelectedLinePublicCode,
            onToggleSearch = { viewModel.handleEvent(TrainEvent.Station2ToggleSearch) },
            onStationSelected = { result -> viewModel.handleEvent(TrainEvent.Station2SelectSearchResult(result)) },
            onLinesClick = { viewModel.handleEvent(TrainEvent.Station2ShowLineSelection) },
            onClearStation = { viewModel.handleEvent(TrainEvent.Station2ClearStation) },
            onDismiss = { viewModel.handleEvent(TrainEvent.DismissStation2Settings) }
        )
        if (uiState.station2IsSearching && activity != null) {
            SearchInputDialog(
                searchQuery = uiState.station2SearchQuery,
                searchResults = uiState.station2SearchResults,
                isSearchLoading = uiState.station2IsSearchLoading,
                onSearchQueryChanged = { query -> viewModel.handleEvent(TrainEvent.Station2UpdateSearchQuery(query)) },
                onSearchResultSelected = { result -> viewModel.handleEvent(TrainEvent.Station2SelectSearchResult(result)) },
                onDismiss = { viewModel.handleEvent(TrainEvent.Station2ToggleSearch) }
            )
        }
    } else {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Laster...",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
        uiState.error != null -> {
            TrainDeparturesScreenError(
                errorText = uiState.error ?: "",
                onRetry = { viewModel.handleEvent(TrainEvent.LoadData) },
                scrollState = scrollState
            )
        }
        uiState.trainData != null -> {
            val activeStation = uiState.activeStation
            val displayData = if (activeStation == 2) uiState.station2TrainData else uiState.trainData
            val isLoadingActive = activeStation == 2 && uiState.station2IsLoadingTrainData

            if (isLoadingActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Laster...",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onBackground
                    )
                }
            } else if (displayData != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    TimeText()
                    TrainListContent(
                        trainData = displayData,
                        onDepartureClick = { index ->
                            if (activeStation == 2) {
                                viewModel.handleEvent(TrainEvent.SelectDepartureFromStation2(index))
                            } else {
                                viewModel.handleEvent(TrainEvent.SelectDeparture(index))
                            }
                        },
                        onSettingsClick = {
                            if (activeStation == 2) {
                                viewModel.handleEvent(TrainEvent.ShowStation2Settings)
                            } else {
                                viewModel.handleEvent(TrainEvent.ShowSettings)
                            }
                        },
                        onLinesClick = {
                            if (activeStation == 2) {
                                viewModel.handleEvent(TrainEvent.Station2ShowLineSelection)
                            } else {
                                viewModel.handleEvent(TrainEvent.ShowLineSelection)
                            }
                        },
                        onDestinationStationClick = { viewModel.handleEvent(TrainEvent.ShowDestinationStationSelection) },
                        onHeaderClick = { viewModel.handleEvent(TrainEvent.ShowStationSwitcher) }
                    )
                    if (uiState.showStationSwitcher) {
                        StationSwitcherDialog(
                            station1Name = uiState.selectedStationName,
                            station2Name = uiState.station2Name,
                            activeStation = activeStation,
                            onSelectStation = { station -> viewModel.handleEvent(TrainEvent.SwitchToStation(station)) },
                            onConfigureStation2 = {
                                viewModel.handleEvent(TrainEvent.DismissStationSwitcher)
                                viewModel.handleEvent(TrainEvent.ShowStation2Settings)
                            },
                            onDismiss = { viewModel.handleEvent(TrainEvent.DismissStationSwitcher) }
                        )
                    }
                    uiState.selectedDepartureIndex?.let { index ->
                        val departure = uiState.trainData?.departures?.getOrNull(index)
                        if (departure != null) {
                            DepartureDetailsDialog(
                                departure = departure,
                                stopName = uiState.trainData!!.stopName,
                                lineCode = uiState.trainData!!.lineCode,
                                stopPlaceId = uiState.selectedStationId,
                                arrivalInfo = uiState.arrivalInfo,
                                isLoadingArrival = uiState.isLoadingArrival,
                                destinationStationName = uiState.destinationStationName,
                                activity = activity,
                                onDismiss = { viewModel.handleEvent(TrainEvent.DismissDetails) }
                            )
                        }
                    }
                    uiState.station2SelectedDepartureIndex?.let { index ->
                        val station2Data = uiState.station2TrainData
                        if (station2Data != null) {
                            val departure = station2Data.departures.getOrNull(index)
                            if (departure != null) {
                                DepartureDetailsDialog(
                                    departure = departure,
                                    stopName = station2Data.stopName,
                                    lineCode = station2Data.lineCode,
                                    stopPlaceId = uiState.station2Id,
                                    arrivalInfo = null,
                                    isLoadingArrival = false,
                                    destinationStationName = null,
                                    activity = activity,
                                    onDismiss = { viewModel.handleEvent(TrainEvent.DismissStation2Details) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun TrainListContent(
    trainData: TrainData,
    onDepartureClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onLinesClick: () -> Unit = {},
    onDestinationStationClick: () -> Unit = {},
    onHeaderClick: () -> Unit = {}
) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onSettingsClick,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("Stasjon")
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 45.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = trainData.lineCode,
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onHeaderClick),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Text(
                    text = "${trainData.stopName} ▾",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onHeaderClick),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }
            val maxDepartures = 6
            trainData.departures.take(maxDepartures).forEachIndexed { index, departure ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        DepartureCard(
                            departure = departure,
                            onClick = { onDepartureClick(index) }
                        )
                    }
                }
            
            }
            if (trainData.departures.isEmpty()) {
                item {
                    Text(
                        text = "Ingen avganger funnet",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Button(
                        onClick = onLinesClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        colors = ButtonDefaults.secondaryButtonColors(
                            backgroundColor = getSurfaceContainerColor(),
                            contentColor = getOnSurfaceColor()
                        )
                    ) {
                        Text(
                            text = "Velg Linjer",
                            style = MaterialTheme.typography.title3,
                            color = getOnSurfaceColor()
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Button(
                        onClick = onDestinationStationClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        colors = ButtonDefaults.secondaryButtonColors(
                            backgroundColor = getSurfaceContainerColor(),
                            contentColor = getOnSurfaceColor()
                        )
                    ) {
                        Text(
                            text = "Velg destinasjon",
                            style = MaterialTheme.typography.title3,
                            color = getOnSurfaceColor()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StationSwitcherDialog(
    station1Name: String?,
    station2Name: String?,
    activeStation: Int,
    onSelectStation: (Int) -> Unit,
    onConfigureStation2: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        val scrollState = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = scrollState,
            edgeButton = {
                EdgeButton(
                    onClick = onDismiss,
                    buttonSize = EdgeButtonSize.ExtraSmall
                ) {
                    Text("Lukk")
                }
            },
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 45.dp
            )
        ) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ListHeader {
                        Text(
                            text = "Bytt stasjon",
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Button(
                            onClick = { onSelectStation(1) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = if (activeStation == 1) {
                                ButtonDefaults.primaryButtonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = MaterialTheme.colors.onPrimary
                                )
                            } else {
                                ButtonDefaults.secondaryButtonColors(
                                    backgroundColor = getSurfaceContainerColor(),
                                    contentColor = getOnSurfaceColor()
                                )
                            }
                        ) {
                            Text(
                                text = station1Name ?: "Stasjon 1",
                                style = MaterialTheme.typography.title3,
                                color = if (activeStation == 1) MaterialTheme.colors.onPrimary else getOnSurfaceColor()
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Button(
                            onClick = { if (station2Name != null) onSelectStation(2) else onConfigureStation2() },
                            enabled = true,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = if (activeStation == 2) {
                                ButtonDefaults.primaryButtonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = MaterialTheme.colors.onPrimary
                                )
                            } else {
                                ButtonDefaults.secondaryButtonColors(
                                    backgroundColor = getSurfaceContainerColor(),
                                    contentColor = getOnSurfaceColor()
                                )
                            }
                        ) {
                            Text(
                                text = station2Name ?: "Stasjon 2 (ikke valgt)",
                                style = MaterialTheme.typography.title3,
                                color = if (activeStation == 2) MaterialTheme.colors.onPrimary else getOnSurfaceColor()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DepartureCard(
    departure: Departure,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        colors = if (departure.isDelayed) {
            ButtonDefaults.secondaryButtonColors(
                backgroundColor = getTertiaryContainerColor(),
                contentColor = getOnTertiaryContainerColor()
            )
        } else {
            ButtonDefaults.secondaryButtonColors(
                backgroundColor = getSurfaceContainerColor(),
                contentColor = getOnSurfaceColor()
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!departure.description.isNullOrBlank() || !departure.summary.isNullOrBlank()) {
                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.title3,
                        color = if (departure.isDelayed) getOnTertiaryContainerColor() else Color(0xFFFFB300),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = departure.destination,
                    style = MaterialTheme.typography.title3,
                    color = if (departure.isDelayed) {
                        getOnTertiaryContainerColor()
                    } else {
                        getOnSurfaceColor()
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (departure.isDelayed) {
                Text(
                    text = departure.expectedTime,
                    style = MaterialTheme.typography.body1,
                    color = getOnTertiaryContainerColor()
                )
            } else {
                Text(
                    text = departure.aimedTime,
                    style = MaterialTheme.typography.body1,
                    color = getOnSurfaceColor()
                )
            }
        }
    }
}

@Composable
fun DepartureDetailsDialog(
    departure: Departure,
    stopName: String,
    lineCode: String,
    stopPlaceId: String?,
    arrivalInfo: no.togavganger.data.ArrivalInfo?,
    isLoadingArrival: Boolean,
    destinationStationName: String?,
    activity: ComponentActivity?,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        val scrollState = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = scrollState,
            edgeButton = {
                EdgeButton(
                    onClick = onDismiss,
                    buttonSize = EdgeButtonSize.ExtraSmall
                ) {
                    Text("Lukk")
                }
            },
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 45.dp
            )
        ) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                if (!departure.description.isNullOrBlank() || !departure.summary.isNullOrBlank()) {
                    item {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.title2,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                item {
                    Text(
                        text = "$lineCode - ${departure.destination}",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (departure.platformCode != null) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    item {
                        Text(
                            text = "Plattform ${departure.platformCode}",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Text(
                        text = "${stopName}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Text(
                        text = "Planlagt: ${departure.aimedTime}",
                        style = MaterialTheme.typography.body2,
                        color = if (departure.isDelayed) MaterialTheme.colors.error else MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (departure.isDelayed) {
                    val delayMinutes = java.time.temporal.ChronoUnit.MINUTES.between(java.time.LocalTime.parse(departure.aimedTime), java.time.LocalTime.parse(departure.expectedTime)).let { if (it < 0) it + 24 * 60 else it }.toInt()
                    item {
                        Text(
                            text = "Forventet: ${departure.expectedTime}",
                            style = MaterialTheme.typography.body2,
                            color = Color(0xFF64B5F6), //blue color
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            text = "$delayMinutes min forsinket",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }else{
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    item {
                        Text(
                            text = "I rute",
                            style = MaterialTheme.typography.body2,
                            color = Color(0xFF00FF55), //light green color
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (destinationStationName != null) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        when {
                            isLoadingArrival -> Text(
                                text = "Henter ankomst...",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            arrivalInfo != null -> {
                                val isArrivalDelayed = departure.isDelayed || arrivalInfo.isDelayed
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Ankomst: $destinationStationName",
                                        style = MaterialTheme.typography.body2,
                                        color = MaterialTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Planlagt: ${arrivalInfo.aimedArrivalTime}",
                                        style = MaterialTheme.typography.body2,
                                        color = if (isArrivalDelayed) MaterialTheme.colors.error else MaterialTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                    if (isArrivalDelayed) {
                                        Text(
                                            text = "Forventet: ${arrivalInfo.expectedArrivalTime}",
                                            style = MaterialTheme.typography.body2,
                                            color = Color(0xFF64B5F6),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            else -> Box(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                if (!departure.description.isNullOrBlank() || !departure.summary.isNullOrBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Avvik:",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = departure.description ?: departure.summary ?: "",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (stopPlaceId != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Button(
                                onClick = {
                                    val url = "https://entur.no/nearby-stop-place-detail?id=$stopPlaceId&transportModes=rail"
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    activity?.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = ButtonDefaults.secondaryButtonColors(
                                    backgroundColor = getSurfaceContainerColor(),
                                    contentColor = getOnSurfaceColor()
                                )
                            ) {
                                Text(
                                    text = "Åpne i Entur",
                                    style = MaterialTheme.typography.title3,
                                    color = getOnSurfaceColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineSelectionScreen(
    availableLines: List<LineInfo>,
    isLoadingLines: Boolean,
    onLineSelected: (LineInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onDismiss,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("Tilbake")
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 45.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Velg linje",
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoadingLines) {
                item {
                    Text(
                        text = "Laster...",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                availableLines.forEach { line ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            val textHex = line.textColour.let { if (it.startsWith("#")) it else "#$it" }
                            val bgHex = line.colour.let { if (it.startsWith("#")) it else "#$it" }
                            val lineColor = try {
                                Color(textHex.toColorInt())
                            } catch (_: Exception) {
                                getOnSurfaceColor()
                            }
                            val bgColor = try {
                                Color(bgHex.toColorInt())
                            } catch (_: Exception) {
                                getSurfaceContainerColor()
                            }
                            Button(
                                onClick = { onLineSelected(line) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = ButtonDefaults.secondaryButtonColors(
                                    backgroundColor = bgColor,
                                    contentColor = lineColor
                                )
                            ) {
                                Text(
                                    text = line.publicCode,
                                    style = MaterialTheme.typography.title3,
                                    color = lineColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DestinationSelectionScreen(
    availableDestinations: List<String>,
    selectedDestinations: Set<String>,
    isLoadingDestinations: Boolean,
    onToggleDestination: (String) -> Unit,
    onConfirm: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onConfirm,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("Ferdig")
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 45.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Destinasjoner",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Button(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        colors = ButtonDefaults.secondaryButtonColors(
                            backgroundColor = getSurfaceContainerColor(),
                            contentColor = getOnSurfaceColor()
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⌛",
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Søk",
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (isLoadingDestinations) {
                item {
                    Text(
                        text = "Laster...",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                availableDestinations.forEach { dest ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            val isSelected = dest in selectedDestinations
                            Button(
                                onClick = { onToggleDestination(dest) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = if (isSelected) {
                                    ButtonDefaults.primaryButtonColors(
                                        backgroundColor = MaterialTheme.colors.primary,
                                        contentColor = MaterialTheme.colors.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = getSurfaceContainerColor(),
                                        contentColor = getOnSurfaceColor()
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dest,
                                        style = MaterialTheme.typography.title3,
                                        color = if (isSelected) MaterialTheme.colors.onPrimary else getOnSurfaceColor()
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.title3,
                                            color = MaterialTheme.colors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DestinationSearchDialog(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onAddDestination: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        val scrollState = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = scrollState,
            edgeButton = {
                EdgeButton(
                    onClick = onDismiss,
                    buttonSize = EdgeButtonSize.ExtraSmall
                ) {
                    Text("Lukk")
                }
            },
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 45.dp
            )
        ) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ListHeader {
                        Text(
                            text = "Skriv inn destinasjon",
                            style = MaterialTheme.typography.title3,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    AndroidView(
                        factory = { ctx ->
                            EditText(ctx).apply {
                                hint = "F.eks. Oslo S, Spikkestad..."
                                setText(searchQuery)
                                isSingleLine = true
                                requestFocus()
                                post {
                                    val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                                }
                                addTextChangedListener(object : android.text.TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        onSearchQueryChanged(s?.toString() ?: "")
                                    }
                                    override fun afterTextChanged(s: android.text.Editable?) {}
                                })
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = {
                                val dest = searchQuery.trim()
                                if (dest.isNotEmpty()) {
                                    onAddDestination(dest)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.primaryButtonColors(
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = MaterialTheme.colors.onPrimary
                            )
                        ) {
                            Text(
                                text = "Legg til",
                                style = MaterialTheme.typography.title3,
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    selectedStation: String?,
    recentStations: List<no.togavganger.data.StationSearchResult>,
    isSearching: Boolean,
    searchQuery: String,
    searchResults: List<no.togavganger.data.StationSearchResult>,
    isSearchLoading: Boolean,
    onToggleSearch: () -> Unit,
    onStationSelected: (no.togavganger.data.StationSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            if (selectedStation != null) {
                EdgeButton(
                    onClick = onDismiss,
                    buttonSize = EdgeButtonSize.Medium
                ) {
                    Text("Tilbake")
                }
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = if (selectedStation != null) 45.dp else 4.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Velg stasjon",
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onToggleSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.secondaryButtonColors(
                                backgroundColor = getSurfaceContainerColor(),
                                contentColor = getOnSurfaceColor()
                            )
                        ) {
                            Text(
                                text = "🔍 Søk",
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onToggleSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.secondaryButtonColors(
                                backgroundColor = getSurfaceContainerColor(),
                                contentColor = getOnSurfaceColor()
                            )
                        ) {
                            Text(
                                text = searchQuery.ifEmpty { "Søk..." },
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (isSearchLoading) {
                    item {
                        Text(
                            text = "Søker...",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (searchResults.isNotEmpty()) {
                    searchResults.forEach { result ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Button(
                                    onClick = { onStationSelected(result) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = getSurfaceContainerColor(),
                                        contentColor = getOnSurfaceColor()
                                    )
                                ) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.title3,
                                        color = getOnSurfaceColor()
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.length >= 2) {
                    item {
                        Text(
                            text = "Ingen resultater",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (!isSearching) {
                recentStations.forEach { station ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Button(
                                onClick = { onStationSelected(station) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = if (station.name == selectedStation) {
                                    ButtonDefaults.primaryButtonColors(
                                        backgroundColor = MaterialTheme.colors.primary,
                                        contentColor = MaterialTheme.colors.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = getSurfaceContainerColor(),
                                        contentColor = getOnSurfaceColor()
                                    )
                                }
                            ) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.title3,
                                    color = if (station.name == selectedStation) {
                                        MaterialTheme.colors.onPrimary
                                    } else {
                                        getOnSurfaceColor()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Station2SettingsScreen(
    selectedStation: String?,
    isSearching: Boolean,
    searchQuery: String,
    searchResults: List<no.togavganger.data.StationSearchResult>,
    isSearchLoading: Boolean,
    selectedLinePublicCode: String?,
    onToggleSearch: () -> Unit,
    onStationSelected: (no.togavganger.data.StationSearchResult) -> Unit,
    onLinesClick: () -> Unit,
    onClearStation: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onDismiss,
                buttonSize = EdgeButtonSize.Medium
            ) {
                Text("Tilbake")
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 45.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Stasjon 2",
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (selectedStation != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Text(
                        text = selectedStation,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (selectedLinePublicCode != null) {
                    item {
                        Text(
                            text = "Linje: $selectedLinePublicCode",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onToggleSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.secondaryButtonColors(
                                backgroundColor = getSurfaceContainerColor(),
                                contentColor = getOnSurfaceColor()
                            )
                        ) {
                            Text(
                                text = "🔍 Søk stasjon",
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onToggleSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.secondaryButtonColors(
                                backgroundColor = getSurfaceContainerColor(),
                                contentColor = getOnSurfaceColor()
                            )
                        ) {
                            Text(
                                text = searchQuery.ifEmpty { "Søk..." },
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (isSearchLoading) {
                    item {
                        Text(
                            text = "Søker...",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (searchResults.isNotEmpty()) {
                    searchResults.forEach { result ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Button(
                                    onClick = { onStationSelected(result) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = getSurfaceContainerColor(),
                                        contentColor = getOnSurfaceColor()
                                    )
                                ) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.title3,
                                        color = getOnSurfaceColor()
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.length >= 2) {
                    item {
                        Text(
                            text = "Ingen resultater",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (selectedStation != null && !isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onLinesClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.secondaryButtonColors(
                                backgroundColor = getSurfaceContainerColor(),
                                contentColor = getOnSurfaceColor()
                            )
                        ) {
                            Text(
                                text = "Velg Linjer",
                                style = MaterialTheme.typography.title3,
                                color = getOnSurfaceColor()
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onClearStation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.secondaryButtonColors(
                                backgroundColor = getSurfaceContainerColor(),
                                contentColor = MaterialTheme.colors.error
                            )
                        ) {
                            Text(
                                text = "Fjern stasjon 2",
                                style = MaterialTheme.typography.title3,
                                color = MaterialTheme.colors.error
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun DestinationStationSelectionScreen(
    selectedDestinationStation: String?,
    searchQuery: String,
    searchResults: List<no.togavganger.data.StationSearchResult>,
    isSearchLoading: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onStationSelected: (no.togavganger.data.StationSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onDismiss,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("Tilbake")
            }
        },
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 45.dp
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Velg destinasjon",
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (selectedDestinationStation != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Text(
                        text = "Valgt: $selectedDestinationStation",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                AndroidView(
                    factory = { ctx ->
                        EditText(ctx).apply {
                            hint = "Søk etter stasjon..."
                            setText(searchQuery)
                            isSingleLine = true
                            requestFocus()
                            post {
                                val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    onSearchQueryChanged(s?.toString() ?: "")
                                }
                                override fun afterTextChanged(s: android.text.Editable?) {}
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 4.dp)
                )
            }
            if (isSearchLoading) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Text(
                        text = "Søker...",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (searchResults.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                searchResults.forEach { result ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Button(
                                onClick = { onStationSelected(result) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = ButtonDefaults.secondaryButtonColors(
                                    backgroundColor = getSurfaceContainerColor(),
                                    contentColor = getOnSurfaceColor()
                                )
                            ) {
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.title3,
                                    color = getOnSurfaceColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchInputDialog(
    searchQuery: String,
    searchResults: List<no.togavganger.data.StationSearchResult>,
    isSearchLoading: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearchResultSelected: (no.togavganger.data.StationSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        val scrollState = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = scrollState,
            edgeButton = {
                EdgeButton(
                    onClick = onDismiss,
                    buttonSize = EdgeButtonSize.ExtraSmall
                ) {
                    Text("Lukk")
                }
            },
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 45.dp
            )
        ) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ListHeader {
                        Text(
                            text = "Søk etter stasjon",
                            style = MaterialTheme.typography.title3,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    AndroidView(
                        factory = { ctx ->
                            EditText(ctx).apply {
                                hint = "Skriv stasjonsnavn..."
                                setText(searchQuery)
                                isSingleLine = true
                                requestFocus()
                                post {
                                    val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                                }
                                setOnEditorActionListener { _, _, _ ->
                                    onSearchQueryChanged(text.toString())
                                    false
                                }
                                addTextChangedListener(object : android.text.TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        onSearchQueryChanged(s?.toString() ?: "")
                                    }
                                    override fun afterTextChanged(s: android.text.Editable?) {}
                                })
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
                if (isSearchLoading) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Text(
                            text = "Søker...",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (searchResults.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    searchResults.forEach { result ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Button(
                                    onClick = { onSearchResultSelected(result) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = getSurfaceContainerColor(),
                                        contentColor = getOnSurfaceColor()
                                    )
                                ) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.title3,
                                        color = getOnSurfaceColor()
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.length >= 2) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Text(
                            text = "Ingen resultater",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
