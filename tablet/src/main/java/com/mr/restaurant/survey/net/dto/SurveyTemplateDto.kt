package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SurveyTemplateDto(
	val id: String,
	val tenantId: String,
	val name: String,
	val scope: String? = null,
	val location: LocationDto? = null,
	val status: String? = null,
	val npsEnabled: Boolean = true,
	val questions: List<QuestionDto>? = null
)
