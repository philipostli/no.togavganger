package no.togavganger.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.togavganger.data.Departure
import no.togavganger.data.LineInfo
import no.togavganger.data.TrainData
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TrainRepository {
    suspend fun fetchLines(stopPlaceId: String): List<LineInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val query = """
                {
                  stopPlace(id: "$stopPlaceId") {
                    name
                    estimatedCalls(
                        numberOfDepartures: 30
                        whiteListedModes: [rail]
                        includeCancelledTrips: true
                    ) {
                        serviceJourney {
                            line {
                                id
                                publicCode
                                presentation {
                                    textColour
                                    colour
                                }
                            }
                        }
                    }
                  }
                }
                """.trimIndent()
                val response = executeGraphQL(query)
                if (response == null) return@withContext emptyList()
                val stopPlace = response.getJSONObject("data").getJSONObject("stopPlace")
                val estimatedCalls = stopPlace.getJSONArray("estimatedCalls")
                val seen = mutableSetOf<String>()
                val lines = mutableListOf<LineInfo>()
                for (i in 0 until estimatedCalls.length()) {
                    val call = estimatedCalls.getJSONObject(i)
                    val line = call.optJSONObject("serviceJourney")?.optJSONObject("line") ?: continue
                    val id = line.getString("id")
                    if (id in seen) continue
                    seen.add(id)
                    val publicCode = line.optString("publicCode", "")
                    val presentation = line.optJSONObject("presentation")
                    val textColour = presentation?.optString("textColour")?.takeIf { it.isNotBlank() } ?: "000000"
                    val colour = presentation?.optString("colour")?.takeIf { it.isNotBlank() } ?: "FFFFFF"
                    lines.add(LineInfo(id = id, publicCode = publicCode, textColour = textColour, colour = colour))
                }
                lines
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchDestinations(stopPlaceId: String, lineId: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val query = """
                {
                  stopPlace(id: "$stopPlaceId") {
                    name
                    estimatedCalls(
                        numberOfDepartures: 10
                        whiteListedModes: [rail]
                        whiteListed: { lines: "$lineId" }
                        includeCancelledTrips: true
                    ) {
                        destinationDisplay { frontText }
                        serviceJourney {
                            line { id publicCode }
                        }
                    }
                  }
                }
                """.trimIndent()
                val response = executeGraphQL(query)
                if (response == null) return@withContext emptyList()
                val stopPlace = response.getJSONObject("data").getJSONObject("stopPlace")
                val estimatedCalls = stopPlace.getJSONArray("estimatedCalls")
                val seen = mutableSetOf<String>()
                val destinations = mutableListOf<String>()
                for (i in 0 until estimatedCalls.length()) {
                    val call = estimatedCalls.getJSONObject(i)
                    val dest = call.optJSONObject("destinationDisplay")?.optString("frontText") ?: continue
                    if (dest !in seen) {
                        seen.add(dest)
                        destinations.add(dest)
                    }
                }
                destinations
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseNorwegianSummary(call: JSONObject): String? {
        val situations = call.optJSONArray("situations") ?: return null
        val values = mutableListOf<String>()
        for (i in 0 until situations.length()) {
            val summaries = situations.getJSONObject(i).optJSONArray("summary")
            if (summaries == null) continue
            for (j in 0 until summaries.length()) {
                val s = summaries.getJSONObject(j)
                if (s.optString("language") == "no") {
                    val value = s.optString("value", "").trim()
                    if (value.isNotEmpty()) values.add(value)
                }
            }
        }
        return values.distinct().joinToString("\n").takeIf { it.isNotEmpty() }
    }

    private fun executeGraphQL(query: String): JSONObject? {
        val url = URL("https://api.entur.io/journey-planner/v3/graphql")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("ET-Client-Name", "togavganger.no")
        connection.doOutput = true
        val body = JSONObject().apply { put("query", query) }
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(response)
    }

    suspend fun fetchTrainData(stopPlaceId: String, lineId: String? = null, destinations: Set<String>? = null): TrainData {
        return withContext(Dispatchers.IO) {
            try {
                val whiteListedClause = if (lineId != null) "\n                        whiteListed: { lines: \"$lineId\" }" else ""
                val query = """
                {
                  stopPlace(id: "$stopPlaceId") {
                    name
                    estimatedCalls(
                        numberOfDepartures: 10 
                        whiteListedModes: [rail]
                        $whiteListedClause
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
                        situations {
                            id
                            description {
                                value
                                language
                            }
                            summary {
                                value
                                language
                            }
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
                val jsonResponse = executeGraphQL(query)
                if (jsonResponse == null) {
                    return@withContext TrainData("API Feil", "", emptyList())
                }
                val stopPlace = jsonResponse.getJSONObject("data").getJSONObject("stopPlace")
                val stopName = stopPlace.getString("name")
                val estimatedCalls = stopPlace.getJSONArray("estimatedCalls")
                val departures = mutableListOf<Departure>()
                var topLevelLineCode = ""
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val destinationsFilter = destinations?.takeIf { it.isNotEmpty() }
                for (i in 0 until estimatedCalls.length()) {
                    val call = estimatedCalls.getJSONObject(i)
                    val dest = call.getJSONObject("destinationDisplay").getString("frontText")
                    if (destinationsFilter != null && dest !in destinationsFilter) continue
                    val lineCode = call.getJSONObject("serviceJourney").getJSONObject("line").getString("publicCode")
                    if (topLevelLineCode.isEmpty()) {
                        topLevelLineCode = lineCode
                    }
                    val quay = call.optJSONObject("quay")
                    val platformCode = quay?.optString("publicCode")
                    val aimedTimeRaw = call.getString("aimedDepartureTime")
                    val expectedTimeRaw = call.optString("expectedDepartureTime", aimedTimeRaw)
                    val aimedDateTime = ZonedDateTime.parse(aimedTimeRaw)
                    val expectedDateTime = ZonedDateTime.parse(expectedTimeRaw)
                    val isDelayed = expectedDateTime.truncatedTo(ChronoUnit.MINUTES)
                        .isAfter(aimedDateTime.truncatedTo(ChronoUnit.MINUTES))
                    val aimedTime = aimedDateTime.format(timeFormatter)
                    val expectedTime = expectedDateTime.format(timeFormatter)
                    val summary = parseNorwegianSummary(call)
                    departures.add(Departure(dest, aimedTime, expectedTime, isDelayed, platformCode, summary))
                }
                TrainData(stopName, topLevelLineCode, departures)
            } catch (e: Exception) {
                TrainData("Feil", e.javaClass.simpleName, emptyList())
            }
        }
    }
}
