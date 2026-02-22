package com.mr.restaurant.survey.admin.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.CreateTemplateRequest
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateScope
import com.mr.restaurant.survey.core.template.dto.TemplateStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(
	private val api: TemplateApi
) : ViewModel() {
	private val _state = MutableStateFlow(TemplatesUiState())
	val state = _state.asStateFlow()

	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		applyTemplateLoadResult(tenantId)
	}

	fun setFilter(status: TemplateStatus?, tenantId: String) {
		_state.update { it.copy(statusFilter = status) }
		load(tenantId)
	}

	fun togglePublish(tenantId: String, t: SurveyTemplateDto) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }

		val res = when (t.status) {
			TemplateStatus.PUBLISHED -> safeCall { api.setStatus(t.id, TemplateStatus.DRAFT) }
			TemplateStatus.DRAFT -> safeCall { api.publish(t.id) }
			TemplateStatus.ARCHIVED -> ApiResult.Err("No se puede publicar un template archivado")
		}

		when (res) {
			is ApiResult.Ok -> {
				applyTemplateLoadResult(tenantId)
				val message = when (t.status) {
					TemplateStatus.PUBLISHED -> "Template despublicado"
					TemplateStatus.DRAFT -> "Template publicado"
					TemplateStatus.ARCHIVED -> "Template actualizado"
				}
				_events.tryEmit(AdminUiEvent.ShowSuccess(message))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
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
			_events.tryEmit(AdminUiEvent.ShowError("El nombre es obligatorio"))
			return@launch
		}
		if (scope == TemplateScope.LOCATION && locationId == null) {
			_events.tryEmit(AdminUiEvent.ShowError("Debes seleccionar una sucursal"))
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
				applyTemplateLoadResult(tenantId)
				_events.tryEmit(AdminUiEvent.ShowSuccess("Template creado"))
				_events.tryEmit(AdminUiEvent.NavigateToTemplateDetail(res.value.id))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	private suspend fun applyTemplateLoadResult(tenantId: String) {
		when (val res = fetchTemplates(tenantId)) {
			is ApiResult.Ok -> _state.update {
				it.copy(
					loading = false,
					error = null,
					templates = res.value
				)
			}

			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	private suspend fun fetchTemplates(tenantId: String): ApiResult<List<SurveyTemplateDto>> {
		val statusParam = _state.value.statusFilter?.name
		return safeCall {
			api.listPaged(tenantId = tenantId, status = statusParam).content
		}
	}

	data class TemplatesUiState(
		val loading: Boolean = false,
		val templates: List<SurveyTemplateDto> = emptyList(),
		val statusFilter: TemplateStatus? = null,
		val error: String? = null
	)
}
