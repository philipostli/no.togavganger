package no.togavganger.data

data class TrainData(
    val stopName: String,
    val lineCode: String,
    val departures: List<Departure>,
    val isApiError: Boolean = false
)

data class LineInfo(
    val id: String,
    val publicCode: String,
    val textColour: String,
    val colour: String
)

data class Departure(
    val destination: String,
    val aimedTime: String,
    val expectedTime: String,
    val isDelayed: Boolean,
    val platformCode: String? = null,
    val summary: String? = null,
    val description: String? = null
)
