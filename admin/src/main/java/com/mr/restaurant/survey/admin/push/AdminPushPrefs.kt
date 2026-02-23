package com.mr.restaurant.survey.admin.push

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminPushPrefs @Inject constructor(
	@ApplicationContext context: Context
) {
	private val prefs = context.getSharedPreferences("admin_push", Context.MODE_PRIVATE)

	fun saveTenantId(tenantId: String) {
		prefs.edit().putString(KEY_TENANT_ID, tenantId).apply()
	}

	fun getTenantId(): String? = prefs.getString(KEY_TENANT_ID, null)?.takeIf { it.isNotBlank() }

	/**
	 * Returns true if this alertId was already seen recently.
	 * Keeps a small rolling window to avoid duplicate notifications from retries/topic overlap.
	 */
	fun wasAlertSeenRecently(alertId: String, maxIds: Int = 50): Boolean {
		val cleanId = alertId.trim()
		if (cleanId.isBlank()) return false
		val ids = prefs.getString(KEY_RECENT_ALERT_IDS, null)
			.orEmpty()
			.split(SEP)
			.map { it.trim() }
			.filter { it.isNotBlank() }
			.toMutableList()
		val duplicate = ids.contains(cleanId)
		if (!duplicate) {
			ids.add(0, cleanId)
			if (ids.size > maxIds) {
				ids.subList(maxIds, ids.size).clear()
			}
			prefs.edit().putString(KEY_RECENT_ALERT_IDS, ids.joinToString(SEP)).apply()
		}
		return duplicate
	}

	private companion object {
		const val KEY_TENANT_ID = "tenant_id"
		const val KEY_RECENT_ALERT_IDS = "recent_alert_ids"
		const val SEP = "|"
	}
}
