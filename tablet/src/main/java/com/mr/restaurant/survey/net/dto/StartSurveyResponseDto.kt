package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class StartSurveyResponseDto(
	val instanceId: String,
	val templateId: String? = null
)
