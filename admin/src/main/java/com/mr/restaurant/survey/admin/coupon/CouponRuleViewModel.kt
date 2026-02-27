package com.mr.restaurant.survey.admin.coupon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.coupon.api.CouponRuleApi
import com.mr.restaurant.survey.core.coupon.dto.CouponRuleDto
import com.mr.restaurant.survey.core.coupon.dto.CreateCouponRuleRequest
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CouponRulesUiState(
	val loading: Boolean = false,
	val contextLoading: Boolean = false,
	val creating: Boolean = false,
	val templates: List<SurveyTemplateDto> = emptyList(),
	val selectedTemplateId: String? = null,
	val templateFull: TemplateFullDto? = null,
	val rules: List<CouponRuleDto> = emptyList(),
	val error: String? = null,
	val contextError: String? = null,
	val busyRuleIds: Set<String> = emptySet()
)

@HiltViewModel
class CouponRuleViewModel @Inject constructor(
	private val templateApi: TemplateApi,
	private val couponRuleApi: CouponRuleApi
) : ViewModel() {
	private val _state = MutableStateFlow(CouponRulesUiState())
	val state = _state.asStateFlow()

	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null, contextError = null) }
		when (val res = safeCall { templateApi.listPaged(tenantId = tenantId, status = null, page = 0, size = 200) }) {
			is ApiResult.Ok -> {
				val templates = res.value.content
				val selected = templates.firstOrNull()?.id
				_state.update {
					it.copy(
						loading = false,
						templates = templates,
						selectedTemplateId = selected,
						templateFull = null,
						rules = emptyList(),
						error = null
					)
				}
				if (selected != null) loadTemplateContext(selected)
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
		val id = _state.value.selectedTemplateId ?: return
		loadTemplateContext(id)
	}

	fun createRule(
		tenantId: String,
		type: String,
		questionId: String?,
		minVisitsInput: String,
		offerType: String,
		percentOffInput: String,
		amountOffInput: String,
		productInput: String,
		termsInput: String
	) = viewModelScope.launch {
		if (_state.value.creating) return@launch

		val templateId = _state.value.selectedTemplateId
		if (templateId.isNullOrBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("Selecciona un template"))
			return@launch
		}

		val normalizedType = type.uppercase()
		val minVisits = minVisitsInput.trim().takeIf { it.isNotBlank() }?.toIntOrNull()
		if (normalizedType == "VISITS_GE" && (minVisits == null || minVisits < 1)) {
			_events.tryEmit(AdminUiEvent.ShowError("Visitas mínimas inválidas"))
			return@launch
		}

		val normalizedOfferType = offerType.uppercase()
		val percentOff = percentOffInput.trim().takeIf { it.isNotBlank() }?.toIntOrNull()
		val amountOff = amountOffInput.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
		val product = productInput.trim().ifBlank { null }
		val terms = termsInput.trim().ifBlank { null }

		when (normalizedOfferType) {
			"PERCENT" -> if (percentOff == null || percentOff !in 1..100) {
				_events.tryEmit(AdminUiEvent.ShowError("Descuento % debe ser 1..100"))
				return@launch
			}

			"AMOUNT" -> if (amountOff == null || amountOff <= 0.0) {
				_events.tryEmit(AdminUiEvent.ShowError("Monto debe ser > 0"))
				return@launch
			}

			"PRODUCT" -> if (product.isNullOrBlank()) {
				_events.tryEmit(AdminUiEvent.ShowError("Producto requerido"))
				return@launch
			}
		}

		val request = CreateCouponRuleRequest(
			tenantId = tenantId,
			templateId = templateId,
			questionId = questionId?.takeIf { it.isNotBlank() },
			type = normalizedType,
			value = when (normalizedType) {
				"VISITS_GE" -> (minVisits ?: 1).toDouble()
				"NEGATIVE" -> 2.0
				else -> 0.0
			},
			minVisits = if (normalizedType == "VISITS_GE") minVisits else null,
			offerType = normalizedOfferType,
			percentOff = if (normalizedOfferType == "PERCENT") percentOff else null,
			amountOff = if (normalizedOfferType == "AMOUNT") amountOff else null,
			product = if (normalizedOfferType == "PRODUCT") product else null,
			terms = terms
		)

		_state.update { it.copy(creating = true) }
		when (val res = safeCall { couponRuleApi.create(request) }) {
			is ApiResult.Ok -> {
				_state.update { it.copy(creating = false) }
				reloadRulesOnly(templateId)
				_events.tryEmit(AdminUiEvent.CloseDialog)
				_events.tryEmit(AdminUiEvent.ShowSuccess("Regla de cupón creada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(creating = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun toggleRule(rule: CouponRuleDto) = viewModelScope.launch {
		setRuleBusy(rule.id, true)
		when (val res = safeCall { couponRuleApi.setActive(rule.id, !rule.active) }) {
			is ApiResult.Ok -> _state.update { st ->
				st.copy(rules = st.rules.map { if (it.id == rule.id) it.copy(active = res.value.active) else it })
			}

			is ApiResult.Err -> _events.tryEmit(AdminUiEvent.ShowError(res.message))
		}
		setRuleBusy(rule.id, false)
	}

	fun deleteRule(ruleId: String) = viewModelScope.launch {
		setRuleBusy(ruleId, true)
		when (val res = safeCall { couponRuleApi.delete(ruleId) }) {
			is ApiResult.Ok -> {
				_state.update { it.copy(rules = it.rules.filterNot { r -> r.id == ruleId }) }
				_events.tryEmit(AdminUiEvent.ShowSuccess("Regla eliminada"))
			}

			is ApiResult.Err -> _events.tryEmit(AdminUiEvent.ShowError(res.message))
		}
		setRuleBusy(ruleId, false)
	}

	private fun loadTemplateContext(templateId: String) = viewModelScope.launch {
		_state.update { it.copy(contextLoading = true, contextError = null) }
		val fullRes = safeCall { templateApi.getFull(templateId) }
		val rulesRes = safeCall { couponRuleApi.list(templateId) }
		val contextError = listOf(fullRes, rulesRes).filterIsInstance<ApiResult.Err>().firstOrNull()?.message
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
		when (val res = safeCall { couponRuleApi.list(templateId) }) {
			is ApiResult.Ok -> _state.update { it.copy(rules = res.value, contextError = null) }
			is ApiResult.Err -> _events.tryEmit(AdminUiEvent.ShowError(res.message))
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
