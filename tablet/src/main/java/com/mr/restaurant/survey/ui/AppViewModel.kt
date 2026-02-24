package com.mr.restaurant.survey.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.data.sync.RtdbSyncListener
import com.mr.restaurant.survey.data.sync.RtdbSyncPrefs
import com.mr.restaurant.survey.net.dto.SurveyTemplateDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
	val loading: Boolean = true,
	val error: String? = null,
	val submitError: String? = null,
	val instanceId: String? = null,
	val template: SurveyTemplateDto? = null,
	val stepIndex: Int = -1,
	val celebrating: Boolean = false
)

class AppViewModel(
	private val repo: SurveyRepository,
	private val tenant: String,
	private val templateId: String?,
	private val locationId: String? = null,
	private val deviceOwner: String? = null,
	private val appContext: Context, // debe ser Application context
	private val table: String = "A7",
	private val waiter: String = "ERIKA",
	private val knownWaiters: List<String> = emptyList()
) : ViewModel() {

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state

	private var syncListener: RtdbSyncListener? = null
	private var runtimeLocationId: String? = locationId?.takeIf { it.isNotBlank() }
	private var runtimeTable: String = table
	private var runtimeWaiter: String = waiter

	init {
		bootstrap()
		startRealtimeSync()
	}

	private fun bootstrap() = viewModelScope.launch {
		setLoading()

		runCatching {
			refreshRemoteAssignmentBestEffort()
			loadInitialTemplate()
		}.onSuccess { template ->
			_state.value = UiState(
				loading = false,
				instanceId = null,
				template = template,
				stepIndex = -1
			)
		}.onFailure { e ->
			setError(e.message ?: "Error de red")
		}
	}

	fun retryBootstrap() {
		bootstrap()
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
			table = runtimeTable,
			waiter = runtimeWaiter
		).instanceId
	}

	fun defaultTable(): String = runtimeTable
	fun defaultWaiter(): String = runtimeWaiter
	fun waiterOptions(): List<String> = knownWaiters

	fun beginSurvey(tableNo: String?, waiterName: String?) = viewModelScope.launch {
		val current = _state.value
		val template = current.template ?: run {
			setError("Template no cargado")
			return@launch
		}
		refreshRemoteAssignmentBestEffort()
		runtimeTable = tableNo?.trim()?.takeIf { it.isNotBlank() } ?: runtimeTable.ifBlank { table }
		runtimeWaiter = waiterName?.trim()?.takeIf { it.isNotBlank() } ?: runtimeWaiter.ifBlank { waiter }

		_state.update { it.copy(loading = true, error = null, submitError = null) }
		runCatching { startNewSurveyInstance(template) }
			.onSuccess { instanceId ->
				_state.update {
					it.copy(
						loading = false,
						error = null,
						submitError = null,
						instanceId = instanceId,
						stepIndex = 0
					)
				}
			}
			.onFailure { e ->
				setError(e.message ?: "No se pudo iniciar la encuesta")
			}
	}

	fun next() {
		_state.update { it.copy(stepIndex = it.stepIndex + 1) }
	}

	fun postAnswer(qid: String, value: Any) = viewModelScope.launch {
		val inst = _state.value.instanceId ?: return@launch
		runCatching { repo.postAnswer(inst, qid, value) }
			.onFailure { e -> setError(e.message) }
	}

	fun submitAndRestart(email: String?, marketingOptIn: Boolean) = viewModelScope.launch {
		val inst = _state.value.instanceId ?: return@launch
		val normalizedEmail = email?.trim()?.takeIf { it.isNotBlank() }
		_state.update { it.copy(submitError = null) }

		runCatching { repo.submit(inst, email = normalizedEmail, marketingOptIn = marketingOptIn) }
			.onSuccess {
				_state.update { it.copy(celebrating = true, error = null, submitError = null) }
				delay(1200)

				val tpl = _state.value.template ?: run {
					setError("Template no cargado")
					_state.update { it.copy(celebrating = false) }
					return@launch
				}

				_state.update {
					it.copy(
						instanceId = null,
						template = tpl,
						stepIndex = -1,
						error = null,
						submitError = null,
						celebrating = false
					)
				}
			}
			.onFailure { e ->
				_state.update {
					it.copy(
						loading = false,
						submitError = e.message ?: "Error al enviar"
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
				runCatching {
					val current = _state.value
					val currentTemplateId = current.template?.id
					val newTpl = repo.refreshCurrentTemplate(tenant, runtimeLocationId)
					val shouldResetInstance = currentTemplateId != null && currentTemplateId != newTpl.id
					val newInstanceId = if (shouldResetInstance) null else current.instanceId
					val newStepIndex = if (shouldResetInstance) -1 else current.stepIndex

					_state.value = current.copy(
						template = newTpl,
						instanceId = newInstanceId,
						error = null,
						submitError = null,
						stepIndex = newStepIndex
					)
				}.onFailure { e ->
					setError(e.message ?: "Error refrescando encuesta")
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

	private suspend fun refreshRemoteAssignmentBestEffort() {
		val owner = deviceOwner?.trim()?.takeIf { it.isNotBlank() } ?: return
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

	private fun setLoading() {
		_state.update { it.copy(loading = true, error = null) }
	}

	private fun setError(msg: String?) {
		_state.update { it.copy(loading = false, error = msg ?: "Error") }
	}

	override fun onCleared() {
		syncListener?.stop()
		syncListener = null
		super.onCleared()
	}
}
