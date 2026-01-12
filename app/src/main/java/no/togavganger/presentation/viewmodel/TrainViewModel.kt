package no.togavganger.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.TrainRepository

data class TrainUiState(
        val trainData: TrainData? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedDepartureIndex: Int? = null,
        val showSettings: Boolean = false,
        val selectedStation: String? = null
)

sealed class TrainEvent {
    object LoadData : TrainEvent()
    data class SelectDeparture(val index: Int) : TrainEvent()
    object DismissDetails : TrainEvent()
    object ShowSettings : TrainEvent()
    object DismissSettings : TrainEvent()
    data class SelectStation(val station: String) : TrainEvent()
}

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrainRepository()
    private val stationPreferences = StationPreferences(application)
    private val _uiState = MutableStateFlow(TrainUiState(selectedStation = stationPreferences.getSelectedStation()))
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()
    init {
        val savedStation = _uiState.value.selectedStation
        if (savedStation != null) {
            loadTrainData()
        }
    }
    fun handleEvent(event: TrainEvent) {
        when (event) {
            is TrainEvent.LoadData -> loadTrainData()
            is TrainEvent.SelectDeparture -> selectDeparture(event.index)
            is TrainEvent.DismissDetails -> dismissDetails()
            is TrainEvent.ShowSettings -> showSettings()
            is TrainEvent.DismissSettings -> dismissSettings()
            is TrainEvent.SelectStation -> selectStation(event.station)
        }
    }
    private fun loadTrainData() {
        viewModelScope.launch {
            val selectedStation = _uiState.value.selectedStation
            if (selectedStation == null) {
                return@launch
            }
            val stopPlaceId = when (selectedStation) {
                "Haugensua stasjon" -> "NSR:StopPlace:59653"
                "Grorud stasjon" -> "NSR:StopPlace:59620"
                else -> return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val data = repository.fetchTrainData(stopPlaceId)
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
    private fun showSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }
    private fun dismissSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }
    private fun selectStation(station: String) {
        stationPreferences.setSelectedStation(station)
        _uiState.value = _uiState.value.copy(selectedStation = station, showSettings = false)
        loadTrainData()
    }
}
