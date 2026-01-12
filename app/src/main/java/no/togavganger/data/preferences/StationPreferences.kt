package no.togavganger.data.preferences

import android.content.Context
import android.content.SharedPreferences

class StationPreferences(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    fun getSelectedStation(): String? {
        return preferences.getString(KEY_SELECTED_STATION, null)
    }
    fun setSelectedStation(station: String?) {
        preferences.edit().putString(KEY_SELECTED_STATION, station).apply()
    }
    companion object {
        private const val PREFS_NAME = "station_preferences"
        private const val KEY_SELECTED_STATION = "selected_station"
    }
}
