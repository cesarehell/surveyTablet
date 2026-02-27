package com.mr.restaurant.survey.admin.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.AddQuestionRequest
import com.mr.restaurant.survey.core.template.dto.CreateOptionReq
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import com.mr.restaurant.survey.core.template.dto.UpdateOptionReq
import com.mr.restaurant.survey.core.template.dto.UpdateQuestionRequest
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

	private suspend fun reloadOrEmitError(templateId: String): Boolean {
		return when (val loadRes = safeCall { api.getFull(templateId) }) {
			is ApiResult.Ok -> {
				_state.update { it.copy(loading = false, error = null, full = loadRes.value) }
				true
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(loadRes.message))
				false
			}
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
					if (!reloadOrEmitError(templateId)) return@launch
					_events.tryEmit(AdminUiEvent.ShowSuccess("Pregunta agregada"))
				}

				is ApiResult.Err -> {
					_state.update { it.copy(loading = false) }
					_events.tryEmit(AdminUiEvent.ShowError(addRes.message))
				}
			}
		}

	fun updateQuestion(
		templateId: String,
		questionId: String,
		order: Int,
		type: String,
		text: String,
		required: Boolean
	) = viewModelScope.launch {
		if (text.isBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("La pregunta no puede ir vacía"))
			return@launch
		}
		_state.update { it.copy(loading = true) }
		when (val res = safeCall {
			api.updateQuestion(
				questionId = questionId,
				req = UpdateQuestionRequest(
					order = order,
					type = type,
					text = text.trim(),
					required = required
				)
			)
		}) {
			is ApiResult.Ok -> {
				if (!reloadOrEmitError(templateId)) return@launch
				_events.tryEmit(AdminUiEvent.ShowSuccess("Pregunta actualizada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun deleteQuestion(templateId: String, questionId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true) }
		when (val res = safeCall { api.deleteQuestion(questionId) }) {
			is ApiResult.Ok -> {
				if (!reloadOrEmitError(templateId)) return@launch
				_events.tryEmit(AdminUiEvent.ShowSuccess("Pregunta eliminada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun moveQuestion(templateId: String, questionId: String, direction: Int) = viewModelScope.launch {
		val full = _state.value.full ?: return@launch
		if (direction == 0) return@launch

		val sorted = full.questions.sortedBy { it.order }
		val fromIndex = sorted.indexOfFirst { it.id == questionId }
		if (fromIndex == -1) return@launch
		val toIndex = fromIndex + direction
		if (toIndex !in sorted.indices) return@launch

		val current = sorted[fromIndex]
		val target = sorted[toIndex]

		_state.update { it.copy(loading = true) }

		val first = safeCall {
			api.updateQuestion(
				questionId = current.id,
				req = UpdateQuestionRequest(
					order = target.order,
					type = current.type,
					text = current.text,
					required = current.required
				)
			)
		}
		if (first is ApiResult.Err) {
			_state.update { it.copy(loading = false) }
			_events.tryEmit(AdminUiEvent.ShowError(first.message))
			return@launch
		}

		val second = safeCall {
			api.updateQuestion(
				questionId = target.id,
				req = UpdateQuestionRequest(
					order = current.order,
					type = target.type,
					text = target.text,
					required = target.required
				)
			)
		}
		if (second is ApiResult.Err) {
			_state.update { it.copy(loading = false) }
			_events.tryEmit(AdminUiEvent.ShowError(second.message))
			return@launch
		}

		if (!reloadOrEmitError(templateId)) return@launch
		_events.tryEmit(AdminUiEvent.ShowSuccess("Orden de preguntas actualizado"))
	}

	fun addOption(templateId: String, questionId: String, label: String, value: String) = viewModelScope.launch {
		val trimmedLabel = label.trim()
		val trimmedValue = value.trim()
		if (trimmedLabel.isBlank() || trimmedValue.isBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("Label y value son requeridos"))
			return@launch
		}
		_state.update { it.copy(loading = true) }
		when (val res = safeCall {
			api.addOptions(
				questionId = questionId,
				opts = listOf(CreateOptionReq(label = trimmedLabel, value = trimmedValue))
			)
		}) {
			is ApiResult.Ok -> {
				if (!reloadOrEmitError(templateId)) return@launch
				_events.tryEmit(AdminUiEvent.ShowSuccess("Opción agregada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun updateOption(
		templateId: String,
		questionId: String,
		optionId: String,
		label: String,
		value: String,
		order: Int
	) = viewModelScope.launch {
		val trimmedLabel = label.trim()
		val trimmedValue = value.trim()
		if (trimmedLabel.isBlank() || trimmedValue.isBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("Label y value son requeridos"))
			return@launch
		}
		_state.update { it.copy(loading = true) }
		when (val res = safeCall {
			api.updateOption(
				questionId = questionId,
				optionId = optionId,
				req = UpdateOptionReq(label = trimmedLabel, value = trimmedValue, oOrder = order)
			)
		}) {
			is ApiResult.Ok -> {
				if (!reloadOrEmitError(templateId)) return@launch
				_events.tryEmit(AdminUiEvent.ShowSuccess("Opción actualizada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun deleteOption(templateId: String, questionId: String, optionId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true) }
		when (val res = safeCall { api.deleteOption(questionId = questionId, optionId = optionId) }) {
			is ApiResult.Ok -> {
				if (!reloadOrEmitError(templateId)) return@launch
				_events.tryEmit(AdminUiEvent.ShowSuccess("Opción eliminada"))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}
}
