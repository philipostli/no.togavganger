package no.togavganger.data

data class TrainData(val stopName: String, val lineCode: String, val departures: List<Departure>)

data class Departure(
    val destination: String,
    val aimedTime: String,
    val expectedTime: String,
    val isDelayed: Boolean,
    val platformCode: String? = null
)
