package com.mr.restaurant.survey.admin.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.core.net.ApiFactory
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.AddQuestionRequest
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateDetailState(
	val loading: Boolean = false,
	val full: TemplateFullDto? = null,
	val error: String? = null
)

class TemplateDetailViewModel : ViewModel() {

	private val api = ApiFactory.retrofit().create(TemplateApi::class.java)

	private val _state = MutableStateFlow(TemplateDetailState())
	val state = _state.asStateFlow()

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
				_state.update { it.copy(error = "La pregunta no puede ir vacía") }
				return@launch
			}

			_state.update { it.copy(loading = true, error = null) }

			val req = AddQuestionRequest(
				order = order,
				type = type,
				text = text.trim(),
				required = required
			)

			when (val res = safeCall { api.addQuestion(templateId, req) }) {
				is ApiResult.Ok -> load(templateId)
				is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
			}
		}
}
