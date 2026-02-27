package com.mr.restaurant.survey.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.data.PendingSurveySubmission
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.data.TabletOfflineStore
import com.mr.restaurant.survey.data.sync.RtdbSyncListener
import com.mr.restaurant.survey.data.sync.RtdbSyncPrefs
import com.mr.restaurant.survey.core.net.ApiErrorMapper
import com.mr.restaurant.survey.net.dto.AnswerDto
import com.mr.restaurant.survey.net.dto.CouponViewDto
import com.mr.restaurant.survey.net.dto.SubmitSurveyRequestDto
import com.mr.restaurant.survey.net.dto.SurveyTemplateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

data class UiState(
	val loading: Boolean = true,
	val error: String? = null,
	val submitError: String? = null,
	val instanceId: String? = null,
	val template: SurveyTemplateDto? = null,
	val stepIndex: Int = -1,
	val celebrating: Boolean = false,
	val submitOutcome: SubmitOutcome? = null,
	val offlineMode: Boolean = false,
	val canUseOffline: Boolean = false,
	val pendingSyncCount: Int = 0,
)

data class SubmitOutcome(
	val offlineQueued: Boolean = false,
	val coupon: CouponViewDto? = null,
	val couponStatus: String? = null,
	val visitCount: Long? = null,
)

class AppViewModel(
	private val repo: SurveyRepository,
	private val tenant: String,
	private val templateId: String?,
	private val locationId: String? = null,
	private val deviceOwner: String? = null,
	private val appContext: Context, // debe ser Application context
	private val table: String = "",
	private val waiter: String = "",
	private val knownWaiters: List<String> = emptyList()
) : ViewModel() {

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state
	private val offlineStore = TabletOfflineStore(appContext)

	private var syncListener: RtdbSyncListener? = null
	private var runtimeLocationId: String? = locationId?.takeIf { it.isNotBlank() }
	private var runtimeTable: String = table
	private var runtimeWaiter: String = waiter
	private var offlineTemplateCandidate: SurveyTemplateDto? = null
	private var pendingTemplateFromSync: SurveyTemplateDto? = null
	private val draftAnswers = linkedMapOf<String, JsonElement>()
	private val syncRefreshMutex = Mutex()
	private val beginSurveyMutex = Mutex()
	private var lastAssignmentRefreshAtMs: Long = 0L

	init {
		bootstrap()
		startRealtimeSync()
	}

	private fun bootstrap() = viewModelScope.launch {
		setLoading()
		try {
			refreshRemoteAssignmentBestEffort(force = true)
			val template = loadInitialTemplate()
			withContext(Dispatchers.IO) { offlineStore.saveCachedTemplate(template) }
			val pendingCount = withContext(Dispatchers.IO) { offlineStore.pendingCount() }

			offlineTemplateCandidate = null
			_state.value = UiState(
				loading = false,
				instanceId = null,
				template = template,
				stepIndex = -1,
				offlineMode = false,
				canUseOffline = false,
				pendingSyncCount = pendingCount
			)

			// Ejecutar sync pendiente fuera de la ruta crítica de arranque UI.
			launch { syncPendingBestEffort() }
		} catch (e: Exception) {
			val cached = withContext(Dispatchers.IO) { offlineStore.loadCachedTemplate() }
			val pendingCount = withContext(Dispatchers.IO) { offlineStore.pendingCount() }
			offlineTemplateCandidate = cached
			_state.value = UiState(
				loading = false,
				error = e.message ?: "Error de red",
				canUseOffline = cached != null,
				pendingSyncCount = pendingCount
			)
		}
	}

	fun retryBootstrap() {
		bootstrap()
	}

	fun useOfflineMode() {
		viewModelScope.launch {
			val cached = offlineTemplateCandidate ?: withContext(Dispatchers.IO) { offlineStore.loadCachedTemplate() }
			if (cached == null) return@launch
			val pendingCount = withContext(Dispatchers.IO) { offlineStore.pendingCount() }
			draftAnswers.clear()
			_state.value = UiState(
				loading = false,
				instanceId = null,
				template = cached,
				stepIndex = -1,
				offlineMode = true,
				canUseOffline = false,
				pendingSyncCount = pendingCount
			)
		}
	}

	private suspend fun loadInitialTemplate(): SurveyTemplateDto =
		if (templateId != null) repo.loadTemplateWithOptions(templateId)
		else repo.pickCurrentTemplate(tenant, runtimeLocationId)

	private suspend fun startNewSurveyInstance(template: SurveyTemplateDto): String {
		val effectiveLocationId = resolveLocationId(template)
		runtimeLocationId = effectiveLocationId

		android.util.Log.i(
			"SURVEY",
			"Iniciando encuesta (app) | tenant=$tenant | templateId=${template.id} | locationId=$effectiveLocationId | table=$runtimeTable | waiter=$runtimeWaiter"
		)

		return repo.startSurvey(
			tenant = tenant,
			templateId = template.id,
			locationId = effectiveLocationId,
			table = runtimeTable.takeIf { it.isNotBlank() },
			waiter = runtimeWaiter.takeIf { it.isNotBlank() }
		).instanceId
	}

	fun defaultTable(): String = runtimeTable

	fun beginSurvey(tableNo: String?, waiterName: String?) = viewModelScope.launch {
		beginSurveyMutex.withLock {
			val current = _state.value
			if (current.loading || current.stepIndex >= 0) return@withLock

			val template = current.template ?: run {
				setError("Template no cargado")
				return@withLock
			}
			if (template.questions.isNullOrEmpty()) {
				_state.update {
					it.copy(
						loading = false,
						error = null,
						submitError = "Encuesta no disponible: no tiene preguntas configuradas.",
						instanceId = null,
						stepIndex = -1
					)
				}
				return@withLock
			}
			refreshRemoteAssignmentBestEffort()
			runtimeTable = tableNo?.trim()?.takeIf { it.isNotBlank() } ?: runtimeTable.ifBlank { table }
			runtimeWaiter = waiterName?.trim()?.takeIf { it.isNotBlank() } ?: runtimeWaiter.ifBlank { waiter }
			draftAnswers.clear()

			if (current.offlineMode) {
				_state.update {
					it.copy(
						loading = false,
						error = null,
						submitError = null,
						instanceId = null,
						stepIndex = 0
					)
				}
				return@withLock
			}

			_state.update { it.copy(loading = true, error = null, submitError = null) }
			runCatching { startNewSurveyInstance(template) }
				.onSuccess { instanceId ->
					_state.update {
						it.copy(
							loading = false,
							error = null,
							submitError = null,
							instanceId = instanceId,
							stepIndex = 0,
							submitOutcome = null
						)
					}
				}
				.onFailure { e ->
					setError(e.message ?: "No se pudo iniciar la encuesta")
				}
		}
	}

	fun next() {
		_state.update { it.copy(stepIndex = it.stepIndex + 1) }
	}

	fun dismissSubmitOutcome() {
		adoptPendingTemplateIfAny()
		_state.update {
			it.copy(
				submitOutcome = null,
				celebrating = false,
				instanceId = null,
				stepIndex = -1,
				submitError = null,
				error = null
			)
		}
	}

	fun returnToWelcome() {
		adoptPendingTemplateIfAny()
		_state.update {
			it.copy(
				loading = false,
				error = null,
				submitError = null,
				instanceId = null,
				stepIndex = -1
			)
		}
	}

	fun postAnswer(qid: String, value: Any) = viewModelScope.launch {
		val answer = AnswerDto(questionId = qid, answer = toJsonElement(value))
		draftAnswers[qid] = answer.answer

		val current = _state.value
		if (current.offlineMode) return@launch
		val inst = current.instanceId ?: return@launch
		runCatching { repo.sendAnswersBatch(inst, listOf(answer)) }
			.onFailure { e ->
				val message = ApiErrorMapper.message(e)
				if (isDeletedQuestionAnswerError(message)) {
					recoverFromStaleQuestion(message)
					return@onFailure
				}
				setError(message)
			}
	}

	fun submitAndRestart(email: String?, marketingOptIn: Boolean?) = viewModelScope.launch {
		val current = _state.value
		val normalizedEmail = email?.trim()?.takeIf { it.isNotBlank() }
		_state.update { it.copy(submitError = null) }

		if (current.offlineMode) {
			val tpl = current.template ?: run {
				setError("Template no cargado")
				return@launch
			}
			offlineStore.enqueuePending(
				PendingSurveySubmission(
					localId = UUID.randomUUID().toString(),
					tenantId = tenant,
					templateId = tpl.id,
					locationId = runtimeLocationId,
					tableNo = runtimeTable,
					waiterName = runtimeWaiter,
					answers = draftAnswers.entries.map { (questionId, answer) ->
						AnswerDto(questionId = questionId, answer = answer)
					},
					submit = SubmitSurveyRequestDto(
						email = normalizedEmail,
						marketingOptIn = marketingOptIn
					)
				)
			)
			finishSurveyCycle(
				offlineMode = true,
				outcome = SubmitOutcome(
					offlineQueued = true,
					coupon = null,
					couponStatus = "PENDING_SYNC",
					visitCount = null
				)
			)
			return@launch
		}

		val inst = current.instanceId ?: return@launch

		runCatching { repo.submit(inst, email = normalizedEmail, marketingOptIn = marketingOptIn) }
			.onSuccess { response ->
				finishSurveyCycle(
					offlineMode = false,
					outcome = SubmitOutcome(
						offlineQueued = false,
						coupon = response.coupon,
						couponStatus = response.couponStatus,
						visitCount = response.visitCount
					)
				)
			}
			.onFailure { e ->
				val submitMessage = ApiErrorMapper.message(e)
				_state.update {
					it.copy(
						loading = false,
						submitError = submitMessage
					)
				}
			}
	}

	private fun startRealtimeSync() {
		val prefs = RtdbSyncPrefs(appContext)

		syncListener = RtdbSyncListener(
			tenantId = tenant,
			prefs = prefs,
			scope = viewModelScope,
			onTrigger = {
				syncRefreshMutex.withLock {
					runCatching {
						val current = _state.value
						val currentTemplateId = current.template?.id
						val currentTemplateFingerprint = current.template?.questionFingerprint()
						val newTpl = repo.refreshCurrentTemplate(tenant, runtimeLocationId)
						val templateIdChanged = currentTemplateId != null && currentTemplateId != newTpl.id
						val templateQuestionsChanged =
							current.template != null &&
								currentTemplateId == newTpl.id &&
								currentTemplateFingerprint != newTpl.questionFingerprint()
						val shouldResetInstance = templateIdChanged || templateQuestionsChanged
						val surveyInProgress = current.stepIndex >= 0 && current.submitOutcome == null

						if (surveyInProgress && shouldResetInstance) {
							pendingTemplateFromSync = newTpl
							withContext(Dispatchers.IO) { offlineStore.saveCachedTemplate(newTpl) }
							android.util.Log.i(
								"SURVEY",
								"Sync diferido durante encuesta activa | templateId=${newTpl.id}"
							)
							return@runCatching
						}
						val newInstanceId = if (shouldResetInstance) null else current.instanceId
						val newStepIndex = if (shouldResetInstance) -1 else current.stepIndex
						if (shouldResetInstance) {
							draftAnswers.clear()
						}

						_state.value = current.copy(
							template = newTpl,
							instanceId = newInstanceId,
							error = null,
							submitError = null,
							stepIndex = newStepIndex,
							offlineMode = false,
							canUseOffline = false
						)
						withContext(Dispatchers.IO) { offlineStore.saveCachedTemplate(newTpl) }
					}.onFailure { e ->
						setError(e.message ?: "Error refrescando encuesta")
					}
				}
			}
		).also { it.start() }
	}

	private fun resolveLocationId(template: SurveyTemplateDto): String? {
		val override = locationId?.takeIf { it.isNotBlank() }
		if (override != null) return override

		val scope = template.scope?.trim()?.uppercase()
		val inferred = template.location?.id?.takeIf { it.isNotBlank() }

		if (scope == "LOCATION") {
			require(inferred != null) {
				"Template LOCATION requiere locationId pero no viene en el template. Revisa /v1/templates y el DTO."
			}
		}
		return inferred
	}

	private suspend fun refreshRemoteAssignmentBestEffort(force: Boolean = false) {
		val now = System.currentTimeMillis()
		if (!force && now - lastAssignmentRefreshAtMs < ASSIGNMENT_REFRESH_TTL_MS) return
		val owner = deviceOwner?.trim()?.takeIf { it.isNotBlank() } ?: return
		lastAssignmentRefreshAtMs = now
		runCatching { repo.getTabletAssignment(tenant, owner) }
			.onSuccess { device ->
				if (device == null) return@onSuccess
				device.defaultTableNo
					?.trim()
					?.takeIf { it.isNotBlank() }
					?.let { runtimeTable = it }
				device.assignedWaiterName
					?.trim()
					?.takeIf { it.isNotBlank() }
					?.let { runtimeWaiter = it }
				android.util.Log.i(
					"SURVEY",
					"Asignacion tablet aplicada | owner=$owner | label=${device.label} | mesa=${device.defaultTableNo} | mesero=${device.assignedWaiterName} | active=${device.active}"
				)
			}
			.onFailure { e ->
				android.util.Log.w("SURVEY", "No se pudo cargar asignacion remota de tablet: ${e.message}")
			}
	}

	private companion object {
		const val ASSIGNMENT_REFRESH_TTL_MS = 2 * 60 * 1000L
	}

	private fun setLoading() {
		_state.update { it.copy(loading = true, error = null, canUseOffline = false, submitOutcome = null) }
	}

	private fun isDeletedQuestionAnswerError(message: String): Boolean {
		val lower = message.lowercase()
		return "pregunta" in lower && "no existe" in lower
	}

	private fun recoverFromStaleQuestion(message: String) {
		draftAnswers.clear()
		_state.update {
			it.copy(
				loading = false,
				error = null,
				submitError = "La encuesta se actualizó. Reinicia para continuar.",
				instanceId = null,
				stepIndex = -1
			)
		}
		viewModelScope.launch {
			runCatching {
				val refreshed = repo.refreshCurrentTemplate(tenant, runtimeLocationId)
				withContext(Dispatchers.IO) { offlineStore.saveCachedTemplate(refreshed) }
				refreshed
			}.onSuccess { refreshed ->
				_state.update {
					it.copy(
						template = refreshed,
						error = null,
						submitError = "La encuesta se actualizó. Reinicia para continuar.",
						instanceId = null,
						stepIndex = -1,
						offlineMode = false,
						canUseOffline = false
					)
				}
			}.onFailure {
				android.util.Log.w("SURVEY", "No se pudo refrescar template tras respuesta inválida: ${it.message}")
				_state.update { st -> st.copy(submitError = message) }
			}
		}
	}

	private fun SurveyTemplateDto.questionFingerprint(): String {
		return questions
			.orEmpty()
			.sortedBy { it.qorder ?: Int.MAX_VALUE }
			.joinToString("|") { q ->
				val options = q.options.orEmpty()
					.joinToString(",") { "${it.value}:${it.label}" }
				"${q.id}:${q.qorder}:${q.type}:${q.required}:${q.text}:${options}"
			}
	}

	private fun setError(msg: String?) {
		_state.update { it.copy(loading = false, error = msg ?: "Error") }
	}

	private suspend fun finishSurveyCycle(offlineMode: Boolean, outcome: SubmitOutcome) {
		draftAnswers.clear()
		val pending = pendingTemplateFromSync
		if (pending != null) {
			withContext(Dispatchers.IO) { offlineStore.saveCachedTemplate(pending) }
		}
		pendingTemplateFromSync = null
		val pendingCount = withContext(Dispatchers.IO) { offlineStore.pendingCount() }
		val tpl = pending ?: _state.value.template ?: run {
			setError("Template no cargado")
			return
		}

		_state.update {
			it.copy(
				instanceId = null,
				template = tpl,
				stepIndex = -1,
				error = null,
				submitError = null,
				celebrating = false,
				submitOutcome = outcome,
				offlineMode = offlineMode,
				canUseOffline = false,
				pendingSyncCount = pendingCount
			)
		}
	}

	private suspend fun syncPendingBestEffort() {
		val result = withContext(Dispatchers.IO) { offlineStore.syncPending(repo) }
		if (result.synced > 0) {
			android.util.Log.i("SURVEY", "Sincronizadas encuestas pendientes: ${result.synced}")
		}
		val pendingCount = withContext(Dispatchers.IO) { offlineStore.pendingCount() }
		_state.update { it.copy(pendingSyncCount = pendingCount) }
	}

	private fun adoptPendingTemplateIfAny() {
		val pending = pendingTemplateFromSync ?: return
		pendingTemplateFromSync = null
		_state.update { st -> st.copy(template = pending, error = null, submitError = null) }
	}

	private fun toJsonElement(value: Any): JsonElement = when (value) {
		is Int -> JsonPrimitive(value)
		is Long -> JsonPrimitive(value)
		is Double -> JsonPrimitive(value)
		is Float -> JsonPrimitive(value)
		is Boolean -> JsonPrimitive(value)
		is String -> JsonPrimitive(value)
		is List<*> -> JsonArray(value.filterNotNull().map { JsonPrimitive(it.toString()) })
		else -> JsonPrimitive(value.toString())
	}

	override fun onCleared() {
		syncListener?.stop()
		syncListener = null
		super.onCleared()
	}
}
