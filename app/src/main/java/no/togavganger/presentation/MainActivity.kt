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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        TimeText()
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                val errorMessage = uiState.error ?: ""
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.handleEvent(TrainEvent.LoadData) },
                        colors = ButtonDefaults.primaryButtonColors()
                    ) {
                        Text("Prøv igjen")
                    }
                }
            }
            uiState.trainData != null -> {
                val trainData = uiState.trainData ?: return@Box
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

@Composable
fun TrainListContent(
    trainData: no.togavganger.data.TrainData,
    onDepartureClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = trainData.lineCode,
            style = MaterialTheme.typography.title1,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = trainData.stopName,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        val maxDepartures = 6
        trainData.departures.take(maxDepartures).forEachIndexed { index, departure ->
            DepartureCard(
                departure = departure,
                onClick = { onDepartureClick(index) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (trainData.departures.isEmpty()) {
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

@Composable
fun DepartureCard(
    departure: Departure,
    onClick: () -> Unit
) {
    val cardColor = if (departure.isDelayed) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = departure.destination,
                style = MaterialTheme.typography.body1,
                color = if (departure.isDelayed) {
                    MaterialTheme.colors.onError
                } else {
                    MaterialTheme.colors.onSurface
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (departure.isDelayed) {
                Text(
                    text = departure.expectedTime,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
            } else {
                Text(
                    text = departure.aimedTime,
                    style = MaterialTheme.typography.body2,
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
                    textAlign = TextAlign.Center // Centering title for better appearance
                )
            },
            negativeButton = { },
            // The positiveButton is defined as before
            positiveButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("Lukk")
                }
            },
            // The content that was in 'message' now goes into the 'content' lambda
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
