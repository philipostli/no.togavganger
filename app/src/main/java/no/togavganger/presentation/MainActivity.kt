package no.togavganger.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Text as Material3Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import no.togavganger.data.Departure
import no.togavganger.presentation.theme.TogavgangerTheme
import no.togavganger.presentation.viewmodel.TrainEvent
import no.togavganger.presentation.viewmodel.TrainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            TogavgangerTheme {
                TrainDeparturesScreen()
            }
        }
    }
}

@Composable
fun TrainDeparturesScreen(
    viewModel: TrainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    EdgeButton(
                        onClick = { viewModel.handleEvent(TrainEvent.LoadData) },
                        modifier = Modifier.fillMaxWidth(),
                        buttonSize = EdgeButtonSize.Small
                    ) {
                        Material3Text("Prøv igjen")
                    }
                }
            }
        }
        uiState.trainData != null -> {
            val trainData = uiState.trainData
            if (trainData != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    TimeText()
                    TrainListContent(
                        trainData = trainData,
                        onDepartureClick = { index -> viewModel.handleEvent(TrainEvent.SelectDeparture(index)) }
                    )
                    uiState.selectedDepartureIndex?.let { index ->
                        val departure = trainData.departures.getOrNull(index)
                        if (departure != null) {
                            DepartureDetailsDialog(
                                departure = departure,
                                stopName = trainData.stopName,
                                lineCode = trainData.lineCode,
                                onDismiss = { viewModel.handleEvent(TrainEvent.DismissDetails) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainListContent(
    trainData: no.togavganger.data.TrainData,
    onDepartureClick: (Int) -> Unit
) {
    val scrollState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 45.dp),
        autoCentering = null
    ) {
        item {
            Text(
                text = trainData.lineCode,
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        item {
            Text(
                text = trainData.stopName,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.secondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        val maxDepartures = 6
        items(trainData.departures.take(maxDepartures).size) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                DepartureCard(
                    departure = trainData.departures[index],
                    onClick = { onDepartureClick(index) }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
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
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colors.error
            )
        } else {
            ButtonDefaults.secondaryButtonColors(
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colors.onSurface
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
            Text(
                text = departure.destination,
                style = MaterialTheme.typography.title3,
                color = if (departure.isDelayed) {
                    MaterialTheme.colors.error
                } else {
                    MaterialTheme.colors.onSurface
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (departure.isDelayed) {
                Text(
                    text = departure.expectedTime,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.error
                )
            } else {
                Text(
                    text = departure.aimedTime,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurfaceVariant
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
    onDismiss: () -> Unit
) {
    // The Dialog composable remains the same
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        // Here is the corrected Alert composable
        Alert(
            title = {
                Text(
                    text = departure.destination,
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center
                )
            },
            negativeButton = { },
            positiveButton = { },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Linje: $lineCode",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface
                    )
                    if (departure.platformCode != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Plattform: ${departure.platformCode}",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Planlagt: ${departure.aimedTime}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                    if (departure.isDelayed) {
                        Text(
                            text = "Forventet: ${departure.expectedTime}",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.error
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    EdgeButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        buttonSize = EdgeButtonSize.ExtraSmall
                    ) {
                        Material3Text("Lukk")
                    }
                }
            }
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    TogavgangerTheme {
        TrainDeparturesScreen()
    }
}
