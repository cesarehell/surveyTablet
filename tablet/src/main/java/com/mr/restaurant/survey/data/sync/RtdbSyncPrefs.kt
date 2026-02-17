package com.mr.restaurant.survey.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "rtdb_sync")

class RtdbSyncPrefs(private val context: Context) {

	private val LAST_EVENT_ID = stringPreferencesKey("last_event_id")

	suspend fun getLastEventId(): String? {
		val prefs = context.dataStore.data.first()
		return prefs[LAST_EVENT_ID]
	}

	suspend fun setLastEventId(eventId: String) {
		context.dataStore.edit { it[LAST_EVENT_ID] = eventId }
	}
}
