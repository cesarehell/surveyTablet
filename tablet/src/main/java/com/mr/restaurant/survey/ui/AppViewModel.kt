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
	private val appContext: Context, // debe ser Application context
	private val table: String = "A7",
	private val waiter: String = "ERIKA"
) : ViewModel() {

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state

	private var syncListener: RtdbSyncListener? = null

	init {
		bootstrap()
		startRealtimeSync()
	}

	private fun bootstrap() = viewModelScope.launch {
		setLoading()

		runCatching {
			val template = loadInitialTemplate()
			val instanceId = startNewSurveyInstance(template.id)

			_state.value = UiState(
				loading = false,
				instanceId = instanceId,
				template = template
			)
		}.onFailure { e ->
			setError(e.message ?: "Error de red")
		}
	}

	private suspend fun loadInitialTemplate(): SurveyTemplateDto =
		if (templateId != null) repo.loadTemplateWithOptions(templateId)
		else repo.pickCurrentTemplate(tenant)

	private suspend fun startNewSurveyInstance(templateId: String): String {
		android.util.Log.i(
			"SURVEY",
			"Iniciando encuesta (app) | tenant=$tenant | templateId=$templateId | locationId=$locationId | table=$table | waiter=$waiter"
		)

		return repo.startSurvey(
			tenant = tenant,
			templateId = templateId,
			locationId = locationId,
			table = table,
			waiter = waiter
		).instanceId
	}

	fun next() {
		_state.update { it.copy(stepIndex = it.stepIndex + 1) }
	}

	fun postAnswer(qid: String, value: Any) = viewModelScope.launch {
		val inst = _state.value.instanceId ?: return@launch
		runCatching { repo.postAnswer(inst, qid, value) }
			.onFailure { e -> setError(e.message) }
	}

	fun submitAndRestart() = viewModelScope.launch {
		val inst = _state.value.instanceId ?: return@launch

		runCatching { repo.submit(inst) }
			.onSuccess {
				_state.update { it.copy(celebrating = true, error = null) }
				delay(1200)

				val tplId = _state.value.template?.id ?: run {
					setError("Template no cargado")
					_state.update { it.copy(celebrating = false) }
					return@launch
				}

				val newInstanceId = startNewSurveyInstance(tplId)
				_state.update {
					it.copy(
						instanceId = newInstanceId,
						stepIndex = -1,
						error = null,
						celebrating = false
					)
				}
			}
			.onFailure { e ->
				setError(e.message ?: "Error al enviar")
			}
	}

	private fun startRealtimeSync() {
		val prefs = RtdbSyncPrefs(appContext)

		syncListener = RtdbSyncListener(
			tenantId = tenant,
			prefs = prefs,
			scope = viewModelScope,
			onTrigger = {
				val newTpl = repo.refreshCurrentTemplate(tenant)
				_state.value = _state.value.copy(template = newTpl, error = null, stepIndex = -1)
			}
		).also { it.start() }
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
