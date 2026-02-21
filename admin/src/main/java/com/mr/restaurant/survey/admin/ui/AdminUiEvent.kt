package com.mr.restaurant.survey.admin.ui

sealed interface AdminUiEvent {
	data class ShowError(val message: String) : AdminUiEvent
	data class ShowSuccess(val message: String) : AdminUiEvent
	data object CloseDialog : AdminUiEvent
	data class NavigateToTemplateDetail(val templateId: String) : AdminUiEvent
}
