package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateQuestionRequest(
	val order: Int? = null,
	val type: String? = null,
	val text: String? = null,
	val required: Boolean? = null,
)
