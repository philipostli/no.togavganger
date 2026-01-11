package no.togavganger.presentation.viewmodel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.togavganger.data.TrainData
import no.togavganger.data.repository.TrainRepository
import no.togavganger.presentation.DepartureDetailsDialog
import no.togavganger.presentation.TrainListContent
import no.togavganger.presentation.theme.TogavgangerTheme

data class TrainUiState(
    val trainData: TrainData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDepartureIndex: Int? = null
)

sealed class TrainEvent {
    object LoadData : TrainEvent()
    data class SelectDeparture(val index: Int) : TrainEvent()
    object DismissDetails : TrainEvent()
}

class TrainViewModel(
    private val repository: TrainRepository = TrainRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainUiState())
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()
    init {
        handleEvent(TrainEvent.LoadData)
    }
    fun handleEvent(event: TrainEvent) {
        when (event) {
            is TrainEvent.LoadData -> loadTrainData()
            is TrainEvent.SelectDeparture -> selectDeparture(event.index)
            is TrainEvent.DismissDetails -> dismissDetails()
        }
    }
    private fun loadTrainData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val data = repository.fetchTrainData()
            _uiState.value = _uiState.value.copy(
                trainData = data,
                isLoading = false,
                error = if (data.departures.isEmpty() && data.stopName == "Feil") "Kunne ikke hente toginformasjon" else null
            )
        }
    }
    private fun selectDeparture(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDepartureIndex = index)
    }
    private fun dismissDetails() {
        _uiState.value = _uiState.value.copy(selectedDepartureIndex = null)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Train List - Success")
@Composable
fun TrainListSuccessPreview() {
    TogavgangerTheme {
        val mockTrainData = TrainData(
            stopName = "Haugenstua stasjon",
            lineCode = "L1",
            departures = listOf(
                no.togavganger.data.Departure("Spikkestad", "21:55", "21:56", true),
                no.togavganger.data.Departure("Oslo S", "22:10", "22:10", false),
                no.togavganger.data.Departure("Asker", "22:25", "22:25", false),
                no.togavganger.data.Departure("Drammen", "22:40", "22:40", false),
                no.togavganger.data.Departure("Oslo S", "22:55", "22:55", false),
                no.togavganger.data.Departure("Spikkestad", "23:10", "23:12", true)
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            TimeText()
            TrainListContent(
                trainData = mockTrainData,
                onDepartureClick = { }
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Train List - Loading")
@Composable
fun TrainListLoadingPreview() {
    TogavgangerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            TimeText()
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
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Train List - Error")
@Composable
fun TrainListErrorPreview() {
    TogavgangerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            TimeText()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Kunne ikke hente toginformasjon",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("Prøv igjen")
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Departure Details Dialog")
@Composable
fun DepartureDetailsDialogPreview() {
    TogavgangerTheme {
        val mockDeparture = no.togavganger.data.Departure(
            destination = "Oslo S",
            aimedTime = "22:10",
            expectedTime = "22:12",
            isDelayed = true
        )
        DepartureDetailsDialog(
            departure = mockDeparture,
            stopName = "Haugenstua stasjon",
            lineCode = "L1",
            onDismiss = { }
        )
    }
}
