package com.mr.restaurant.survey.admin.threshold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import com.mr.restaurant.survey.core.threshold.api.ThresholdApi
import com.mr.restaurant.survey.core.threshold.dto.CreateThresholdRequest
import com.mr.restaurant.survey.core.threshold.dto.ThresholdRuleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThresholdsUiState(
	val loading: Boolean = false,
	val contextLoading: Boolean = false,
	val creating: Boolean = false,
	val templates: List<SurveyTemplateDto> = emptyList(),
	val selectedTemplateId: String? = null,
	val templateFull: TemplateFullDto? = null,
	val rules: List<ThresholdRuleDto> = emptyList(),
	val error: String? = null,
	val contextError: String? = null,
	val busyRuleIds: Set<String> = emptySet()
)

@HiltViewModel
class ThresholdViewModel @Inject constructor(
	private val templateApi: TemplateApi,
	private val thresholdApi: ThresholdApi
) : ViewModel() {
	private val _state = MutableStateFlow(ThresholdsUiState())
	val state = _state.asStateFlow()

	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update {
			it.copy(
				loading = true,
				error = null,
				contextError = null
			)
		}

		when (val res = safeCall { templateApi.listPaged(tenantId = tenantId, status = null, page = 0, size = 200) }) {
			is ApiResult.Ok -> {
				val templates = res.value.content
				val selected = templates.firstOrNull()?.id
				_state.update {
					it.copy(
						loading = false,
						templates = templates,
						selectedTemplateId = selected,
						error = null,
						templateFull = null,
						rules = emptyList()
					)
				}
				if (selected != null) {
					loadTemplateContext(selected)
				}
			}

			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	fun selectTemplate(templateId: String) {
		if (_state.value.selectedTemplateId == templateId && _state.value.templateFull != null) return
		_state.update { it.copy(selectedTemplateId = templateId, contextError = null) }
		loadTemplateContext(templateId)
	}

	fun retrySelected() {
		val selected = _state.value.selectedTemplateId ?: return
		loadTemplateContext(selected)
	}

	fun createRule(
		tenantId: String,
		type: String,
		valueInput: String,
		cooldownInput: String,
		questionId: String?
	) = viewModelScope.launch {
		if (_state.value.creating) return@launch

		val templateId = _state.value.selectedTemplateId
		if (templateId.isNullOrBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("Selecciona un template"))
			return@launch
		}

		val value = valueInput.trim().toDoubleOrNull()
		if (value == null) {
			_events.tryEmit(AdminUiEvent.ShowError("Valor inválido"))
			return@launch
		}

		val cooldown = cooldownInput.trim().toIntOrNull()
		if (cooldown == null || cooldown < 0) {
			_events.tryEmit(AdminUiEvent.ShowError("Cooldown inválido"))
			return@launch
		}

		_state.update { it.copy(creating = true) }
		val req = CreateThresholdRequest(
			tenantId = tenantId,
			templateId = templateId,
			questionId = questionId,
			type = type,
			value = value,
			cooldownMin = cooldown
		)

		when (val res = safeCall { thresholdApi.create(req) }) {
			is ApiResult.Ok -> {
				_state.update { it.copy(creating = false) }
				reloadRulesOnly(templateId)
				_events.tryEmit(AdminUiEvent.CloseDialog)
				_events.tryEmit(AdminUiEvent.ShowSuccess("Regla creada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(creating = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun toggleRule(rule: ThresholdRuleDto) = viewModelScope.launch {
		setRuleBusy(rule.id, true)
		when (val res = safeCall { thresholdApi.setActive(rule.id, !rule.active) }) {
			is ApiResult.Ok -> {
				_state.update { st ->
					st.copy(
						rules = st.rules.map {
							if (it.id == rule.id) it.copy(active = res.value.active) else it
						}
					)
				}
			}

			is ApiResult.Err -> _events.tryEmit(AdminUiEvent.ShowError(res.message))
		}
		setRuleBusy(rule.id, false)
	}

	fun deleteRule(ruleId: String) = viewModelScope.launch {
		setRuleBusy(ruleId, true)
		when (val res = safeCall { thresholdApi.delete(ruleId) }) {
			is ApiResult.Ok -> {
				_state.update { st -> st.copy(rules = st.rules.filterNot { it.id == ruleId }) }
				_events.tryEmit(AdminUiEvent.ShowSuccess("Regla eliminada"))
			}

			is ApiResult.Err -> _events.tryEmit(AdminUiEvent.ShowError(res.message))
		}
		setRuleBusy(ruleId, false)
	}

	private fun loadTemplateContext(templateId: String) = viewModelScope.launch {
		_state.update { it.copy(contextLoading = true, contextError = null) }

		val fullRes = safeCall { templateApi.getFull(templateId) }
		val rulesRes = safeCall { thresholdApi.list(templateId) }

		val contextError = listOf(fullRes, rulesRes)
			.filterIsInstance<ApiResult.Err>()
			.firstOrNull()
			?.message

		_state.update { st ->
			st.copy(
				contextLoading = false,
				templateFull = (fullRes as? ApiResult.Ok)?.value ?: st.templateFull,
				rules = (rulesRes as? ApiResult.Ok)?.value ?: st.rules,
				contextError = contextError
			)
		}
	}

	private suspend fun reloadRulesOnly(templateId: String) {
		_state.update { it.copy(contextLoading = true, contextError = null) }
		when (val res = safeCall { thresholdApi.list(templateId) }) {
			is ApiResult.Ok -> _state.update {
				it.copy(
					contextLoading = false,
					rules = res.value,
					contextError = null
				)
			}

			is ApiResult.Err -> {
				_state.update { it.copy(contextLoading = false, contextError = res.message) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	private fun setRuleBusy(ruleId: String, busy: Boolean) {
		_state.update { st ->
			val next = st.busyRuleIds.toMutableSet()
			if (busy) next += ruleId else next -= ruleId
			st.copy(busyRuleIds = next)
		}
	}
}
