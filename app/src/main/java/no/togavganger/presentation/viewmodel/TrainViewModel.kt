package no.togavganger.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.togavganger.data.TrainData
import no.togavganger.data.repository.TrainRepository
import no.togavganger.presentation.DepartureDetailsDialog
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

class TrainViewModel(private val repository: TrainRepository = TrainRepository()) : ViewModel() {
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
            _uiState.value =
                    _uiState.value.copy(
                            trainData = data,
                            isLoading = false,
                            error =
                                    if (data.departures.isEmpty() && data.stopName == "Feil")
                                            "Kunne ikke hente toginformasjon"
                                    else null
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
        val mockDeparture =
                no.togavganger.data.Departure(
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
                onDismiss = {}
        )
    }
}
