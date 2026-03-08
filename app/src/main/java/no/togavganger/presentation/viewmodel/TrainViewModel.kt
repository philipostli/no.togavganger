package no.togavganger.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.togavganger.data.ArrivalInfo
import no.togavganger.data.LineInfo
import no.togavganger.data.StationSearchResult
import no.togavganger.data.TrainData
import no.togavganger.data.preferences.StationPreferences
import no.togavganger.data.repository.GeocoderRepository
import no.togavganger.data.repository.TrainRepository
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
        val recentStations: List<StationSearchResult> = emptyList(),
        val destinationStationId: String? = null,
        val destinationStationName: String? = null,
        val showDestinationStationSelection: Boolean = false,
        val destinationStationSearchQuery: String = "",
        val destinationStationSearchResults: List<StationSearchResult> = emptyList(),
        val isDestinationStationSearchLoading: Boolean = false,
        val arrivalInfo: ArrivalInfo? = null,
        val isLoadingArrival: Boolean = false,
        val showStation2Settings: Boolean = false,
        val station2Id: String? = null,
        val station2Name: String? = null,
        val station2IsSearching: Boolean = false,
        val station2SearchQuery: String = "",
        val station2SearchResults: List<StationSearchResult> = emptyList(),
        val station2IsSearchLoading: Boolean = false,
        val station2ShowLineSelection: Boolean = false,
        val station2AvailableLines: List<LineInfo> = emptyList(),
        val station2IsLoadingLines: Boolean = false,
        val station2SelectedLineId: String? = null,
        val station2SelectedLinePublicCode: String? = null,
        val station2ShowDestinationSelection: Boolean = false,
        val station2AvailableDestinations: List<String> = emptyList(),
        val station2SelectedDestinations: Set<String> = emptySet(),
        val station2IsLoadingDestinations: Boolean = false,
        val station2ShowDestinationSearch: Boolean = false,
        val station2DestinationSearchQuery: String = "",
        val station2TrainData: TrainData? = null,
        val station2SelectedDepartureIndex: Int? = null,
        val station2IsLoadingTrainData: Boolean = false,
        val activeStation: Int = 1,
        val showStationSwitcher: Boolean = false
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
    object ShowDestinationStationSelection : TrainEvent()
    object DismissDestinationStationSelection : TrainEvent()
    data class UpdateDestinationStationSearchQuery(val query: String) : TrainEvent()
    data class SelectDestinationStation(val result: StationSearchResult) : TrainEvent()

    object ShowStation2Settings : TrainEvent()
    object DismissStation2Settings : TrainEvent()
    object Station2ToggleSearch : TrainEvent()
    data class Station2UpdateSearchQuery(val query: String) : TrainEvent()
    data class Station2SelectSearchResult(val result: StationSearchResult) : TrainEvent()
    object Station2ShowLineSelection : TrainEvent()
    object Station2DismissLineSelection : TrainEvent()
    data class Station2SelectLine(val lineInfo: LineInfo) : TrainEvent()
    data class Station2ToggleDestination(val destination: String) : TrainEvent()
    object Station2ConfirmDestinations : TrainEvent()
    object Station2ShowDestinationSearch : TrainEvent()
    object Station2DismissDestinationSearch : TrainEvent()
    data class Station2AddCustomDestination(val destination: String) : TrainEvent()
    object Station2ClearStation : TrainEvent()
    data class SelectDepartureFromStation2(val index: Int) : TrainEvent()
    object DismissStation2Details : TrainEvent()
    object ShowStationSwitcher : TrainEvent()
    object DismissStationSwitcher : TrainEvent()
    data class SwitchToStation(val station: Int) : TrainEvent()
}

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrainRepository()
    private val geocoderRepository = GeocoderRepository()
    private val stationPreferences = StationPreferences(application)
    private val stationPreferences2 = StationPreferences(application, slot = 2)
    private val _uiState = MutableStateFlow(
        TrainUiState(
            selectedStationId = stationPreferences.getSelectedStationId(),
            selectedStationName = stationPreferences.getSelectedStationName(),
            selectedLineId = stationPreferences.getSelectedLineId(),
            selectedLinePublicCode = stationPreferences.getSelectedLinePublicCode(),
            selectedDestinations = stationPreferences.getSelectedDestinations().toSet(),
            destinationStationId = stationPreferences.getDestinationStationId(),
            destinationStationName = stationPreferences.getDestinationStationName(),
            station2Id = stationPreferences2.getSelectedStationId(),
            station2Name = stationPreferences2.getSelectedStationName(),
            station2SelectedLineId = stationPreferences2.getSelectedLineId(),
            station2SelectedLinePublicCode = stationPreferences2.getSelectedLinePublicCode(),
            station2SelectedDestinations = stationPreferences2.getSelectedDestinations().toSet()
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
            is TrainEvent.ShowDestinationStationSelection -> showDestinationStationSelection()
            is TrainEvent.DismissDestinationStationSelection -> dismissDestinationStationSelection()
            is TrainEvent.UpdateDestinationStationSearchQuery -> updateDestinationStationSearchQuery(event.query)
            is TrainEvent.SelectDestinationStation -> selectDestinationStation(event.result)

            is TrainEvent.ShowStation2Settings -> showStation2Settings()
            is TrainEvent.DismissStation2Settings -> dismissStation2Settings()
            is TrainEvent.Station2ToggleSearch -> station2ToggleSearch()
            is TrainEvent.Station2UpdateSearchQuery -> station2UpdateSearchQuery(event.query)
            is TrainEvent.Station2SelectSearchResult -> station2SelectSearchResult(event.result)
            is TrainEvent.Station2ShowLineSelection -> station2ShowLineSelection()
            is TrainEvent.Station2DismissLineSelection -> station2DismissLineSelection()
            is TrainEvent.Station2SelectLine -> station2SelectLine(event.lineInfo)
            is TrainEvent.Station2ToggleDestination -> station2ToggleDestination(event.destination)
            is TrainEvent.Station2ConfirmDestinations -> station2ConfirmDestinations()
            is TrainEvent.Station2ShowDestinationSearch -> station2ShowDestinationSearch()
            is TrainEvent.Station2DismissDestinationSearch -> station2DismissDestinationSearch()
            is TrainEvent.Station2AddCustomDestination -> station2AddCustomDestination(event.destination)
            is TrainEvent.Station2ClearStation -> station2ClearStation()
            is TrainEvent.SelectDepartureFromStation2 -> selectDepartureFromStation2(event.index)
            is TrainEvent.DismissStation2Details -> dismissStation2Details()
            is TrainEvent.ShowStationSwitcher -> _uiState.value = _uiState.value.copy(showStationSwitcher = true)
            is TrainEvent.DismissStationSwitcher -> _uiState.value = _uiState.value.copy(showStationSwitcher = false)
            is TrainEvent.SwitchToStation -> switchToStation(event.station)
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
        _uiState.value = _uiState.value.copy(
            selectedDepartureIndex = index,
            arrivalInfo = null,
            isLoadingArrival = false
        )
        val destStationId = _uiState.value.destinationStationId ?: return
        val fromStationId = _uiState.value.selectedStationId ?: return
        val departures = _uiState.value.trainData?.departures ?: return
        val departure = departures.getOrNull(index) ?: return
        
        val now = ZonedDateTime.now(java.time.ZoneId.systemDefault())
        val localTime = LocalTime.parse(departure.aimedTime, DateTimeFormatter.ofPattern("HH:mm"))
        var departureDate = now.toLocalDate()
        var departureDateTime = ZonedDateTime.of(departureDate, localTime, now.zone)
        
        if (departureDateTime.isBefore(now)) {
            departureDate = departureDate.plusDays(1)
            departureDateTime = ZonedDateTime.of(departureDate, localTime, now.zone)
        }
        
        val departureDateTimeString = departureDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingArrival = true)
            val arrival = repository.fetchArrivalTime(
                fromStopPlaceId = fromStationId,
                toStopPlaceId = destStationId,
                departureDateTime = departureDateTimeString,
                lineId = _uiState.value.selectedLineId
            )
            val current = _uiState.value
            if (current.selectedDepartureIndex == index) {
                _uiState.value = current.copy(arrivalInfo = arrival, isLoadingArrival = false)
            }
        }
    }
    private fun dismissDetails() {
        _uiState.value = _uiState.value.copy(
            selectedDepartureIndex = null,
            arrivalInfo = null,
            isLoadingArrival = false
        )
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
    private fun showDestinationStationSelection() {
        _uiState.value = _uiState.value.copy(
            showDestinationStationSelection = true,
            destinationStationSearchQuery = "",
            destinationStationSearchResults = emptyList()
        )
    }
    private fun dismissDestinationStationSelection() {
        _uiState.value = _uiState.value.copy(
            showDestinationStationSelection = false,
            destinationStationSearchQuery = "",
            destinationStationSearchResults = emptyList(),
            isDestinationStationSearchLoading = false
        )
    }
    private fun updateDestinationStationSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(destinationStationSearchQuery = query)
        if (query.length >= 2) {
            searchDestinationStations(query)
        } else {
            _uiState.value = _uiState.value.copy(
                destinationStationSearchResults = emptyList(),
                isDestinationStationSearchLoading = false
            )
        }
    }
    private fun searchDestinationStations(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDestinationStationSearchLoading = true)
            val results = geocoderRepository.searchStations(query)
            _uiState.value = _uiState.value.copy(
                destinationStationSearchResults = results,
                isDestinationStationSearchLoading = false
            )
        }
    }
    private fun selectDestinationStation(result: StationSearchResult) {
        stationPreferences.setDestinationStation(result.id, result.name)
        _uiState.value = _uiState.value.copy(
            destinationStationId = result.id,
            destinationStationName = result.name,
            showDestinationStationSelection = false,
            destinationStationSearchQuery = "",
            destinationStationSearchResults = emptyList()
        )
    }

    private fun showStation2Settings() {
        _uiState.value = _uiState.value.copy(showStation2Settings = true)
    }

    private fun dismissStation2Settings() {
        _uiState.value = _uiState.value.copy(
            showStation2Settings = false,
            station2IsSearching = false,
            station2SearchQuery = "",
            station2SearchResults = emptyList(),
            station2ShowLineSelection = false,
            station2ShowDestinationSelection = false,
            station2ShowDestinationSearch = false
        )
    }

    private fun station2ToggleSearch() {
        val isSearching = _uiState.value.station2IsSearching
        _uiState.value = _uiState.value.copy(
            station2IsSearching = !isSearching,
            station2SearchQuery = "",
            station2SearchResults = emptyList()
        )
    }

    private fun station2UpdateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(station2SearchQuery = query)
        if (query.length >= 2) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(station2IsSearchLoading = true)
                val results = geocoderRepository.searchStations(query)
                _uiState.value = _uiState.value.copy(station2SearchResults = results, station2IsSearchLoading = false)
            }
        } else {
            _uiState.value = _uiState.value.copy(station2SearchResults = emptyList(), station2IsSearchLoading = false)
        }
    }

    private fun station2SelectSearchResult(result: StationSearchResult) {
        stationPreferences2.setSelectedStation(result.id, result.name)
        stationPreferences2.addStationToRecent(result.id, result.name)
        _uiState.value = _uiState.value.copy(
            station2Id = result.id,
            station2Name = result.name,
            station2IsSearching = false,
            station2SearchQuery = "",
            station2SearchResults = emptyList(),
            station2SelectedLineId = null,
            station2SelectedLinePublicCode = null,
            station2SelectedDestinations = emptySet()
        )
    }

    private fun station2ShowLineSelection() {
        _uiState.value = _uiState.value.copy(station2ShowLineSelection = true, station2IsLoadingLines = true, station2AvailableLines = emptyList())
        val stopPlaceId = _uiState.value.station2Id ?: return
        viewModelScope.launch {
            val lines = repository.fetchLines(stopPlaceId).sortedBy { it.publicCode }
            _uiState.value = _uiState.value.copy(station2AvailableLines = lines, station2IsLoadingLines = false)
        }
    }

    private fun station2DismissLineSelection() {
        _uiState.value = _uiState.value.copy(station2ShowLineSelection = false, station2AvailableLines = emptyList(), station2IsLoadingLines = false)
    }

    private fun station2SelectLine(lineInfo: LineInfo) {
        stationPreferences2.setSelectedLine(lineInfo.id, lineInfo.publicCode)
        _uiState.value = _uiState.value.copy(
            station2ShowLineSelection = false,
            station2AvailableLines = emptyList(),
            station2SelectedLineId = lineInfo.id,
            station2SelectedLinePublicCode = lineInfo.publicCode,
            station2ShowDestinationSelection = true,
            station2IsLoadingDestinations = true,
            station2AvailableDestinations = emptyList()
        )
        val stopPlaceId = _uiState.value.station2Id ?: return
        viewModelScope.launch {
            val destinations = repository.fetchDestinations(stopPlaceId, lineInfo.id)
            _uiState.value = _uiState.value.copy(
                station2AvailableDestinations = destinations,
                station2SelectedDestinations = destinations.toSet(),
                station2IsLoadingDestinations = false
            )
        }
    }

    private fun station2ToggleDestination(destination: String) {
        val current = _uiState.value.station2SelectedDestinations
        val next = if (destination in current) current - destination else current + destination
        _uiState.value = _uiState.value.copy(station2SelectedDestinations = next)
    }

    private fun station2ConfirmDestinations() {
        val selected = _uiState.value.station2SelectedDestinations
        stationPreferences2.setSelectedDestinations(selected)
        _uiState.value = _uiState.value.copy(
            station2ShowDestinationSelection = false,
            station2AvailableDestinations = emptyList(),
            station2SelectedDestinations = selected
        )
    }

    private fun station2ShowDestinationSearch() {
        _uiState.value = _uiState.value.copy(station2ShowDestinationSearch = true, station2DestinationSearchQuery = "")
    }

    private fun station2DismissDestinationSearch() {
        _uiState.value = _uiState.value.copy(station2ShowDestinationSearch = false, station2DestinationSearchQuery = "")
    }

    private fun station2AddCustomDestination(destination: String) {
        val trimmed = destination.trim()
        if (trimmed.isEmpty()) return
        val updated = _uiState.value.station2SelectedDestinations + trimmed
        val dests = _uiState.value.station2AvailableDestinations
        val newDests = if (trimmed in dests) dests else dests + trimmed
        _uiState.value = _uiState.value.copy(
            station2SelectedDestinations = updated,
            station2ShowDestinationSearch = false,
            station2DestinationSearchQuery = "",
            station2AvailableDestinations = newDests
        )
    }

    private fun station2ClearStation() {
        stationPreferences2.setSelectedStation(null, null)
        stationPreferences2.setSelectedLine(null, null)
        stationPreferences2.setSelectedDestinations(emptySet())
        _uiState.value = _uiState.value.copy(
            station2Id = null,
            station2Name = null,
            station2SelectedLineId = null,
            station2SelectedLinePublicCode = null,
            station2SelectedDestinations = emptySet()
        )
    }

    private fun selectDepartureFromStation2(index: Int) {
        val existingData = _uiState.value.station2TrainData
        if (existingData != null) {
            _uiState.value = _uiState.value.copy(station2SelectedDepartureIndex = index)
            return
        }
        val stationId = _uiState.value.station2Id ?: return
        _uiState.value = _uiState.value.copy(
            station2SelectedDepartureIndex = index,
            station2IsLoadingTrainData = true
        )
        viewModelScope.launch {
            val lineId = _uiState.value.station2SelectedLineId
            val destinations = _uiState.value.station2SelectedDestinations.takeIf { it.isNotEmpty() }
            val data = repository.fetchTrainData(stationId, lineId, destinations, cacheContext = getApplication())
            _uiState.value = _uiState.value.copy(
                station2TrainData = data,
                station2IsLoadingTrainData = false
            )
        }
    }

    private fun dismissStation2Details() {
        _uiState.value = _uiState.value.copy(
            station2SelectedDepartureIndex = null
        )
    }

    private fun switchToStation(station: Int) {
        _uiState.value = _uiState.value.copy(
            activeStation = station,
            showStationSwitcher = false
        )
        if (station == 2) {
            val stationId = _uiState.value.station2Id ?: return
            if (_uiState.value.station2TrainData == null) {
                _uiState.value = _uiState.value.copy(station2IsLoadingTrainData = true)
                viewModelScope.launch {
                    val lineId = _uiState.value.station2SelectedLineId
                    val destinations = _uiState.value.station2SelectedDestinations.takeIf { it.isNotEmpty() }
                    val data = repository.fetchTrainData(stationId, lineId, destinations, cacheContext = getApplication())
                    _uiState.value = _uiState.value.copy(
                        station2TrainData = data,
                        station2IsLoadingTrainData = false
                    )
                }
            }
        }
    }
}
