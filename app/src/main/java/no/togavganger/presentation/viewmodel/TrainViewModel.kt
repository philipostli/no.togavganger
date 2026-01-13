package no.togavganger.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.togavganger.data.StationSearchResult
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.GeocoderRepository
import no.togavganger.data.repository.TrainRepository

data class TrainUiState(
        val trainData: TrainData? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedDepartureIndex: Int? = null,
        val showSettings: Boolean = false,
        val selectedStationId: String? = null,
        val selectedStationName: String? = null,
        val isSearching: Boolean = false,
        val searchQuery: String = "",
        val searchResults: List<StationSearchResult> = emptyList(),
        val isSearchLoading: Boolean = false
)

sealed class TrainEvent {
    object LoadData : TrainEvent()
    data class SelectDeparture(val index: Int) : TrainEvent()
    object DismissDetails : TrainEvent()
    object ShowSettings : TrainEvent()
    object DismissSettings : TrainEvent()
    data class SelectStation(val station: String) : TrainEvent()
    object ToggleSearch : TrainEvent()
    data class UpdateSearchQuery(val query: String) : TrainEvent()
    data class SelectSearchResult(val result: StationSearchResult) : TrainEvent()
}

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrainRepository()
    private val geocoderRepository = GeocoderRepository()
    private val stationPreferences = StationPreferences(application)
    private val _uiState = MutableStateFlow(
        TrainUiState(
            selectedStationId = stationPreferences.getSelectedStationId(),
            selectedStationName = stationPreferences.getSelectedStationName()
        )
    )
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()
    init {
        val savedStationId = _uiState.value.selectedStationId
        if (savedStationId != null) {
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
            is TrainEvent.ToggleSearch -> toggleSearch()
            is TrainEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is TrainEvent.SelectSearchResult -> selectSearchResult(event.result)
        }
    }
    private fun loadTrainData() {
        viewModelScope.launch {
            val selectedStationId = _uiState.value.selectedStationId
            if (selectedStationId == null) {
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val data = repository.fetchTrainData(selectedStationId)
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
        val stopPlaceId = when (station) {
            "Haugensua stasjon" -> "NSR:StopPlace:59653"
            "Grorud stasjon" -> "NSR:StopPlace:59620"
            else -> return
        }
        stationPreferences.setSelectedStation(stopPlaceId, station)
        _uiState.value = _uiState.value.copy(selectedStationId = stopPlaceId, selectedStationName = station, showSettings = false)
        loadTrainData()
    }
    private fun toggleSearch() {
        val isSearching = _uiState.value.isSearching
        _uiState.value = _uiState.value.copy(
            isSearching = !isSearching,
            searchQuery = if (!isSearching) "" else _uiState.value.searchQuery,
            searchResults = if (!isSearching) emptyList() else _uiState.value.searchResults
        )
    }
    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.length >= 2) {
            searchStations(query)
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearchLoading = false)
        }
    }
    private fun searchStations(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchLoading = true)
            val results = geocoderRepository.searchStations(query)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearchLoading = false)
        }
    }
    private fun selectSearchResult(result: StationSearchResult) {
        val stopPlaceId = result.id
        stationPreferences.setSelectedStation(stopPlaceId, result.name)
        _uiState.value = _uiState.value.copy(
            selectedStationId = stopPlaceId,
            selectedStationName = result.name,
            showSettings = false,
            isSearching = false,
            searchQuery = "",
            searchResults = emptyList()
        )
        loadTrainDataWithStopPlaceId(stopPlaceId)
    }
    private fun loadTrainDataWithStopPlaceId(stopPlaceId: String) {
        viewModelScope.launch {
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
}
