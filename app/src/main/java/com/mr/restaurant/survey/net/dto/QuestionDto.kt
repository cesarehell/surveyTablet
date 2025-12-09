package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuestionDto(
	val id: String,
	val tenantId: String? = null,
	val type: QuestionType,
	val text: String,
	val required: Boolean = true,
	val qorder: Int? = null,
	val options: List<QOptionDTO>? = null
)
