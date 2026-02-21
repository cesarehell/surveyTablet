package com.mr.restaurant.survey.admin.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.AddQuestionRequest
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

data class TemplateDetailState(
	val loading: Boolean = false,
	val full: TemplateFullDto? = null,
	val error: String? = null
)

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
	private val api: TemplateApi
) : ViewModel() {
	private val _state = MutableStateFlow(TemplateDetailState())
	val state = _state.asStateFlow()
	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(templateId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		when (val res = safeCall { api.getFull(templateId) }) {
			is ApiResult.Ok -> _state.update { it.copy(loading = false, full = res.value) }
			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	fun addQuestion(templateId: String, order: Int, type: String, text: String, required: Boolean) =
		viewModelScope.launch {
			if (text.isBlank()) {
				_events.tryEmit(AdminUiEvent.ShowError("La pregunta no puede ir vacía"))
				return@launch
			}

			_state.update { it.copy(loading = true) }

			val req = AddQuestionRequest(
				order = order,
				type = type,
				text = text.trim(),
				required = required
			)

			when (val addRes = safeCall { api.addQuestion(templateId, req) }) {
				is ApiResult.Ok -> {
					when (val loadRes = safeCall { api.getFull(templateId) }) {
						is ApiResult.Ok -> _state.update {
							it.copy(
								loading = false,
								error = null,
								full = loadRes.value
							)
						}
						is ApiResult.Err -> {
							_state.update { it.copy(loading = false) }
							_events.tryEmit(AdminUiEvent.ShowError(loadRes.message))
							return@launch
						}
					}
					_events.tryEmit(AdminUiEvent.ShowSuccess("Pregunta agregada"))
				}

				is ApiResult.Err -> {
					_state.update { it.copy(loading = false) }
					_events.tryEmit(AdminUiEvent.ShowError(addRes.message))
				}
			}
		}
}
