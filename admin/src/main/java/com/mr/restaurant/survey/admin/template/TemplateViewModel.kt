package com.mr.restaurant.survey.admin.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.core.net.ApiFactory
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.CreateTemplateRequest
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateScope
import com.mr.restaurant.survey.core.template.dto.TemplateStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TemplateViewModel : ViewModel() {

	private val api = ApiFactory
		.retrofit()
		.create(TemplateApi::class.java)

	private val _state = MutableStateFlow(TemplatesUiState())
	val state: StateFlow<TemplatesUiState> = _state.asStateFlow()
	private val _effects = MutableSharedFlow<TemplateEffect>()
	val effects: SharedFlow<TemplateEffect> = _effects.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		val statusParam = _state.value.statusFilter?.name

		when (val res = safeCall { api.listPaged(tenantId = tenantId, status = statusParam) }) {
			is ApiResult.Ok -> _state.update { it.copy(loading = false, templates = res.value.content) }
			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}

	}

	fun setFilter(status: TemplateStatus?, tenantId: String) {
		_state.update { it.copy(statusFilter = status) }
		load(tenantId)
	}

	fun togglePublish(tenantId: String, t: SurveyTemplateDto) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }

		val res = when (t.status) {
			TemplateStatus.PUBLISHED ->
				safeCall { api.setStatus(t.id, TemplateStatus.DRAFT) }

			TemplateStatus.DRAFT ->
				safeCall { api.publish(t.id) }

			TemplateStatus.ARCHIVED ->
				ApiResult.Err("No se puede publicar un template archivado")
		}
		when (res) {
			is ApiResult.Ok -> {
				load(tenantId)
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false, error = res.message) }
			}
		}

	}

	fun create(
		tenantId: String,
		name: String,
		scope: TemplateScope,
		locationId: String?,
		npsEnabled: Boolean
	) = viewModelScope.launch {

		if (name.isBlank()) {
			_state.update { it.copy(error = "El nombre es obligatorio") }
			return@launch
		}
		if (scope == TemplateScope.LOCATION && locationId == null) {
			_state.update { it.copy(error = "Debes seleccionar una sucursal") }
			return@launch
		}

		_state.update { it.copy(loading = true, error = null) }

		val req = CreateTemplateRequest(
			tenantId = tenantId,
			name = name.trim(),
			scope = scope,
			locationId = if (scope == TemplateScope.LOCATION) locationId else null,
			npsEnabled = npsEnabled
		)

		when (val res = safeCall { api.create(req) }) {
			is ApiResult.Ok -> {
				load(tenantId)
				_state.update { it.copy(loading = false) }
				_effects.emit(TemplateEffect.Created(res.value.id))
			}

			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	sealed class TemplateEffect {
		data class Created(val templateId: String) : TemplateEffect()
	}

	data class TemplatesUiState(
		val loading: Boolean = false,
		val templates: List<SurveyTemplateDto> = emptyList(),
		val statusFilter: TemplateStatus? = null, // null | DRAFT | PUBLISHED | ARCHIVED
		val error: String? = null
	)
}