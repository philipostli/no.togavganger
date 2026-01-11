package no.togavganger.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.togavganger.data.Departure
import no.togavganger.data.TrainData
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TrainRepository {
    suspend fun fetchTrainData(): TrainData {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.entur.io/journey-planner/v3/graphql")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("ET-Client-Name", "philip-wear-tiles-codelab")
                connection.doOutput = true
                val query = """
                {
                  stopPlace(id: "NSR:StopPlace:59653") {
                    name
                    estimatedCalls(
                        numberOfDepartures: 10 
                        whiteListedModes: [rail]
                        includeCancelledTrips: true
                    ) {
                        aimedDepartureTime
                        expectedDepartureTime
                        destinationDisplay {
                            frontText
                        }
                        quay {
                            publicCode
                        }
                        serviceJourney {
                            line {
                                publicCode
                            }
                        }
                    }
                  }
                }
                """.trimIndent()
                val body = JSONObject().apply {
                    put("query", query)
                }
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext TrainData("API Feil", responseCode.toString(), emptyList())
                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val stopPlace = jsonResponse.getJSONObject("data").getJSONObject("stopPlace")
                val stopName = stopPlace.getString("name")
                val estimatedCalls = stopPlace.getJSONArray("estimatedCalls")
                val departures = mutableListOf<Departure>()
                var topLevelLineCode = ""
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                for (i in 0 until estimatedCalls.length()) {
                    val call = estimatedCalls.getJSONObject(i)
                    val dest = call.getJSONObject("destinationDisplay").getString("frontText")
                    if (dest !in listOf("Spikkestad", "Asker", "Oslo S", "Drammen")) continue
                    val lineCode = call.getJSONObject("serviceJourney").getJSONObject("line").getString("publicCode")
                    if (topLevelLineCode.isEmpty()) {
                        topLevelLineCode = lineCode
                    }
                    val quay = call.optJSONObject("quay")
                    val platformCode = quay?.optString("publicCode", null)
                    val aimedTimeRaw = call.getString("aimedDepartureTime")
                    val expectedTimeRaw = call.optString("expectedDepartureTime", aimedTimeRaw)
                    val aimedDateTime = ZonedDateTime.parse(aimedTimeRaw)
                    val expectedDateTime = ZonedDateTime.parse(expectedTimeRaw)
                    val isDelayed = expectedDateTime.truncatedTo(ChronoUnit.MINUTES)
                        .isAfter(aimedDateTime.truncatedTo(ChronoUnit.MINUTES))
                    val aimedTime = aimedDateTime.format(timeFormatter)
                    val expectedTime = expectedDateTime.format(timeFormatter)
                    departures.add(Departure(dest, aimedTime, expectedTime, isDelayed, platformCode))
                }
                TrainData(stopName, topLevelLineCode, departures)
            } catch (e: Exception) {
                TrainData("Feil", e.javaClass.simpleName, emptyList())
            }
        }
    }
}
