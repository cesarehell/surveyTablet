package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class TemplateFullDto(
	val id: String,
	val tenantId: String,
	val name: String,
	val status: String,
	val npsEnabled: Boolean = true,
	val scope: String? = null,
	val locationId: String? = null,
	val groupId: String? = null,
	val questions: List<QuestionDto> = emptyList()
) {
	@Serializable
	data class QuestionDto(
		val id: String,
		val order: Int,
		val text: String,
		val type: String,
		val required: Boolean = true,
		val options: List<OptionDto> = emptyList()
	)

	@Serializable
	data class OptionDto(
		val id: String,
		val label: String,
		val value: String,
		val oOrder: Int = 0
	)
}
