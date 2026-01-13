package no.togavganger.data.preferences

import android.content.Context
import android.content.SharedPreferences

class StationPreferences(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    fun getSelectedStationId(): String? {
        return preferences.getString(KEY_SELECTED_STATION_ID, null)
    }
    fun getSelectedStationName(): String? {
        return preferences.getString(KEY_SELECTED_STATION_NAME, null)
    }
    fun setSelectedStation(stationId: String?, stationName: String?) {
        preferences.edit().apply {
            putString(KEY_SELECTED_STATION_ID, stationId)
            putString(KEY_SELECTED_STATION_NAME, stationName)
        }.apply()
    }
    companion object {
        private const val PREFS_NAME = "station_preferences"
        private const val KEY_SELECTED_STATION_ID = "selected_station_id"
        private const val KEY_SELECTED_STATION_NAME = "selected_station_name"
    }
}
