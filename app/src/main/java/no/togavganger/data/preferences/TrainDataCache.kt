package no.togavganger.data.preferences

import android.content.Context
import android.content.SharedPreferences
import no.togavganger.data.Departure
import no.togavganger.data.TrainData
import org.json.JSONArray
import org.json.JSONObject

class TrainDataCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun get(stopPlaceId: String, lineId: String?, destinations: Set<String>?): TrainData? {
        val key = cacheKey(stopPlaceId, lineId, destinations) ?: return null
        val json = prefs.getString(key, null) ?: return null
        return try {
            parseTrainData(JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }

    fun put(stopPlaceId: String, lineId: String?, destinations: Set<String>?, data: TrainData) {
        if (data.departures.isEmpty()) return
        val key = cacheKey(stopPlaceId, lineId, destinations) ?: return
        prefs.edit().putString(key, serializeTrainData(data).toString()).apply()
    }

    private fun cacheKey(stopPlaceId: String, lineId: String?, destinations: Set<String>?): String? {
        val destKey = destinations?.sorted()?.joinToString(",") ?: ""
        return "train_${stopPlaceId}_${lineId ?: ""}_$destKey"
    }

    private fun serializeTrainData(d: TrainData): JSONObject {
        return JSONObject().apply {
            put("stopName", d.stopName)
            put("lineCode", d.lineCode)
            put("departures", JSONArray().apply {
                d.departures.forEach { dep ->
                    put(JSONObject().apply {
                        put("destination", dep.destination)
                        put("aimedTime", dep.aimedTime)
                        put("expectedTime", dep.expectedTime)
                        put("isDelayed", dep.isDelayed)
                        put("platformCode", dep.platformCode)
                        put("summary", dep.summary)
                        put("description", dep.description)
                    })
                }
            })
        }
    }

    private fun parseTrainData(obj: JSONObject): TrainData {
        val stopName = obj.getString("stopName")
        val lineCode = obj.getString("lineCode")
        val departures = obj.getJSONArray("departures").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Departure(
                    destination = o.getString("destination"),
                    aimedTime = o.getString("aimedTime"),
                    expectedTime = o.getString("expectedTime"),
                    isDelayed = o.getBoolean("isDelayed"),
                    platformCode = o.optString("platformCode").takeIf { it.isNotEmpty() },
                    summary = o.optString("summary").takeIf { it.isNotEmpty() },
                    description = o.optString("description").takeIf { it.isNotEmpty() }
                )
            }
        }
        return TrainData(stopName, lineCode, departures)
    }

    companion object {
        private const val PREFS_NAME = "train_data_cache"
    }
}
