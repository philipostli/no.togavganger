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
            remove(KEY_SELECTED_LINE_ID)
            remove(KEY_SELECTED_LINE_PUBLIC_CODE)
            remove(KEY_SELECTED_DESTINATIONS)
        }.apply()
    }
    fun getSelectedLineId(): String? = preferences.getString(KEY_SELECTED_LINE_ID, null)
    fun getSelectedLinePublicCode(): String? = preferences.getString(KEY_SELECTED_LINE_PUBLIC_CODE, null)
    fun setSelectedLine(lineId: String?, publicCode: String?) {
        preferences.edit().apply {
            putString(KEY_SELECTED_LINE_ID, lineId)
            putString(KEY_SELECTED_LINE_PUBLIC_CODE, publicCode)
        }.apply()
    }
    fun getSelectedDestinations(): Set<String> = preferences.getStringSet(KEY_SELECTED_DESTINATIONS, null) ?: emptySet()
    fun setSelectedDestinations(destinations: Set<String>) {
        preferences.edit().apply {
            putStringSet(KEY_SELECTED_DESTINATIONS, destinations)
        }.apply()
    }
    companion object {
        private const val PREFS_NAME = "station_preferences"
        private const val KEY_SELECTED_STATION_ID = "selected_station_id"
        private const val KEY_SELECTED_STATION_NAME = "selected_station_name"
        private const val KEY_SELECTED_LINE_ID = "selected_line_id"
        private const val KEY_SELECTED_LINE_PUBLIC_CODE = "selected_line_public_code"
        private const val KEY_SELECTED_DESTINATIONS = "selected_destinations"
    }
}
