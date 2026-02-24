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

	fun saveLastToken(token: String?) {
		prefs.edit()
			.putString(KEY_LAST_TOKEN, token)
			.putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
			.apply()
	}

	fun getLastToken(): String? = prefs.getString(KEY_LAST_TOKEN, null)?.takeIf { it.isNotBlank() }

	fun saveLastRegistrationResult(
		topic: String?,
		pushSubscribed: Boolean?,
		error: String?
	) {
		prefs.edit()
			.putString(KEY_LAST_TOPIC, topic)
			.putString(KEY_LAST_ERROR, error)
			.putString(KEY_LAST_PUSH_SUBSCRIBED, pushSubscribed?.toString())
			.putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
			.apply()
	}

	fun saveLastLocalTopic(topic: String?, error: String?) {
		prefs.edit()
			.putString(KEY_LAST_LOCAL_TOPIC, topic)
			.putString(KEY_LAST_LOCAL_TOPIC_ERROR, error)
			.putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
			.apply()
	}

	fun getLastTopic(): String? = prefs.getString(KEY_LAST_TOPIC, null)
	fun getLastError(): String? = prefs.getString(KEY_LAST_ERROR, null)
	fun getLastLocalTopic(): String? = prefs.getString(KEY_LAST_LOCAL_TOPIC, null)
	fun getLastLocalTopicError(): String? = prefs.getString(KEY_LAST_LOCAL_TOPIC_ERROR, null)
	fun getLastPushSubscribed(): Boolean? = prefs.getString(KEY_LAST_PUSH_SUBSCRIBED, null)?.toBooleanStrictOrNull()
	fun getLastUpdatedAtMillis(): Long? = prefs.getLong(KEY_LAST_UPDATED_AT, 0L).takeIf { it > 0L }

	fun clearDebug() {
		prefs.edit()
			.remove(KEY_LAST_TOKEN)
			.remove(KEY_LAST_TOPIC)
			.remove(KEY_LAST_ERROR)
			.remove(KEY_LAST_LOCAL_TOPIC)
			.remove(KEY_LAST_LOCAL_TOPIC_ERROR)
			.remove(KEY_LAST_PUSH_SUBSCRIBED)
			.remove(KEY_LAST_UPDATED_AT)
			.apply()
	}

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
		const val KEY_LAST_TOKEN = "last_token"
		const val KEY_LAST_TOPIC = "last_topic"
		const val KEY_LAST_ERROR = "last_error"
		const val KEY_LAST_LOCAL_TOPIC = "last_local_topic"
		const val KEY_LAST_LOCAL_TOPIC_ERROR = "last_local_topic_error"
		const val KEY_LAST_PUSH_SUBSCRIBED = "last_push_subscribed"
		const val KEY_LAST_UPDATED_AT = "last_updated_at"
		const val KEY_RECENT_ALERT_IDS = "recent_alert_ids"
		const val SEP = "|"
	}
}
