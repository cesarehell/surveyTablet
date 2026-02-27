package com.mr.restaurant.survey.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.mr.restaurant.survey.net.dto.AnswerDto
import com.mr.restaurant.survey.net.dto.SubmitSurveyRequestDto
import com.mr.restaurant.survey.net.dto.SurveyTemplateDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class PendingSurveySubmission(
	val localId: String,
	val tenantId: String,
	val templateId: String,
	val locationId: String? = null,
	val tableNo: String? = null,
	val waiterName: String? = null,
	val answers: List<AnswerDto> = emptyList(),
	val submit: SubmitSurveyRequestDto = SubmitSurveyRequestDto(),
	val createdAtMillis: Long = System.currentTimeMillis(),
)

class TabletOfflineStore(context: Context) {
	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
	private val json = Json {
		ignoreUnknownKeys = true
		explicitNulls = false
		isLenient = true
	}

	fun saveCachedTemplate(template: SurveyTemplateDto) {
		runCatching { json.encodeToString(SurveyTemplateDto.serializer(), template) }
			.onSuccess { prefs.edit { putString(KEY_CACHED_TEMPLATE, it) } }
			.onFailure { Log.w(TAG, "No se pudo cachear template: ${it.message}") }
	}

	fun loadCachedTemplate(): SurveyTemplateDto? {
		val raw = prefs.getString(KEY_CACHED_TEMPLATE, null) ?: return null
		return runCatching { json.decodeFromString(SurveyTemplateDto.serializer(), raw) }
			.onFailure { Log.w(TAG, "No se pudo leer template cacheado: ${it.message}") }
			.getOrNull()
	}

	fun enqueuePending(item: PendingSurveySubmission) {
		val current = loadPending().toMutableList()
		current.add(item)
		savePending(current)
	}

	fun pendingCount(): Int = loadPending().size

	suspend fun syncPending(repo: SurveyRepository): SyncResult {
		val pending = loadPending()
		if (pending.isEmpty()) return SyncResult(0, 0)

		val remaining = mutableListOf<PendingSurveySubmission>()
		var synced = 0
		for (item in pending) {
			val result = runCatching {
				val inst = repo.startSurvey(
					tenant = item.tenantId,
					templateId = item.templateId,
					locationId = item.locationId,
					table = item.tableNo,
					waiter = item.waiterName
				).instanceId
				if (item.answers.isNotEmpty()) {
					repo.sendAnswersBatch(inst, item.answers)
				}
				repo.submit(inst, item.submit.email, item.submit.marketingOptIn)
			}
			if (result.isSuccess) {
				synced++
			} else {
				Log.w(TAG, "Sync pendiente falló localId=${item.localId}: ${result.exceptionOrNull()?.message}")
				remaining.add(item)
			}
		}
		savePending(remaining)
		return SyncResult(synced = synced, remaining = remaining.size)
	}

	private fun loadPending(): List<PendingSurveySubmission> {
		val raw = prefs.getString(KEY_PENDING_LIST, null) ?: return emptyList()
		return runCatching {
			json.decodeFromString(ListSerializer(PendingSurveySubmission.serializer()), raw)
		}.onFailure {
			Log.w(TAG, "No se pudo leer cola pendiente: ${it.message}")
		}.getOrDefault(emptyList())
	}

	private fun savePending(items: List<PendingSurveySubmission>) {
		runCatching {
			json.encodeToString(ListSerializer(PendingSurveySubmission.serializer()), items)
		}.onSuccess {
			prefs.edit { putString(KEY_PENDING_LIST, it) }
		}.onFailure {
			Log.e(TAG, "No se pudo persistir cola pendiente: ${it.message}", it)
		}
	}

	data class SyncResult(val synced: Int, val remaining: Int)

	private companion object {
		const val PREFS_NAME = "tablet_offline_store"
		const val KEY_CACHED_TEMPLATE = "cached_template"
		const val KEY_PENDING_LIST = "pending_surveys"
		const val TAG = "TABLET_OFFLINE"
	}
}
