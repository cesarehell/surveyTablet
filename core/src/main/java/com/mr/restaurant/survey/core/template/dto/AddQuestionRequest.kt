package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddQuestionRequest(
	val order: Int,
	val type: String,
	val text: String,
	val required: Boolean = true
)

