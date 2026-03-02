package no.togavganger.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.togavganger.data.LineInfo
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
        val isSearchLoading: Boolean = false,
        val showLineSelection: Boolean = false,
        val availableLines: List<LineInfo> = emptyList(),
        val isLoadingLines: Boolean = false,
        val showDestinationSelection: Boolean = false,
        val availableDestinations: List<String> = emptyList(),
        val selectedDestinations: Set<String> = emptySet(),
        val isLoadingDestinations: Boolean = false,
        val selectedLineId: String? = null,
        val selectedLinePublicCode: String? = null,
        val showDestinationSearch: Boolean = false,
        val destinationSearchQuery: String = "",
        val recentStations: List<StationSearchResult> = emptyList()
)

sealed class TrainEvent {
    object LoadData : TrainEvent()
    data class SelectDeparture(val index: Int) : TrainEvent()
    object DismissDetails : TrainEvent()
    object ShowSettings : TrainEvent()
    object DismissSettings : TrainEvent()
    object ToggleSearch : TrainEvent()
    data class UpdateSearchQuery(val query: String) : TrainEvent()
    data class SelectSearchResult(val result: StationSearchResult) : TrainEvent()
    object ShowLineSelection : TrainEvent()
    object DismissLineSelection : TrainEvent()
    data class SelectLine(val lineInfo: LineInfo) : TrainEvent()
    data class ToggleDestination(val destination: String) : TrainEvent()
    object ConfirmDestinations : TrainEvent()
    object ShowDestinationSearch : TrainEvent()
    object DismissDestinationSearch : TrainEvent()
    data class UpdateDestinationSearchQuery(val query: String) : TrainEvent()
    data class AddCustomDestination(val destination: String) : TrainEvent()
}

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrainRepository()
    private val geocoderRepository = GeocoderRepository()
    private val stationPreferences = StationPreferences(application)
    private val _uiState = MutableStateFlow(
        TrainUiState(
            selectedStationId = stationPreferences.getSelectedStationId(),
            selectedStationName = stationPreferences.getSelectedStationName(),
            selectedLineId = stationPreferences.getSelectedLineId(),
            selectedLinePublicCode = stationPreferences.getSelectedLinePublicCode(),
            selectedDestinations = stationPreferences.getSelectedDestinations().toSet()
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
            is TrainEvent.ToggleSearch -> toggleSearch()
            is TrainEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is TrainEvent.SelectSearchResult -> selectSearchResult(event.result)
            is TrainEvent.ShowLineSelection -> showLineSelection()
            is TrainEvent.DismissLineSelection -> dismissLineSelection()
            is TrainEvent.SelectLine -> selectLine(event.lineInfo)
            is TrainEvent.ToggleDestination -> toggleDestination(event.destination)
            is TrainEvent.ConfirmDestinations -> confirmDestinations()
            is TrainEvent.ShowDestinationSearch -> showDestinationSearch()
            is TrainEvent.DismissDestinationSearch -> dismissDestinationSearch()
            is TrainEvent.UpdateDestinationSearchQuery -> updateDestinationSearchQuery(event.query)
            is TrainEvent.AddCustomDestination -> addCustomDestination(event.destination)
        }
    }
    private fun loadTrainData() {
        viewModelScope.launch {
            val selectedStationId = _uiState.value.selectedStationId ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val lineId = _uiState.value.selectedLineId
            val destinations = _uiState.value.selectedDestinations.takeIf { it.isNotEmpty() }
            val data = repository.fetchTrainData(selectedStationId, lineId, destinations, cacheContext = getApplication())
            _uiState.value =
                    _uiState.value.copy(
                            trainData = data,
                            isLoading = false,
                            error = if (data.isApiError) "Kunne ikke hente toginformasjon" else null
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
        val recent = stationPreferences.getRecentStations().map { StationSearchResult(it.first, it.second) }
        _uiState.value = _uiState.value.copy(showSettings = true, recentStations = recent)
    }
    private fun dismissSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
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
        stationPreferences.addStationToRecent(stopPlaceId, result.name)
        val recent = stationPreferences.getRecentStations().map { StationSearchResult(it.first, it.second) }
        _uiState.value = _uiState.value.copy(
            selectedStationId = stopPlaceId,
            selectedStationName = result.name,
            showSettings = false,
            isSearching = false,
            searchQuery = "",
            searchResults = emptyList(),
            recentStations = recent
        )
        loadTrainDataWithStopPlaceId(stopPlaceId)
    }
    private fun loadTrainDataWithStopPlaceId(stopPlaceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val lineId = _uiState.value.selectedLineId
            val destinations = _uiState.value.selectedDestinations.takeIf { it.isNotEmpty() }
            val data = repository.fetchTrainData(stopPlaceId, lineId, destinations, cacheContext = getApplication())
            _uiState.value =
                    _uiState.value.copy(
                            trainData = data,
                            isLoading = false,
                            error = if (data.isApiError) "Kunne ikke hente toginformasjon" else null
                    )
        }
    }
    private fun showLineSelection() {
        _uiState.value = _uiState.value.copy(showLineSelection = true, isLoadingLines = true, availableLines = emptyList())
        val stopPlaceId = _uiState.value.selectedStationId ?: return
        viewModelScope.launch {
            val lines = repository.fetchLines(stopPlaceId).sortedBy { it.publicCode }
            _uiState.value = _uiState.value.copy(availableLines = lines, isLoadingLines = false)
        }
    }
    private fun dismissLineSelection() {
        _uiState.value = _uiState.value.copy(showLineSelection = false, availableLines = emptyList(), isLoadingLines = false)
    }
    private fun selectLine(lineInfo: LineInfo) {
        stationPreferences.setSelectedLine(lineInfo.id, lineInfo.publicCode)
        _uiState.value = _uiState.value.copy(
            showLineSelection = false,
            availableLines = emptyList(),
            selectedLineId = lineInfo.id,
            selectedLinePublicCode = lineInfo.publicCode,
            showDestinationSelection = true,
            isLoadingDestinations = true,
            availableDestinations = emptyList()
        )
        val stopPlaceId = _uiState.value.selectedStationId ?: return
        viewModelScope.launch {
            val destinations = repository.fetchDestinations(stopPlaceId, lineInfo.id)
            _uiState.value = _uiState.value.copy(
                availableDestinations = destinations,
                selectedDestinations = destinations.toSet(),
                isLoadingDestinations = false
            )
        }
    }
    private fun toggleDestination(destination: String) {
        val current = _uiState.value.selectedDestinations
        val next = if (destination in current) current - destination else current + destination
        _uiState.value = _uiState.value.copy(selectedDestinations = next)
    }
    private fun confirmDestinations() {
        val selected = _uiState.value.selectedDestinations
        stationPreferences.setSelectedDestinations(selected)
        _uiState.value = _uiState.value.copy(
            showDestinationSelection = false,
            availableDestinations = emptyList(),
            selectedDestinations = selected
        )
        loadTrainData()
    }
    private fun showDestinationSearch() {
        _uiState.value = _uiState.value.copy(showDestinationSearch = true, destinationSearchQuery = "")
    }
    private fun dismissDestinationSearch() {
        _uiState.value = _uiState.value.copy(showDestinationSearch = false, destinationSearchQuery = "")
    }
    private fun updateDestinationSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(destinationSearchQuery = query)
    }
    private fun addCustomDestination(destination: String) {
        val trimmed = destination.trim()
        if (trimmed.isEmpty()) return
        val updated = _uiState.value.selectedDestinations + trimmed
        val dests = _uiState.value.availableDestinations
        val newDests = if (trimmed in dests) dests else dests + trimmed
        _uiState.value = _uiState.value.copy(
            selectedDestinations = updated,
            showDestinationSearch = false,
            destinationSearchQuery = "",
            availableDestinations = newDests
        )
    }
}
