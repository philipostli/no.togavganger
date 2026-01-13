package no.togavganger.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.togavganger.data.StationSearchResult
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeocoderRepository {
    suspend fun searchStations(query: String): List<StationSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                var results = performSearch(encodedQuery, 10)
                if (results.isEmpty()) {
                    results = performSearch(encodedQuery, 20)
                }
                results
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    private fun performSearch(encodedQuery: String, size: Int): List<StationSearchResult> {
        try {
            val url = URL("https://api.entur.io/geocoder/v1/autocomplete?lang=no&size=$size&text=$encodedQuery")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("ET-Client-Name", "togavganger.no")
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return emptyList()
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(response)
            val features = jsonResponse.getJSONArray("features")
            val results = mutableListOf<StationSearchResult>()
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val categoryArray = properties.optJSONArray("category")
                var hasRailStation = false
                if (categoryArray != null) {
                    for (j in 0 until categoryArray.length()) {
                        val category = categoryArray.getString(j)
                        if (category == "railStation") {
                            hasRailStation = true
                            break
                        }
                    }
                }
                if (hasRailStation) {
                    val id = properties.getString("id")
                    val name = properties.getString("name")
                    results.add(StationSearchResult(id, name))
                }
            }
            return results
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
